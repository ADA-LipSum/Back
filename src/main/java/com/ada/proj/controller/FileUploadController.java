package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.service.S3Service;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "파일 업로드", description = "S3 파일 업로드 — 프로필 이미지, 게시글 미디어(이미지·영상), 댓글 이미지")
public class FileUploadController {

    private final S3Service s3Service;
    private final UserService userService;

    public FileUploadController(S3Service s3Service, UserService userService) {
        this.s3Service = s3Service;
        this.userService = userService;
    }

    private void ensureSelfOrAdmin(Authentication auth, String targetUuid) {
        if (auth == null) throw new ForbiddenException("인증이 필요합니다.");
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !auth.getName().equals(targetUuid)) {
            throw new ForbiddenException("본인의 파일만 업로드할 수 있습니다.");
        }
    }

    @PostMapping(value = {"/profile-image/{uuid}", "/profile/{uuid}"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "프로필 이미지 업로드",
            description = """
                    프로필 이미지를 S3에 업로드하고 DB에 저장합니다. 본인 또는 ADMIN만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body (multipart/form-data):**
                    - `file` (필수): 업로드할 이미지 파일 (jpeg/png/gif/webp, 최대 15MB)

                    **Response:**
                    - `data`: 업로드된 이미지의 S3 URL (String)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 형식 오류 또는 파일 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"success":false,"code":"BAD_REQUEST","message":"허용되지 않는 파일 형식입니다. (jpeg, png, gif, webp만 허용)"}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "파일 크기 초과 (최대 15MB)",
                    content = @Content(examples = @ExampleObject(value = """
                            {"success":false,"code":"BAD_REQUEST","message":"15MB까지만 업로드 가능합니다."}""")))
    })
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "업로드할 이미지 파일") @RequestPart("file") MultipartFile file,
            Authentication auth) {

        ensureSelfOrAdmin(auth, uuid);
        String url = s3Service.uploadProfileImage(file, uuid);

        userService.updateProfileImage(uuid, url);

        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PostMapping(value = "/community/post-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "게시글 첨부 미디어 업로드",
            description = """
                    게시글에 첨부할 이미지 또는 영상을 S3에 업로드하고 URL을 반환합니다. 로그인이 필요합니다.

                    **허용 형식:** jpeg, png, gif, webp, svg (최대 15MB) / mp4 (최대 100MB)

                    **Request Body (multipart/form-data):**
                    - `file` (필수): 업로드할 파일

                    **Response:**
                    - `data`: 업로드된 파일의 S3 URL (String)
                    """
    )
    public ResponseEntity<ApiResponse<String>> uploadPostMedia(
            @Parameter(description = "업로드할 이미지/영상 파일") @RequestPart("file") MultipartFile file,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) throw new ForbiddenException("인증이 필요합니다.");
        String userUuid = auth.getName();
        String url = s3Service.uploadPostMedia(file, userUuid);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PostMapping(value = "/community/comment-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "댓글 첨부 이미지 업로드",
            description = """
                    댓글에 첨부할 이미지를 S3에 업로드하고 URL을 반환합니다. 로그인이 필요합니다.

                    **허용 형식:** jpeg, png, gif, webp, svg (최대 15MB) — mp4 불가

                    **Request Body (multipart/form-data):**
                    - `file` (필수): 업로드할 이미지 파일

                    **Response:**
                    - `data`: 업로드된 파일의 S3 URL (String)
                    """
    )
    public ResponseEntity<ApiResponse<String>> uploadCommentImage(
            @Parameter(description = "업로드할 이미지 파일") @RequestPart("file") MultipartFile file,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) throw new ForbiddenException("인증이 필요합니다.");
        String userUuid = auth.getName();
        String url = s3Service.uploadCommentImage(file, userUuid);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

}
