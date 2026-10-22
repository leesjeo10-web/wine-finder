package org.example.winefinder.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 인증", description = "회원가입, 내 정보, CSRF 토큰")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Schema(description = "회원가입 입력 정보")
    public record SignupRequest(
            @Schema(description = "로그인에 사용할 이메일. 최대 254자", example = "student@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Email @Size(max = 254) String email,
            @Schema(description = "표시할 이름. 1~40자", example = "테스트 학생", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 40) String name,
            @Schema(description = "비밀번호. 8~64자", example = "example-password", accessMode = Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Size(min = 8, max = 64) String password) {}
    public record CsrfResponse(
            @Schema(description = "변경 요청에 추가할 헤더 이름", example = "X-CSRF-TOKEN") String headerName,
            @Schema(description = "서버에서 발급된 토큰. 로그인·로그아웃 후 다시 조회하세요.") String token) {}

    @Operation(summary = "CSRF 토큰 조회", description = "회원가입·로그인·로그아웃을 포함한 모든 변경 요청 전에 호출하세요. 반환된 headerName 이름으로 token을 전송합니다. 로그인·로그아웃 후 다시 조회하세요.")
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @Operation(summary = "회원가입", description = "이메일·이름·비밀번호로 가입합니다. 이메일은 소문자로 저장하며 중복 시 409입니다. 가입 후 별도로 로그인해야 합니다. 이메일 소유 인증은 하지 않습니다. CSRF 토큰이 필요합니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return auth.signup(request.email(), request.name(), request.password());
    }

    @Operation(summary = "내 회원 정보 조회", description = "로그인 세션이 필요합니다. 비밀번호 및 비밀번호 해시는 반환하지 않습니다. 로그인하지 않았으면 401입니다.")
    @GetMapping("/me")
    public MemberResponse me(@Parameter(hidden = true) Authentication authentication) {
        return MemberResponse.from(auth.current(authentication));
    }
}
