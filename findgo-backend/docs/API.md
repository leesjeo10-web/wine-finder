# 찾GO 1차 API

기본 경로: `/api`. JSON 요청은 `Content-Type: application/json`을 사용합니다.
목록·상세·홈·추천·사진 조회는 공개이며, 등록·변경·업로드·마이페이지는 로그인이 필요합니다.

## 인증과 CSRF

인증은 서버 세션과 HttpOnly `JSESSIONID` 쿠키를 사용합니다. JWT나 브라우저 저장소의 사용자 ID로 인증하지 않습니다.
모든 변경 요청(회원가입·로그인·로그아웃 포함)은 CSRF 토큰을 보내야 합니다.

1. `GET /auth/csrf` → `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`
2. 모든 요청에 `credentials: 'include'` 사용
3. POST/PUT/PATCH/DELETE 요청에 반환된 이름의 헤더와 토큰을 추가
4. 로그인·로그아웃 후 기존 토큰을 버리고 다시 `/auth/csrf` 호출

| 메서드 | 경로 | 요청 / 응답 |
| --- | --- | --- |
| GET | `/auth/csrf` | CSRF 헤더 이름과 토큰 |
| POST | `/auth/signup` | JSON `{email, name, password}` → 201 회원 정보 |
| POST | `/auth/login` | **폼 인코딩** `email=...&password=...` → 204 |
| GET | `/auth/me` | 로그인 사용자 `{id, name, email}` |
| POST | `/auth/logout` | 204, 세션 종료 |

이메일은 소문자로 통일합니다. 이름은 1~40자, 비밀번호는 8~64자입니다.
회원가입은 자동 로그인하지 않습니다. 비밀번호 해시는 응답에 포함되지 않습니다.
비밀번호를 평문이나 localStorage에 저장하지 마세요.

```js
const API = 'http://localhost:8080/api'; // 배포 시 같은 사이트의 '/api'
let csrf;

async function refreshCsrf() {
  const res = await fetch(`${API}/auth/csrf`, { credentials: 'include' });
  if (!res.ok) throw new Error('인증 토큰을 가져오지 못했습니다.');
  csrf = await res.json();
}

async function api(path, { method = 'GET', body, form = false } = {}) {
  const headers = {};
  if (!['GET', 'HEAD'].includes(method)) {
    if (!csrf) await refreshCsrf();
    headers[csrf.headerName] = csrf.token;
  }
  if (body !== undefined && !form) headers['Content-Type'] = 'application/json';
  const res = await fetch(API + path, {
    method, credentials: 'include', headers,
    body: body === undefined ? undefined : form ? body : JSON.stringify(body),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || '요청을 처리하지 못했습니다.');
  }
  return res.status === 204 ? undefined : res.json();
}

await api('/auth/signup', {
  method: 'POST', body: { email: 'student@example.com', name: '테스트 학생', password: 'example-password' },
});
await api('/auth/login', {
  method: 'POST', form: true,
  body: new URLSearchParams({ email: 'student@example.com', password: 'example-password' }),
});
csrf = undefined;
await refreshCsrf();
const me = await api('/auth/me');
// 로그아웃 뒤에도 csrf = undefined; await refreshCsrf(); 처리
```

`form: true`는 URLSearchParams 또는 FormData를 전달할 때 사용합니다.
브라우저가 Content-Type(사진의 경우 boundary 포함)을 설정하도록 직접 지정하지 않습니다.
CSRF 오류(403) 시 토큰을 갱신하고 사용자에게 재시도를 안내하세요. 권한 오류와 구분 없이 자동 재전송하지 마세요.

## 물품

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | `/items` | 검색·목록 |
| GET | `/items/{id}` | 상세 |
| POST | `/items` | 등록 → 201 |
| PUT | `/items/{id}` | 전체 수정 |
| PATCH | `/items/{id}/status` | `{ "status": "RESOLVED" }` 또는 `OPEN` |
| DELETE | `/items/{id}` | 삭제 → 204 |
| GET | `/me/items` | 내 물품 목록 |
| GET | `/home` | 통계와 최근 분실물·습득물 |
| GET | `/items/similar?name=우산&type=LOST` | 반대 유형의 미해결 물품 키워드 추천 |

목록과 내 물품은 같은 쿼리 파라미터를 지원합니다.

| 파라미터 | 값 |
| --- | --- |
| `keyword` | 이름·설명·장소·보관 장소의 부분 문자열. 최대 100자 |
| `type` | `LOST` / `FOUND`, 생략 시 전체 |
| `status` | `OPEN` / `RESOLVED`, 생략 시 전체 |
| `sort` | `latest`(기본) / `oldest` |
| `page` | 0부터 시작, 기본 0 |
| `size` | 1~100, 기본 20 |

응답 형식:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

등록/전체 수정 요청 예:

```json
{
  "type": "FOUND",
  "name": "검정 우산",
  "description": "손잡이에 파란 스티커가 있습니다.",
  "status": "OPEN",
  "location": "도서관",
  "storageLocation": "학생회실",
  "imageUrl": null,
  "lostFoundAt": "2026-10-01"
}
```

- 유형·이름·설명·장소·날짜는 필수이며 미래 날짜는 허용하지 않습니다.
- 이름·장소·보관 장소는 최대 100자, 설명은 최대 2000자입니다.
- 등록 시 `status` 생략은 `OPEN`. 전체 수정에서는 필수입니다.
- `imageUrl`은 사진 업로드 API가 반환한 본인 사진 URL 한 개 또는 null/빈 문자열입니다.
- 분실물의 `storageLocation`은 비웁니다. 전체 수정에서 선택 필드를 비우면 기존 값도 제거합니다.
- 서버가 `id`, `userId`, `createdAt`을 정하며 응답에는 위 필드와 함께 반환합니다.
- 다른 계정의 수정·상태 변경·삭제는 403입니다.
- 날짜는 `YYYY-MM-DD`, 등록 일시는 `YYYY-MM-DDTHH:mm:ss` 형식입니다.
- 추천은 반대 유형의 미해결 물품 중 최근 100건을 대상으로 이름·설명의 키워드 일치 수를 비교해 최대 3건을 반환합니다.
  `excludeId`로 수정 중인 물품을 제외할 수 있습니다. AI 분석은 사용하지 않습니다.
- `/home` 응답: `{total, lost, found, resolved, recentLost, recentFound}`. 최근 목록은 각각 최대 3건입니다.

## 사진 한 장

`POST /images`: multipart/form-data의 `file` 필드 → 201 `{ "imageUrl": "/api/images/UUID.jpg" }`

JPG/PNG 5MB 이하, 최대 2천만 화소를 허용합니다. 서버에서 최대 긴 변 1600px의 JPEG로 다시 저장합니다.
WEBP는 React에서 JPEG로 변환 후 전송하세요. 기존 데모의 사진 크기 변환도 JPEG를 출력합니다.
원본 파일명과 메타데이터는 저장하지 않습니다. 사진은 물품과 동일하게 공개 조회됩니다.
등록 전에 업로드한 사진도 저장되며, 미사용 사진 자동 정리는 추후 작업입니다.

```js
const data = new FormData();
data.append('file', file); // JPG/PNG 파일
const { imageUrl } = await api('/images', { method: 'POST', body: data, form: true });
// 이후 POST /items 또는 PUT /items/{id} 본문에 imageUrl 포함
// 다른 origin의 백엔드 직접 사용 시 <img src={'http://localhost:8080' + imageUrl} />
```

## 오류

- 400: 잘못된 입력/날짜/필터/파일
- 401: 로그인 필요 또는 비밀번호 오류
- 403: 작성자·사진 소유권 오류, CSRF 오류, 허용되지 않은 origin
- 404: 물품/사진 없음
- 409: 중복 이메일 등 데이터 충돌
- 413: 업로드 크기 제한 초과

비즈니스 오류는 `{ "message": "..." }`, 입력 검증 오류는 추가로 `errors` 목록을 반환합니다.
