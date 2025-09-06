package com.manil.manil.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 프론트 오리진만 명시적으로 허용
        List<String> allowedOrigins = List.of(
                "http://localhost:3000",
                "http://43.201.10.255",
                "http://https://2025-seasonthon-team-56-fe.vercel.app:3000"
        );

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);                  // ← allowCredentials=false일 땐 이걸 쓰자
        // cfg.setAllowedOriginPatterns(List.of("*"));          // (개발 중 와일드카드가 필요하면 이 줄로 교체)
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(false);                         // 쿠키/인증 필요없으면 false 권장
        cfg.setMaxAge(3600L);                                   // preflight 캐시 1시간

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        // API만 CORS 적용. /images/** 는 Nginx가 처리하므로 제외
        src.registerCorsConfiguration("/api/**", cfg);

        return new CorsFilter(src);
    }
}