# LipSum — 경북소프트웨어마이스터고 커뮤니티 플랫폼 백엔드

경북소프트웨어마이스터고등학교 구성원(학생·교직원)을 위한 커뮤니티 플랫폼의 REST API 서버입니다.
게시물·댓글·스터디 그룹·포인트 기반 마켓플레이스·프로필 관리 등 학교 커뮤니티에 필요한 기능을 제공합니다.

---

## 목차

- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [시작하기](#시작하기)
  - [사전 요구사항](#사전-요구사항)
  - [환경 변수](#환경-변수)
  - [로컬 실행](#로컬-실행)
  - [Docker 실행](#docker-실행)
- [배포](#배포)
- [API 문서](#api-문서)
  - [표준 응답 형식](#표준-응답-형식)
  - [에러 코드](#에러-코드)
  - [주요 엔드포인트](#주요-엔드포인트)
- [인증 흐름](#인증-흐름)
- [프로젝트 구조](#프로젝트-구조)
- [커밋 컨벤션](#커밋-컨벤션)

---

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 8 |
| Database | MySQL 8.0 (JPA/Hibernate) |
| Authentication | JWT (HS256) + Spring Security |
| OAuth | GitHub OAuth 2.0 |
| Storage | AWS S3 (ap-northeast-2) |
| Cache | Caffeine |
| Mapping | MapStruct |
| Documentation | SpringDoc OpenAPI 3 (Swagger UI) |
| Content | Flexmark (Markdown), OWASP HTML Sanitizer |
| CI/CD | GitHub Actions → GHCR → AWS EC2 |
| Container | Docker (multi-stage build) |

---

## 주요 기능

### 인증 & 사용자 관리

- **커스텀 로그인**: 관리자 발급 계정 기반의 ID/Password 로그인
- **GitHub OAuth**: GitHub 계정 연동 소셜 로그인
- **역할 기반 권한(RBAC)**: `ADMIN` / `TEACHER` / `STUDENT` / `MENTOR` 역할 분리
- **프로필 관리**: 닉네임, 소개글, 기술 스택, SNS 링크, 프로필·배너 이미지 (S3)
- **방명록**: 다른 사용자의 프로필에 방명록 작성
- **포트폴리오 프로젝트**: 개인 프로젝트 등록 및 관리

### 게시물 & 커뮤니티

- **게시물**: 마크다운 기반 작성, 페이지네이션, 좋아요, 조회수
- **댓글**: 대댓글(계층 구조), 좋아요, 고정 댓글 기능
- **신고 & 제재**: 게시물·사용자 신고 접수, 관리자 제재 처리

### 스터디 그룹

- **그룹 생성**: 공개/비공개 설정, 정원 제한
- **멤버 관리**: 가입 신청 → 승인 플로우, 역할(OWNER/MEMBER) 분리
- **상태 관리**: `RECRUITING` / `IN_PROGRESS` / `CLOSED`

### 포인트 & 마켓플레이스

- **포인트 시스템**: 지급·차감·사용 내역 관리, 잔액 조회
- **코인 시스템**: 별도 화폐 단위 관리 (관리자 지급)
- **마켓플레이스**: 아이템 등록·검색·구매·재고 관리, 장바구니
- **인벤토리**: 구매한 아이템 보관 및 조회

### 운영 & 관리

- **관리자 대시보드**: 웹 기반 어드민 UI (`/admin`)
- **S3 브라우저**: 업로드 파일 관리 (`/admin/s3`)
- **헬스 체크**: 애플리케이션 상태 모니터링

---

## 시작하기

### 사전 요구사항

- Java 17+
- MySQL 8.0+
- (선택) Docker & Docker Compose
- AWS S3 버킷 또는 IAM Role (EC2 배포 시 대체 가능)

### 환경 변수

프로젝트 루트의 `.env` 파일을 참고하여 환경 변수를 설정합니다.

```dotenv
# ── Database ──────────────────────────────────────────────
DB_URL=jdbc:mysql://localhost:3306/lipsumdb?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=your_password

# ── JWT ───────────────────────────────────────────────────
# 64자 이상의 랜덤 hex 문자열 권장
JWT_SECRET=hCB8xQ7mVYF2wzG3JpA9kL5rN2uS8eD1tQ6yZ3mB7cP4vX9sR1aT6uW3qE8nK5bM

# ── CORS ──────────────────────────────────────────────────
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,https://your-frontend.com

# ── Cookie ────────────────────────────────────────────────
# HTTPS 환경: true / HTTP 로컬 개발: false
COOKIE_SECURE=false

# ── AWS S3 ────────────────────────────────────────────────
# EC2 IAM Role 사용 시 아래 두 항목 생략 가능
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key

# ── GitHub OAuth ──────────────────────────────────────────
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_CALLBACK_URL=http://localhost:8080/api/auth/github/callback
FRONTEND_BASE_URL=http://localhost:3000

# ── Logging ───────────────────────────────────────────────
LOG_FILE=logs/app.log
```

### 로컬 실행

```bash
# 빌드 (테스트 제외)
./gradlew clean build -x test

# 실행
java -jar build/libs/proj-0.0.1-SNAPSHOT.jar

# 포트 변경
java -jar build/libs/proj-0.0.1-SNAPSHOT.jar --server.port=9090

# 프로덕션 프로파일 (DDL: validate)
java -jar build/libs/proj-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker 실행

```bash
# 이미지 빌드
docker build -t lipsum-backend .

# 컨테이너 실행 (.env 파일 사용)
docker run -d \
  --name lipsum \
  --env-file .env \
  -p 8080:8080 \
  lipsum-backend
```

---

## 배포

본 프로젝트는 GitHub Actions를 통한 CI/CD 파이프라인이 구성되어 있습니다.

```
main 브랜치 push
    │
    ▼
[build-and-push.yml]
Gradle 빌드 → Docker 이미지 빌드 → GHCR(GitHub Container Registry) 푸시
    │
    ▼
[deploy.yml]
EC2 SSH 접속 → 최신 이미지 Pull → docker compose up -d → 구 이미지 정리
```

**EC2 환경 요구사항:**

- Docker & Docker Compose 설치
- GHCR 인증 설정 (`docker login ghcr.io`)
- `.env` 파일 또는 IAM Role 구성

---

## API 문서

서버 실행 후 Swagger UI에서 전체 API 목록을 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

### 표준 응답 형식

모든 엔드포인트는 `ResponseEntity<ApiResponse<T>>` 형태로 응답합니다.

**성공**

```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

**실패**

```json
{
  "success": false,
  "errorCode": "USER_NOT_FOUND",
  "message": "해당 유저를 찾을 수 없습니다."
}
```

### 에러 코드

| 코드 | HTTP | 설명 |
| --- | --- | --- |
| `UNAUTHENTICATED` | 401 | 인증 필요 (토큰 없음) |
| `INVALID_PASSWORD` | 401 | 비밀번호 불일치 |
| `TOKEN_EXPIRED` | 401 | 액세스 토큰 만료 |
| `TOKEN_INVALID` | 401 | 토큰 형식/서명 오류 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `USER_BANNED` | 403 | 계정 정지 상태 |
| `USER_NOT_FOUND` | 404 | 존재하지 않는 유저 |
| `VALIDATION_ERROR` | 400 | 요청 값 검증 실패 |
| `BAD_REQUEST` | 400 | 잘못된 요청 |
| `CONFLICT` | 409 | 리소스 상태 충돌 |
| `AUTH_FAILURE` | 401 | 인증 전반 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### 주요 엔드포인트

#### 인증 (`/api/auth`)

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | ID/Password 로그인 | - |
| `POST` | `/api/auth/reissue` | 액세스 토큰 재발급 | Refresh Token |
| `POST` | `/api/auth/logout` | 로그아웃 | Required |
| `GET` | `/api/auth/github` | GitHub OAuth 시작 | - |
| `GET` | `/api/auth/github/callback` | GitHub OAuth 콜백 | - |
| `POST` | `/api/auth/admin/init` | 최초 어드민 계정 생성 (1회) | - |
| `POST` | `/api/auth/admin/create` | 사용자 계정 발급 | ADMIN |

#### 사용자 (`/api/users`)

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/users` | 사용자 목록 조회 (검색·역할 필터) | - |
| `GET` | `/api/users/{uuid}` | 사용자 프로필 조회 | - |
| `GET` | `/api/users/by-username/{username}` | customId로 프로필 조회 | - |
| `PATCH` | `/api/users/{uuid}/profile` | 프로필 수정 | Required |
| `PATCH` | `/api/users/{uuid}/role` | 역할 변경 | ADMIN |
| `POST` | `/api/users/{uuid}/custom` | 커스텀 ID 생성 (최초 1회) | Required |
| `PATCH` | `/api/users/{uuid}/custom/password` | 비밀번호 변경 | Required |

#### 게시물 (`/api/posts`)

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/posts` | 게시물 목록 (페이지네이션) | - |
| `POST` | `/api/posts` | 게시물 작성 | Required |
| `GET` | `/api/posts/{uuid}` | 게시물 상세 조회 | - |
| `PUT` | `/api/posts/{uuid}` | 게시물 수정 | Owner/ADMIN |
| `DELETE` | `/api/posts/{uuid}` | 게시물 삭제 | Owner/ADMIN |
| `POST` | `/api/posts/{uuid}/like` | 좋아요 토글 | Required |

#### 댓글 (`/api/comments`)

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/posts/{uuid}/comments` | 댓글 목록 조회 | - |
| `POST` | `/api/comments` | 댓글 작성 | Required |
| `PUT` | `/api/comments/{id}` | 댓글 수정 | Owner |
| `DELETE` | `/api/comments/{id}` | 댓글 삭제 | Owner/ADMIN |
| `POST` | `/api/comments/{id}/like` | 댓글 좋아요 토글 | Required |
| `POST` | `/api/comments/{id}/fixed` | 댓글 고정/해제 | Required |

#### 스터디 그룹 (`/api/studies`)

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/studies` | 그룹 목록 (페이지네이션) | - |
| `POST` | `/api/studies` | 그룹 생성 | Required |
| `GET` | `/api/studies/{id}` | 그룹 상세 조회 | - |
| `POST` | `/api/studies/{id}/join` | 가입 신청 | Required |
| `POST` | `/api/studies/{id}/leave` | 그룹 탈퇴 | Required |

#### 마켓플레이스 (`/api/trade`)

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/trade/items` | 아이템 목록 | - |
| `GET` | `/api/trade/items/search` | 아이템 검색 | - |
| `POST` | `/api/trade/items` | 아이템 등록 | ADMIN |
| `GET` | `/api/trade/items/{id}` | 아이템 상세 | - |
| `PUT` | `/api/trade/items/{id}` | 아이템 수정 | ADMIN |
| `DELETE` | `/api/trade/items/{id}` | 아이템 삭제 | ADMIN |
| `POST` | `/api/trade/purchase` | 아이템 구매 | Required |

#### 파일 업로드 (`/api/upload`)

| Method | Path | 설명 | 제한 |
| --- | --- | --- | --- |
| `POST` | `/api/upload/profile/{uuid}` | 프로필 이미지 업로드 | 15MB |
| `POST` | `/api/upload/banner/{uuid}` | 배너 이미지 업로드 | 20MB |

---

## 인증 흐름

### JWT 기반 인증

```
[클라이언트]  POST /api/auth/login { customId, password }
                       │
                       ▼
             AuthService: BCrypt 검증
                       │
                       ▼
             JwtTokenProvider 토큰 생성
             ├─ Access Token  (10분, Authorization 헤더)
             └─ Refresh Token (7일, HttpOnly Cookie)
                       │
                       ▼
[클라이언트]  이후 요청: Authorization: Bearer <accessToken>
```

### 토큰 재발급

```
Access Token 만료 (401 TOKEN_EXPIRED)
                       │
                       ▼
[클라이언트]  POST /api/auth/reissue
             (Refresh Token은 HttpOnly Cookie로 자동 전송)
                       │
                       ▼
             새 Access Token + 새 Refresh Token 발급
```

### GitHub OAuth

```
[클라이언트]  GET /api/auth/github
                       │
                       ▼
             GitHub 로그인 페이지 리다이렉트
                       │
                       ▼
             GET /api/auth/github/callback?code=...
                       │
                       ▼
             GitHub API로 사용자 정보 조회
             → 기존 계정 연동 또는 신규 계정 생성
                       │
                       ▼
             프론트엔드로 리다이렉트 (토큰 포함)
```

**쿠키 설정:**

| 속성 | 값 |
| --- | --- |
| HttpOnly | `true` |
| Secure | `true` (HTTPS) / `false` (로컬) |
| SameSite | `None` |
| Max-Age | 7일 |

---

## 프로젝트 구조

```
src/main/java/com/ada/proj/
├── ProjApplication.java
├── config/              # Security, CORS, Cache, S3 설정
├── controller/          # REST 컨트롤러 (20개)
├── service/             # 비즈니스 로직 (20개)
├── entity/              # JPA 엔티티 (26개)
├── repository/          # Spring Data JPA 레포지터리 (26개)
├── dto/                 # 요청/응답 DTO (75개)
├── security/            # JWT 필터, Provider, UserDetails
├── enums/               # Role, Status 등 열거형 (12개)
├── exception/           # 커스텀 예외 및 GlobalExceptionHandler
└── util/                # 공통 유틸리티

src/main/resources/
├── application.yml          # 기본 설정
├── application-prod.yml     # 프로덕션 설정 (DDL: validate)
└── logback-spring.xml       # 로깅 설정 (UTF-8, 파일 출력)
```

---

## 커밋 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 스펙을 따릅니다.

```
<type>(<scope>): <subject>

<body>
```

- **type**: 변경 성격 (필수)
- **scope**: 변경된 모듈/레이어 (선택)
- **subject**: 50자 이내, 현재형·명령형, 마침표 없음
- **body**: 무엇을/왜/어떻게 변경했는지 (선택)

### Type 목록

| Type | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 |
| `style` | 코드 의미 변화 없음 (포맷, 세미콜론 등) |
| `refactor` | 기능 변화 없는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `build` | 빌드 시스템·외부 의존성 변경 |
| `ci` | CI 구성·스크립트 변경 |
| `perf` | 성능 개선 |
| `chore` | 기타 잡무 |
| `revert` | 이전 커밋 되돌리기 |

### 예시

```text
feat(post): 게시글 작성 시 썸네일 자동 추출

본문 첫 이미지를 탐색하여 썸네일 필드에 저장하도록 로직 추가.
기존 API 응답 스키마에는 변화 없음.
```

```text
fix(auth): 토큰 만료 시 500 대신 401 반환

GlobalExceptionHandler에서 TokenExpiredException 매핑 누락 수정.
```
