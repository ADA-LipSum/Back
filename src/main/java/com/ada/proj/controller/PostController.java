package com.ada.proj.controller;

import java.io.IOException;

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

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.service.FileStorageService;
import com.ada.proj.service.FileStorageService.StoredFile;
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
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
            description = "JSON 데이터(@RequestPart name=data)와 이미지/영상 파일을 함께 전송하면 서버가 파일을 저장하고 contentMd에 자동 삽입합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> createWithFiles(
            @Valid @RequestPart("data") PostCreateRequest data,
            @RequestPart(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestPart(value = "videoFiles", required = false) MultipartFile[] videoFiles,
            Authentication authentication
    ) throws IOException {
        if (authentication != null) {
            data.setWriterUuid(authentication.getName());
        }

        StringBuilder md = new StringBuilder();
        if (data.getContent() != null) { md.append(data.getContent()); }

        appendImages(data, imageFiles, md);
        appendVideos(data, videoFiles, md);

        data.setContent(md.toString());
        String uuid = postService.create(data);
        return ResponseEntity.ok(ApiResponse.success(uuid));
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
    @Operation(summary = "수정하기", description = "기존 게시글의 내용을 수정합니다 (게시글 ID 기준).", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> update(@RequestParam("uuid") String uuid,
                                                    @RequestBody PostUpdateRequest req,
                                                    Authentication authentication) {
        // 권한 검증이 필요하다면 authentication.getName()과 작성자 비교 로직을 Service로 전달
        postService.update(uuid, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 삭제
    @PostMapping("/delete")
    @Operation(summary = "삭제하기", description = "선택한 게시글을 삭제합니다 (게시글 ID 기준).", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam("uuid") String uuid,
                                                    Authentication authentication) {
        postService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 상세(+조회수)
    @GetMapping("/view")
    @Operation(summary = "자세히 보기", description = "게시글 상세 정보를 조회합니다 (게시글 ID 기준). 호출 시 조회수가 1 증가합니다.")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(@RequestParam("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid)));
    }

    // 목록
    @GetMapping("/list")
    @Operation(summary = "목록", description = "최신순 목록. page=0부터, size 기본 10")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Page<PostSummaryResponse> res = postService.list(page, size);
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}