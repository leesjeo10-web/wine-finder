# 찾GO 백엔드

교내 분실물·습득물 서비스의 1차 Spring Boot 백엔드입니다.
Java 21, Spring Boot 4.1.1, Spring Security, JPA, MySQL를 사용합니다.
첨부된 두 ZIP의 프론트는 HTML/CSS/JavaScript이며 React 전환·API 연결은 별도 작업입니다.

## 구현 범위

- 이메일·비밀번호 회원가입, 세션 로그인·로그아웃, 내 정보 조회
- PBKDF2 비밀번호 해시, CSRF 보호, 허용된 프론트 주소의 CORS 요청
- 물품 등록·조회·수정·삭제, 작성자 권한, 해결 상태 변경
- 검색, 유형·상태 필터, 최신/오래된 순 정렬, 페이지 조회
- 내 등록 물품 목록, 홈 통계와 최근 물품
- 사진 1장 업로드: 5MB 이하 JPG/PNG, 해상도 검사 및 JPEG 재인코딩
- 키워드 기반 비슷한 물품 추천

채팅, AI 이미지 분석, 이메일 소유 인증, 비밀번호 재설정, 소셜 로그인, 알림은 제외했습니다.
회원가입 후 로그인 요청을 따로 보내야 합니다. 이메일 소유 여부나 재학생 신분은 검증하지 않습니다.

## 실행

1. Java 21과 MySQL를 준비합니다.
2. MySQL에 `wine_finder` 데이터베이스와 접근 가능한 사용자 계정을 만듭니다.

```sql
CREATE DATABASE wine_finder CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 실행할 셸 또는 IDE 환경 변수에 다음 값을 설정합니다.

| 환경 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_USERNAME` | MySQL 사용자 | 필수 |
| `DB_PASSWORD` | MySQL 비밀번호 | 필수 |
| `DB_URL` | JDBC 주소 | `jdbc:mysql://localhost:3306/wine_finder` |
| `PORT` | 서버 포트 | `8080` |
| `FRONTEND_ORIGINS` | 허용 프론트 origin. 여러 개면 쉼표 구분 | `http://localhost:5173` |
| `UPLOAD_DIR` | 사진 저장 디렉터리 | `./uploads` |
| `COOKIE_SECURE` | HTTPS 환경에서 `true` | `false` |

환경 변수 파일을 자동으로 읽는 기능은 없습니다. IDE 실행 설정이나 셸에 값을 넣으세요.

```bash
./gradlew bootRun
```

- API: `http://localhost:8080/api`
- Swagger 화면: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI 문서: `http://localhost:8080/v3/api-docs`
- 개발 중에는 JPA `ddl-auto=update`로 테이블을 생성·갱신합니다.

## 테스트·빌드

```bash
./gradlew test
./gradlew bootJar
```

테스트는 `test` 프로필의 H2 데이터베이스와 임시 사진 디렉터리를 사용해 실제 MySQL 데이터에 접근하지 않습니다.
인증, CSRF, 권한, 검색·필터·정렬, 통계·추천, 사진 처리 및 CORS를 확인합니다.
MySQL 환경의 접속·배포 검증은 따로 필요합니다.

## 프론트 연결

[API 명세와 연결 예제](docs/API.md)를 참고하세요.
현재 프론트의 `localStorage` 데이터·데모 로그인은 서버로 자동 이전되지 않습니다.
서버가 반환하는 물품 `status`는 불리언 대신 `OPEN` 또는 `RESOLVED`입니다.

## 배포 시 설정

프론트와 `/api`를 같은 사이트에서 제공하는 구성을 권장합니다. 예를 들어 React 사이트의 `/api` 요청을
Spring Boot 서버로 프록시하면 세션 쿠키를 그대로 사용할 수 있습니다. 로컬은 `localhost:5173`과
`localhost:8080`을 사용하며 모든 API 요청에 `credentials: 'include'`를 설정합니다.
서로 다른 사이트의 도메인으로 직접 호출하는 배포는 현재 `SameSite=Lax` 쿠키 구성의 대상이 아닙니다.

HTTPS 배포에서는 `COOKIE_SECURE=true`, 실제 프론트 origin, 영구 보존되는 `UPLOAD_DIR`을 설정하세요.
서버 재시작 시 로그인 세션은 초기화되며 계정·물품은 DB, 사진은 디스크에 남습니다.
서버가 여러 대인 구성과 클라우드 사진 저장소는 1차 범위에 포함하지 않았습니다.

이전 DB에 이미 있던 물품은 `userId`가 없어 조회만 가능합니다. 임의 사용자에게 소유권을 부여하지 않습니다.
