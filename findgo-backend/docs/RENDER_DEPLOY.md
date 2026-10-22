# Render 배포 체크리스트 (Spring Boot + MySQL)

## 1. Render Web Service
- Repository: `leesjeo10-web/wine-finder`
- Branch: `main`
- Language: **Docker**
- Root Directory: `findgo-backend`
- Dockerfile Path: `./Dockerfile` (Root Directory 기준)
- Region: Singapore
- Compute: **Free ($0/month)** 를 직접 선택하고 최종 요금 표시를 확인하세요.
- Java 21 이미지를 사용하는 Dockerfile은 GitHub에 포함되어 있습니다.

## 2. MySQL
Render 컨테이너의 `localhost`는 사용자의 MacBook이 아닙니다.
Render에서 접근 가능한 외부 MySQL 호스트를 준비하고 네트워크 접근과 TLS 요구사항을 확인하세요.
필요한 DB와 사용자 권한을 생성한 후 다음 환경 변수를 Render의 **Environment** 화면에서 설정합니다.
실제 비밀번호를 GitHub, README, 스크린샷에 넣지 마세요.

| Key | Value |
| --- | --- |
| `DB_URL` | `jdbc:mysql://HOST:PORT/wine_finder` (호스팅 제공자의 TLS 요구사항 반영) |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `FRONTEND_ORIGINS` | 프론트엔드 배포 origin, 예: `https://example.vercel.app` |
| `COOKIE_SECURE` | `true` (HTTPS) |

`PORT`는 Render가 자동 제공하므로 별도로 설정할 필요가 없습니다.

## 3. 프론트 연결 주의
- API 기본 경로는 `/api`, Swagger는 `/swagger-ui/index.html`입니다.
- 프론트 요청에 `credentials: 'include'`가 필요합니다.
- **현재 세션 쿠키는 `SameSite=Lax`** 입니다. Vercel과 Render의 서로 다른 사이트 간 직접 호출에서는 브라우저가 세션 쿠키를 보내지 않을 수 있습니다.
- 가장 간단한 해결책은 프론트엔드 도메인에서 `/api`를 백엔드로 프록시하는 **same-origin 구성**입니다. CORS origin만 등록한다고 세션 문제가 해결되지는 않습니다.

## 4. 무료 플랜 주의
- Render Free 인스턴스는 유휴 시 중지되고 다시 시작할 때 지연될 수 있습니다.
- 무료 인스턴스의 로컬 디스크는 영구 저장소가 아닙니다. 현재 `UPLOAD_DIR`에 저장하는 사진은 재배포/재시작 시 유실될 수 있으므로 실제 운영 전 객체 스토리지로 옮겨야 합니다.
- DB가 외부 서비스라면 해당 서비스의 무료 제공 여부와 한도를 별도로 확인하세요.

## 5. 배포 확인
1. Render Logs에서 Docker 빌드와 Spring Boot 기동을 확인합니다.
2. `https://<your-render-host>/v3/api-docs` 또는 `/swagger-ui/index.html`에 접속합니다.
3. DB 접속 오류가 발생하면 `DB_URL`, 계정, TLS, 방화벽/허용 IP를 점검합니다.
4. 실제 프론트에서 회원가입/로그인/로그아웃/물품 API를 테스트합니다.
