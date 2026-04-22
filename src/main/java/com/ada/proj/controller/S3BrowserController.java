package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.S3Service;
import com.ada.proj.service.S3Service.S3Item;
import com.ada.proj.service.S3Service.S3ObjectData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/s3")
@Tag(name = "S3 브라우저", description = "ADMIN 전용 S3 버킷 탐색 API")
public class S3BrowserController {

    private final S3Service s3Service;

    public S3BrowserController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/list")
    @Operation(summary = "S3 객체 목록 조회", description = "prefix 경로의 폴더/파일 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<List<S3Item>>> list(
            @RequestParam(defaultValue = "") String prefix) {
        return ResponseEntity.ok(ApiResponse.ok(s3Service.listObjects(prefix)));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "S3 객체 삭제", description = "key에 해당하는 S3 객체를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam String key) {
        s3Service.deleteByKey(key);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "S3 파일 업로드", description = "지정한 prefix 경로에 파일을 업로드합니다.")
    public ResponseEntity<ApiResponse<String>> upload(
            @RequestParam(defaultValue = "") String prefix,
            @RequestPart("file") MultipartFile file) {
        String url = s3Service.uploadToPrefix(file, prefix);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PostMapping(value = "/upload-admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "S3 어드민 파일 업로드", description = "파일 타입 제한 없이 지정한 prefix 경로에 파일을 업로드합니다.")
    public ResponseEntity<ApiResponse<String>> uploadAdmin(
            @RequestParam(defaultValue = "") String prefix,
            @RequestPart("file") MultipartFile file) {
        String url = s3Service.uploadToPrefixAdmin(file, prefix);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PutMapping(value = "/overwrite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "S3 파일 덮어쓰기", description = "key에 해당하는 S3 객체를 업로드한 파일로 덮어씁니다.")
    public ResponseEntity<ApiResponse<String>> overwrite(
            @RequestParam String key,
            @RequestPart("file") MultipartFile file) {
        String url = s3Service.overwriteByKey(file, key);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PostMapping("/mkdir")
    @Operation(summary = "S3 폴더 생성", description = "지정한 prefix 경로에 폴더를 생성합니다.")
    public ResponseEntity<ApiResponse<Void>> mkdir(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam String name) {
        s3Service.createFolder(prefix, name);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/proxy")
    @Operation(summary = "S3 파일 프록시", description = "CORS 우회용 — S3 파일 내용을 그대로 반환합니다.")
    public ResponseEntity<byte[]> proxy(@RequestParam String key) {
        S3ObjectData data = s3Service.getObject(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(data.contentType()))
                .body(data.bytes());
    }
}
