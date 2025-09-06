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
import java.util.concurrent.atomic.AtomicInteger;

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

    // ---------- 분석 단계: 0번만 캐시에 저장 ----------
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

    // ---------- 등록 단계: 전체를 정식 경로로 ----------
    public List<String> finalizeProductImages(Long productId, String analyzeId, List<String> imageUrls, int startIndex) throws IOException {
        Path productDir = Path.of(props.getRootDir(), "products", String.valueOf(productId));
        ensureDir(productDir);

        List<String> finalUrls = new ArrayList<>();
        AtomicInteger idx = new AtomicInteger(startIndex);

        // 1) analyzeId의 main.* → 0번으로 이동
        if (analyzeId != null && !analyzeId.isBlank()) {
            Path cacheDir = Path.of(props.getRootDir(), "cache", analyzeId);
            if (Files.exists(cacheDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheDir, "main.*")) {
                    for (Path p : ds) {
                        String ext = extFromNameOrUrl(p.getFileName().toString());
                        if (!allowed().contains(ext)) continue;
                        Path dst = productDir.resolve(nextFileName(0, ext)); // 무조건 0번으로
                        Files.move(p, dst, StandardCopyOption.REPLACE_EXISTING);
                        finalUrls.add(urlJoin(props.getPublicBaseUrl(), "products", String.valueOf(productId), dst.getFileName().toString()));
                        break;
                    }
                }
            }
        }

        // 2) 나머지 imageUrls 저장 (1부터 순번)
        int fileOrdinal = 1;
        if (imageUrls != null) {
            for (String u : imageUrls) {
                if (u == null || u.isBlank()) continue;

                // 캐시 main URL을 그대로 넣어왔으면 중복 스킵
                if (finalUrls.stream().anyMatch(u::equals)) continue;

                String ext = extFromNameOrUrl(u);
                if (!allowed().contains(ext)) continue;

                Path dst = productDir.resolve(nextFileName(fileOrdinal++, ext));

                if (u.startsWith(props.getPublicBaseUrl())) {
                    String relative = u.substring(props.getPublicBaseUrl().length());
                    Path src = Path.of(props.getRootDir(), relative);
                    if (Files.exists(src)) {
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        download(u, dst);
                    }
                } else {
                    download(u, dst);
                }
                finalUrls.add(urlJoin(props.getPublicBaseUrl(), "products", String.valueOf(productId), dst.getFileName().toString()));
            }
        }
        return finalUrls;
    }

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
        if (contentLen > 0) {
            if (props.getMaxSizeBytes() > 0 && contentLen > props.getMaxSizeBytes()) {
                con.disconnect();
                throw new IOException("이미지 용량이 제한을 초과했습니다.");
            }
        }

        try (InputStream in = con.getInputStream();
             OutputStream out = Files.newOutputStream(dst, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            StreamUtils.copy(in, out);
        } finally {
            con.disconnect();
        }
    }
}