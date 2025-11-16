package com.ada.proj.controller;

import java.io.IOException;

import com.ada.proj.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.service.FileStorageService;
import com.ada.proj.service.FileStorageService.StoredFile;
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
@Tag(name = "게시물")
public class PostController {

    private final PostService postService;
    private final FileStorageService fileStorageService;

    // 파일 포함 생성
    @PostMapping(path = "/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(
            summary = "게시물 생성(파일 포함)",
            description = "@RequestPart('data') JSON에 title, content(contentMd 호환), isDev, devTags 포함 가능. 이미지/영상 파일 동시 업로드 지원.",
            security = @SecurityRequirement(name = "bearerAuth")
        )
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = { @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE) }
        ))
        public ResponseEntity<ApiResponse<String>> createWithFiles(
                @Parameter(name = "data", description = "게시물 본문 JSON (title, content, isDev, devTags). images/videos는 서버가 파일로 자동 설정합니다.", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = PostCreateRequest.class),
                        examples = @ExampleObject(value = "{\n  \"title\": \"제목\",\n  \"content\": \"본문\",\n  \"isDev\": true,\n  \"devTags\": \"spring\"\n}")))
            @Valid @RequestPart("data") PostCreateRequest data,
            @Parameter(name = "files", description = "이미지/영상 혼합 업로드 (단일 배열)",
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @Parameter(name = "imageFiles", description = "이미지 파일 배열(하위호환)",
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @Parameter(name = "videoFiles", description = "영상 파일 배열(하위호환)",
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "videoFiles", required = false) MultipartFile[] videoFiles,
            Authentication authentication
    ) throws IOException {
        if (authentication != null) {
            data.setWriterUuid(authentication.getName());
        }

        StringBuilder md = new StringBuilder();
        if (data.getContent() != null) { md.append(data.getContent()); }

        // 단일 files 배열도 허용(이미지/영상 자동 판별). 하위호환: imageFiles, videoFiles 유지
        appendMixedFiles(data, files, md);
        appendImages(data, imageFiles, md);
        appendVideos(data, videoFiles, md);

        data.setContent(md.toString());
        String uuid = postService.create(data);
        return ResponseEntity.ok(ApiResponse.success(uuid));
    }

    private void appendMixedFiles(PostCreateRequest data, MultipartFile[] files, StringBuilder md) throws IOException {
        if (files == null) return;
        String firstImg = null;
        String firstVid = null;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            String ct = f.getContentType();
            if (ct != null && ct.startsWith("image")) {
                StoredFile saved = fileStorageService.storeImage(f);
                if (firstImg == null) firstImg = saved.url();
                md.append("\n\n![image](").append(saved.url()).append(")\n");
            } else if (ct != null && ct.startsWith("video")) {
                StoredFile saved = fileStorageService.storeVideo(f);
                if (firstVid == null) firstVid = saved.url();
                md.append("\n\n<video controls src=\"")
                  .append(saved.url())
                  .append("\" style=\"max-width:100%\"></video>\n");
            } else {
                // 타입을 모르면 이미지로 시도(필요시 정책 변경)
                StoredFile saved = fileStorageService.storeImage(f);
                if (firstImg == null) firstImg = saved.url();
                md.append("\n\n![file](").append(saved.url()).append(")\n");
            }
        }
        if (firstImg != null && (data.getImages() == null || data.getImages().isBlank())) {
            data.setImages(firstImg);
        }
        if (firstVid != null && (data.getVideos() == null || data.getVideos().isBlank())) {
            data.setVideos(firstVid);
        }
    }

    private void appendImages(PostCreateRequest data, MultipartFile[] imageFiles, StringBuilder md) throws IOException {
        if (imageFiles == null) return;
        String firstImg = null;
        for (MultipartFile f : imageFiles) {
            if (f == null || f.isEmpty()) continue;
            StoredFile saved = fileStorageService.storeImage(f);
            if (firstImg == null) firstImg = saved.url();
            md.append("\n\n![image](").append(saved.url()).append(")\n");
        }
        if (firstImg != null && (data.getImages() == null || data.getImages().isBlank())) {
            data.setImages(firstImg);
        }
    }

    private void appendVideos(PostCreateRequest data, MultipartFile[] videoFiles, StringBuilder md) throws IOException {
        if (videoFiles == null) return;
        String firstVid = null;
        for (MultipartFile f : videoFiles) {
            if (f == null || f.isEmpty()) continue;
            StoredFile saved = fileStorageService.storeVideo(f);
            if (firstVid == null) firstVid = saved.url();
            md.append("\n\n<video controls src=\"")
              .append(saved.url())
              .append("\" style=\"max-width:100%\"></video>\n");
        }
        if (firstVid != null && (data.getVideos() == null || data.getVideos().isBlank())) {
            data.setVideos(firstVid);
        }
    }

    // 수정
    @PostMapping("/update")
    @Operation(summary = "수정", description = "title, content(또는 contentMd), isDev, devTags 선택 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> update(
                                                    @Parameter(description = "게시글 UUID", example = "post-uuid-...")
                                                    @RequestParam("uuid") String uuid,
                                                    @RequestBody PostUpdateRequest req,
                                                    Authentication authentication) {
        // 권한 검증이 필요하다면 authentication.getName()과 작성자 비교 로직을 Service로 전달
        postService.update(uuid, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 삭제
    @PostMapping("/delete")
    @Operation(summary = "삭제하기", description = "선택한 게시글을 삭제합니다 (게시글 ID 기준).", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(
                                                    @Parameter(description = "게시글 UUID", example = "post-uuid-...")
                                                    @RequestParam("uuid") String uuid,
                                                    Authentication authentication) {
        postService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 상세(+조회수)
    @GetMapping("/view")
    @Operation(summary = "자세히 보기", description = "게시글 상세 정보를 조회합니다 (게시글 ID 기준). 호출 시 조회수가 1 증가합니다.")
        public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @RequestParam("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid)));
    }

    @GetMapping("/list")
    @Operation(
            summary = "게시글 목록 조회",
            description = """
            게시판에 등록된 게시글들을 페이지 단위로 조회합니다.

            Parameters:
            - page: 조회할 페이지 번호 (기본값 0)
            - size: 한 페이지에 포함될 게시글 수 (기본값 20)

            Example:
            /post/list?page=0&size=20
            """
    )
    public ApiResponse<PageResponse<PostSummaryResponse>> list(
            @Parameter(description = "조회할 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "한 페이지에 포함될 게시글 개수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(postService.list(page, size));
    }
}