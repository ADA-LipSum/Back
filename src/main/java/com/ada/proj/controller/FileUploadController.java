package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.S3Service;
import com.ada.proj.service.UserService;
import com.ada.proj.dto.UpdateProfileRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/profile-image/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드",
            description = "이미지 파일(jpeg/png/gif/webp, 최대 5MB)을 S3에 업로드하고, 해당 유저의 profileImage를 갱신합니다.")
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "업로드할 이미지 파일") @RequestPart("file") MultipartFile file) {

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
            @Parameter(description = "업로드할 배너 파일") @RequestPart("file") MultipartFile file) {

        String url = s3Service.uploadBanner(file, uuid);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setProfileBanner(url);
        userService.updateProfile(uuid, req);

        return ResponseEntity.ok(ApiResponse.ok(url));
    }
}
