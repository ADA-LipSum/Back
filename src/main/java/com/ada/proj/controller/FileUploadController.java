package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.service.S3Service;
import com.ada.proj.service.UserService;
import com.ada.proj.dto.UpdateProfileRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "파일 업로드", description = "S3를 이용한 프로필 이미지 / 배너 업로드 API")
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

    @PostMapping(value = "/profile-image/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드",
            description = "이미지 파일(jpeg/png/gif/webp, 최대 5MB)을 S3에 업로드하고, 해당 유저의 profileImage를 갱신합니다.")
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "업로드할 이미지 파일") @RequestPart("file") MultipartFile file,
            Authentication auth) {

        ensureSelfOrAdmin(auth, uuid);
        String url = s3Service.uploadProfileImage(file, uuid);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setProfileImage(url);
        userService.updateProfile(uuid, req);

        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PostMapping(value = "/banner/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "배너 이미지 업로드",
            description = "이미지 파일(jpeg/png/gif/webp, 최대 10MB)을 S3에 업로드하고, 해당 유저의 profileBanner를 갱신합니다.")
    public ResponseEntity<ApiResponse<String>> uploadBanner(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "업로드할 배너 파일") @RequestPart("file") MultipartFile file,
            Authentication auth) {

        ensureSelfOrAdmin(auth, uuid);
        String url = s3Service.uploadBanner(file, uuid);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setProfileBanner(url);
        userService.updateProfile(uuid, req);

        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    /**
     * 프로필 이미지 + 배너를 한 번에 S3 업로드하고 DB에 저장합니다. profileImage, banner 파일은 각각 선택적으로
     * 포함할 수 있습니다.
     */
    @PostMapping(value = "/profile/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 통합 수정",
            description = "프로필 이미지 / 배너를 선택적으로 받아 S3에 업로드한 뒤 DB에 한 번에 저장합니다.")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateProfileImages(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "프로필 이미지 (선택)") @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @Parameter(description = "배너 이미지 (선택)") @RequestPart(value = "banner", required = false) MultipartFile banner,
            Authentication auth) {

        ensureSelfOrAdmin(auth, uuid);

        UpdateProfileRequest req = new UpdateProfileRequest();
        Map<String, String> result = new LinkedHashMap<>();

        if (profileImage != null && !profileImage.isEmpty()) {
            String url = s3Service.uploadProfileImage(profileImage, uuid);
            req.setProfileImage(url);
            result.put("profileImageUrl", url);
        }
        if (banner != null && !banner.isEmpty()) {
            String url = s3Service.uploadBanner(banner, uuid);
            req.setProfileBanner(url);
            result.put("bannerUrl", url);
        }

        if (!result.isEmpty()) {
            userService.updateProfile(uuid, req);
        }

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
