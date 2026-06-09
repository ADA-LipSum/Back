package com.ada.proj.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("ADA 백엔드 API")
                        .version("v2"))
                .servers(List.of(new Server().url("/").description("현재 서버")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .tags(List.of(
                        // ── 인증 / 사용자 ─────────────────────────────
                        new Tag().name("로그인/인증").description("회원가입, 로그인, 토큰 갱신, 소셜 로그인"),
                        new Tag().name("GitHub OAuth").description("GitHub 소셜 로그인 연동"),
                        new Tag().name("회원/프로필").description("사용자 프로필 조회·수정, 역할 관리"),
                        new Tag().name("권한 관리").description("역할(Role) 목록 조회·수정"),

                        // ── 커뮤니티 ─────────────────────────────────
                        new Tag().name("일반 커뮤니티").description("잡담·밈·프로젝트 자랑 피드 — CRUD, 미디어 필터, 좋아요, 북마크"),
                        new Tag().name("개발 커뮤니티").description("개발 피드 — 질문·프로젝트·자료 공유, 언어 필터"),
                        new Tag().name("이모지 반응").description("게시글 이모지 반응 추가·제거·조회 (디스코드 스타일)"),
                        new Tag().name("댓글").description("댓글 작성·수정·삭제·좋아요, 고정 댓글"),
                        new Tag().name("투표").description("투표 생성, 참여, 결과 조회"),
                        new Tag().name("커뮤니티 배너").description("커뮤니티 상단 배너 슬라이드 관리"),
                        new Tag().name("커뮤니티 위젯").description("우측 위젯 — 내 활동 통계, 인기 개발 태그"),
                        new Tag().name("이벤트 위젯").description("다가오는 이벤트 위젯 — 조회 비로그인 허용, 등록·수정·삭제는 ADMIN/TEACHER 전용"),

                        // ── 공지사항 ─────────────────────────────────
                        new Tag().name("공지사항").description("공지사항 CRUD, 고정·해제 — 작성·수정·삭제·고정은 ADMIN 전용"),

                        // ── 블로그 ───────────────────────────────────
                        new Tag().name("블로그").description("개인 블로그 게시글 CRUD"),

                        // ── 학교 정보 ─────────────────────────────────
                        new Tag().name("급식").description("NEIS 연동 학교 급식 정보 (조식·중식·석식), Redis 12시간 캐싱"),
                        new Tag().name("학사 일정").description("학사 일정 월별 조회 + 관리자·교사 직접 등록"),

                        // ── 소셜 ─────────────────────────────────────
                        new Tag().name("스터디 그룹").description("스터디 그룹 생성·가입·관리"),
                        new Tag().name("방명록").description("사용자 프로필 방명록 작성·조회·삭제"),
                        new Tag().name("프로젝트").description("사용자 포트폴리오 프로젝트 CRUD"),

                        // ── 경제 시스템 ──────────────────────────────
                        new Tag().name("거래소").description("아이템 거래소 — 등록·구매·검색"),
                        new Tag().name("포인트").description("포인트 잔액 조회·획득·차감"),
                        new Tag().name("코인").description("코인 잔액 조회·관리 (관리자 지급)"),
                        new Tag().name("인벤토리").description("사용자 아이템 인벤토리 조회"),
                        new Tag().name("커스텀 스티커").description("커스텀 스티커 신청·심사 (관리자)"),

                        // ── 관리자 ───────────────────────────────────
                        new Tag().name("관리자 도구").description("대시보드 통계, 사용자 생성, 게시글 관리 (ADMIN 전용)"),
                        new Tag().name("신고 관리").description("게시글·사용자 신고 접수·처리 (관리자)"),
                        new Tag().name("제재 관리").description("사용자 정지 처리·해제 (관리자)"),

                        // ── 시스템 ───────────────────────────────────
                        new Tag().name("파일 업로드").description("S3 파일 업로드 — 프로필, 게시글 미디어, 댓글 이미지, 공지사항 첨부파일"),
                        new Tag().name("알림").description("시스템 알림 조회, 읽음 처리"),
                        new Tag().name("S3 브라우저").description("S3 버킷 내용 탐색 (관리자)"),
                        new Tag().name("서비스 상태").description("헬스 체크, 서비스 메트릭")
                ));
    }
}
