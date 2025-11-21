# 경북소프트웨어마이스터고등학교 커뮤니티 백엔드 (LipSum)

Spring Boot 기반으로 경북소프트웨어마이스터고등학교 구성원(학생/교직원)이 소통·자료 공유·거래·포인트 기능을 활용할 수 있는 커뮤니티 플랫폼 백엔드입니다.

## 주요 기능

- 회원 관리: 역할(Role) 기반 권한, 닉네임/프로필/배너 이미지 관리, 커스텀 로그인(ID/PW) 생성
- 인증/인가: JWT 기반 인증, 표준 오류 응답(401/403)
- 게시물: 생성(파일 업로드), 수정, 삭제, 목록, 상세, 좋아요 토글
- 댓글: 작성/수정/삭제, 대댓글 구조, 좋아요/고정 토글
- 포인트: 지급/차감/사용 및 거래 로그
- 거래/아이템: 아이템 생성/검색/구매/재고 관리
- 파일 업로드: 이미지/영상 저장 + 본문 Markdown 삽입
- 헬스 체크: 기본 HealthController (확장 가능)

## 기술 스택

- Java / Spring Boot
- Spring Security (JWT)
- Gradle
- Jackson (JSON), Lombok(일부 클래스만)
- Swagger / OpenAPI

## 개발 환경 준비

```powershell
# 테스트 제외 빌드 (필요 시)
./gradlew clean build -x test

# 기본 빌드
./gradlew clean build

# 기본 프로필 실행
java -jar build/libs/app-0.0.1-SNAPSHOT.jar

# 포트 변경 시 실행
java -jar build/libs/app-0.0.1-SNAPSHOT.jar --server.port= [Your Port]

# 로그 리다이렉트 시
java -jar build/libs/app-0.0.1-SNAPSHOT.jar *> app.log

# 문제 발생시 확인용
java -version
Get-Content app.log -Wait

```

## 표준 API 응답 형식

모든 컨트롤러는 `ResponseEntity<ApiResponse<...>>`를 반환합니다.

### 성공

```json
{
  "success": true,
  "data": { /* 객체/배열/값/null */ },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

### 실패

```json
{
  "success": false,
  "errorCode": "USER_NOT_FOUND",
  "message": "해당 유저를 찾을 수 없습니다."
}
```

### ApiResponse 정적 메서드

- `ApiResponse.success(data)`, `ApiResponse.success()`
- `ApiResponse.successMessage(message)`
- `ApiResponse.error(errorCode, message)`
- (하위호환) `ApiResponse.fail(message)` → `errorCode = ERROR`

## 에러 코드 (ErrorCode Enum)

| 코드 | 설명 |
|------|------|
| USER_NOT_FOUND | 존재하지 않는 유저 |
| INVALID_PASSWORD | 비밀번호 불일치 |
| TOKEN_EXPIRED | 토큰 만료 |
| TOKEN_INVALID | 토큰 형식/검증 실패 |
| DUPLICATE_EMAIL | 이메일 중복 |
| FORBIDDEN | 권한 없음 |
| UNAUTHENTICATED | 인증 필요 |
| VALIDATION_ERROR | 요청 검증 실패 |
| BAD_REQUEST | 일반 잘못된 요청 |
| CONFLICT | 리소스 상태 충돌 |
| AUTH_FAILURE | 인증 전반 실패(포괄) |
| INTERNAL_ERROR | 서버 내부 오류 |

## 예외 매핑 (GlobalExceptionHandler)

| Exception | HTTP Status | ErrorCode |
|-----------|------------|-----------|
| IllegalArgumentException | 400 | BAD_REQUEST |
| MethodArgumentNotValidException | 400 | VALIDATION_ERROR |
| IllegalStateException | 409 | CONFLICT |
| UnauthenticatedException | 401 | UNAUTHENTICATED |
| InvalidCredentialsException | 401 | INVALID_PASSWORD |
| TokenExpiredException | 401 | TOKEN_EXPIRED |
| TokenInvalidException | 401 | TOKEN_INVALID |
| ForbiddenException | 403 | FORBIDDEN |
| UserNotFoundException | 404 | USER_NOT_FOUND |
| SecurityException | 403 | FORBIDDEN |
| (기타 Exception) | 500 | INTERNAL_ERROR |

## 엔드포인트 예시 (일부)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/posts | 게시글 생성 (multipart 지원) |
| GET  | /api/posts | 게시글 목록 조회 (page/size) |
| GET  | /api/posts/{uuid} | 게시글 상세 조회 |
| POST | /api/posts/{uuid}/like | 좋아요 토글 |
| GET  | /api/posts/{uuid}/comments | 해당 게시글 댓글 전체 조회 |
| POST | /api/comments | 댓글 작성 |
| PUT  | /api/comments/{id} | 댓글 수정 |
| DELETE | /api/comments/{id} | 댓글 삭제 |
| POST | /api/comments/{id}/like | 댓글 좋아요 토글 |
| POST | /api/comments/{id}/fixed | 댓글 고정/해제 |
| GET  | /users | 사용자 목록 (관리자) |
| PATCH | /users/{uuid}/profile | 사용자 프로필 수정 |
| POST | /users/{uuid}/profile/image | 프로필 이미지 업로드 |

> 더 많은 엔드포인트는 Swagger UI 또는 소스 코드 컨트롤러 패키지 참고.

## 기여 방법

1. 이슈 작성 → 브랜치(`feature/...`, `fix/...`) 생성
2. 커밋 컨벤션: `feat:`, `fix:`, `docs:`, `refactor:`, `test:` 등
3. PR 시 변경 목적/테스트 포인트 명시

---
