// src/main/java/com/manil/manil/image/service/ImageStorageService.java
package com.manil.manil.image.service;

import com.manil.manil.image.config.ImageStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private final ImageStorageProperties props;

    private static final Set<String> DEFAULT_ALLOWED = Set.of("jpg","jpeg","png","webp","gif");

    private Set<String> allowed() {
        String conf = props.getAllowedExtensions();
        if (conf == null || conf.isBlank()) return DEFAULT_ALLOWED;
        Set<String> out = new HashSet<>();
        for (String s : conf.split(",")) {
            if (s != null && !s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /** 파일명/URL에서 확장자 추출 */
    private String extFromNameOrUrl(String nameOrUrl) {
        if (nameOrUrl == null) return "";
        String path = nameOrUrl;
        try {
            URI uri = URI.create(nameOrUrl);
            if (uri.getScheme() != null) path = uri.getPath();
        } catch (IllegalArgumentException ignore) { }
        String ext = StringUtils.getFilenameExtension(path);
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }

    private void ensureDir(Path p) throws IOException {
        if (!Files.exists(p)) Files.createDirectories(p);
    }

    private void checkSize(long size) {
        if (props.getMaxSizeBytes() > 0 && size > props.getMaxSizeBytes()) {
            throw new IllegalArgumentException("이미지 용량이 제한을 초과했습니다.");
        }
    }

    private String nextFileName(int index, String ext) {
        return String.format("%03d.%s", index, ext);
    }

    private String urlJoin(String base, String... parts) {
        String b = base.endsWith("/") ? base.substring(0, base.length()-1) : base;
        StringBuilder sb = new StringBuilder(b);
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            String seg = p.startsWith("/") ? p.substring(1) : p;
            sb.append('/').append(seg);
        }
        return sb.toString();
    }

    // =========================================
    // 1) 분석 단계: 첫 이미지(0번)만 캐시에 저장
    // =========================================
    public void saveAnalyzeMainFromMultipart(List<MultipartFile> images, String analyzeId) throws IOException {
        if (images == null || images.isEmpty()) return;
        MultipartFile main = images.get(0);

        String ext = extFromNameOrUrl(main.getOriginalFilename());
        if (!allowed().contains(ext)) throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다.");
        checkSize(main.getSize());

        Path dir = Path.of(props.getRootDir(), "cache", analyzeId);
        ensureDir(dir);

        Path path = dir.resolve("main." + ext);
        try (InputStream in = main.getInputStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // (선택) JSON 분석 플로우에서 외부 URL을 0번으로 저장할 때 사용
    public void saveAnalyzeMainFromUrl(List<String> urls, String analyzeId) throws IOException {
        if (urls == null || urls.isEmpty()) return;
        String first = urls.get(0);

        String ext = extFromNameOrUrl(first);
        if (!allowed().contains(ext)) throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다.");

        Path dir = Path.of(props.getRootDir(), "cache", analyzeId);
        ensureDir(dir);

        Path path = dir.resolve("main." + ext);
        download(first, path);
    }

    public List<String> finalizeProductImagesFromMultipart(
            Long productId,
            String analyzeId,
            List<MultipartFile> images,
            Integer mainIndex
    ) throws IOException {

        Path productDir = Path.of(props.getRootDir(), "products", String.valueOf(productId));
        ensureDir(productDir);

        // --- 캐시 main(있으면) 찾기 ---
        Path cacheMain = null;
        if (analyzeId != null && !analyzeId.isBlank()) {
            Path cacheDir = Path.of(props.getRootDir(), "cache", analyzeId);
            if (Files.exists(cacheDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheDir, "main.*")) {
                    for (Path p : ds) { cacheMain = p; break; } // 하나만
                }
            }
        }

        // --- 업로드 이미지 유효성 검사 ---
        List<MultipartFile> safeImages = new ArrayList<>();
        if (images != null) {
            for (MultipartFile mf : images) {
                if (mf == null || mf.isEmpty()) continue;
                String ext = extFromNameOrUrl(mf.getOriginalFilename());
                if (!allowed().contains(ext)) continue;
                checkSize(mf.getSize());
                safeImages.add(mf);
            }
        }

        // --- 메인 선정: images[mainIndex] 우선, 없으면 cacheMain ---
        List<Sink> order = new ArrayList<>();
        boolean mainPicked = false;

        if (!safeImages.isEmpty() && mainIndex != null
                && mainIndex >= 0 && mainIndex < safeImages.size()) {
            order.add(Sink.of(safeImages.get(mainIndex)));  // 메인
            mainPicked = true;
        } else if (cacheMain != null) {
            order.add(Sink.of(cacheMain));                  // 메인
            mainPicked = true;
        }

        // --- 나머지들 채우기 (중복 제외) ---
        if (!safeImages.isEmpty()) {
            for (int i = 0; i < safeImages.size(); i++) {
                if (mainPicked && mainIndex != null && i == mainIndex) continue; // 이미 0번으로 사용
                order.add(Sink.of(safeImages.get(i)));
            }
        }

        if (cacheMain != null) {
            // 람다 대신 for-loop로 중복 검사 (effectively final 이슈 회피)
            boolean exists = false;
            for (Sink s : order) {
                if (s.isCacheMain(cacheMain)) { exists = true; break; }
            }
            if (!exists) order.add(Sink.of(cacheMain));
        }

        // --- 디스크 기록 (000부터) ---
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            Sink s = order.get(i);
            String ext = s.ext();
            Path dst = productDir.resolve(nextFileName(i, ext));

            if (s.multipart != null) {
                try (InputStream in = s.multipart.getInputStream()) {
                    Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (s.cachePath != null) {
                Files.move(s.cachePath, dst, StandardCopyOption.REPLACE_EXISTING);
            }
            urls.add(urlJoin(props.getPublicBaseUrl(), "products", String.valueOf(productId), dst.getFileName().toString()));
        }

        // --- (선택) 캐시 디렉터리 정리 ---
        if (analyzeId != null && !analyzeId.isBlank()) {
            Path cacheDir = Path.of(props.getRootDir(), "cache", analyzeId);
            try {
                if (Files.exists(cacheDir)) {
                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheDir)) {
                        for (Path p : ds) Files.deleteIfExists(p);
                    }
                    Files.deleteIfExists(cacheDir);
                }
            } catch (IOException ignore) { /* 청소 실패는 무시 */ }
        }

        return urls;
    }

    // =========================================
    // 공용: URL 다운로드 (분석 단계 URL 지원용)
    // =========================================
    private void download(String urlStr, Path dst) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(15000);
        con.setInstanceFollowRedirects(true);

        int code = con.getResponseCode();
        if (code != 200) throw new IOException("이미지 다운로드 실패: HTTP " + code);

        String ct = con.getContentType();
        if (ct != null && !ct.startsWith("image/") && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(ct)) {
            con.disconnect();
            throw new IOException("이미지 Content-Type 아님: " + ct);
        }

        long contentLen = con.getContentLengthLong();
        if (contentLen > 0 && props.getMaxSizeBytes() > 0 && contentLen > props.getMaxSizeBytes()) {
            con.disconnect();
            throw new IOException("이미지 용량이 제한을 초과했습니다.");
        }

        try (InputStream in = con.getInputStream();
             OutputStream out = Files.newOutputStream(dst, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            StreamUtils.copy(in, out);
        } finally {
            con.disconnect();
        }
    }

    // 내부 표현: 업로드 파일 또는 캐시 파일
    private record Sink(MultipartFile multipart, Path cachePath, String ext) {
        static Sink of(MultipartFile mf) {
            String ext = mf != null ? StringUtils.getFilenameExtension(mf.getOriginalFilename()) : null;
            ext = (ext == null ? "" : ext.toLowerCase(Locale.ROOT));
            return new Sink(mf, null, ext);
        }
        static Sink of(Path cache) {
            String ext = cache != null ? StringUtils.getFilenameExtension(cache.getFileName().toString()) : null;
            ext = (ext == null ? "" : ext.toLowerCase(Locale.ROOT));
            return new Sink(null, cache, ext);
        }
        boolean isCacheMain(Path p) { return cachePath != null && cachePath.equals(p); }
    }
}