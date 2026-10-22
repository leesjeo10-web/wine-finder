package org.example.winefinder.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI findgoOpenApi() {
        return new OpenAPI().info(new Info().title("찾GO API").version("v1")
                .description("교내 분실물·습득물 서비스. 목록·상세·홈·추천·사진 조회는 공개입니다. "
                        + "등록·수정·삭제·사진 업로드에는 로그인 세션과 CSRF 토큰이 필요합니다. "
                        + "GET /api/auth/csrf에서 토큰을 발급받아 X-CSRF-TOKEN 헤더로 전송하세요. "
                        + "로그인은 Spring Security가 처리하는 POST /api/auth/login에 "
                        + "application/x-www-form-urlencoded 형식의 email·password를 전송합니다. "
                        + "로그아웃은 POST /api/auth/logout이며 성공 시 204를 반환합니다. "
                        + "로그인·로그아웃 후 CSRF 토큰을 다시 발급받으세요. "
                        + "회원가입은 이메일 소유 여부를 검증하지 않습니다."));
    }
}
