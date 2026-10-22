package org.example.winefinder.auth;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "회원 공개 응답. 비밀번호는 포함하지 않습니다.")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1") Long id,
        @Schema(description = "회원 이름", example = "테스트 학생") String name,
        @Schema(description = "이메일", example = "student@example.com") String email) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail());
    }
}
