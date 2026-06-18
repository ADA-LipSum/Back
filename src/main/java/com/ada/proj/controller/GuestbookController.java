package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.GuestbookRequest;
import com.ada.proj.dto.GuestbookResponse;
import com.ada.proj.service.GuestbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "방명록", description = "사용자 프로필 방명록 API")
public class GuestbookController {

    private final GuestbookService guestbookService;

    public GuestbookController(GuestbookService guestbookService) {
        this.guestbookService = guestbookService;
    }

    @GetMapping("/users/{customId}/guestbook")
    @Operation(
            summary = "방명록 조회",
            description = """
                    특정 사용자의 방명록 목록을 최신순으로 전체 조회합니다. 인증 불필요.

                    **Path Variable:**
                    - `customId` (필수): 방명록 주인의 Custom ID

                    **Response:**
                    - `data` : 방명록 항목 배열
                      - `id` : 방명록 ID (Long)
                      - `writerUuid` : 작성자 UUID (String)
                      - `writerName` : 작성자 닉네임 (String)
                      - `writerId` : 작성자 Custom ID (String)
                      - `writerProfileImage` : 작성자 프로필 이미지 URL (String)
                      - `content` : 방명록 내용 (String)
                      - `createdAt` : 작성 시각 (ISO 8601)
                      - `updatedAt` : 수정 시각 (ISO 8601)
                    """
    )
    public ResponseEntity<ApiResponse<List<GuestbookResponse>>> list(
            @Parameter(description = "방명록 주인 Custom ID", example = "hong123") @PathVariable String customId) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.listEntries(customId)));
    }

    @PostMapping("/users/{customId}/guestbook")
    @Operation(
            summary = "방명록 작성",
            description = """
                    로그인한 사용자가 특정 사용자의 방명록에 글을 작성합니다. **JWT 인증 필요.**

                    **Path Variable:**
                    - `customId` (필수): 방명록 주인의 Custom ID

                    **Request Body (application/json):**
                    - `content` (필수): 방명록 내용 (최대 500자)

                    **Response:**
                    - `data` : 작성된 방명록 항목
                      - `id` : 방명록 ID (Long)
                      - `writerUuid` : 작성자 UUID (String)
                      - `writerName` : 작성자 닉네임 (String)
                      - `writerId` : 작성자 Custom ID (String)
                      - `writerProfileImage` : 작성자 프로필 이미지 URL (String)
                      - `content` : 방명록 내용 (String)
                      - `createdAt` : 작성 시각 (ISO 8601)
                      - `updatedAt` : 수정 시각 (ISO 8601)

                    **제약:** 사용자당 방명록 1개만 작성 가능. 이미 작성한 경우 수정 API를 사용하세요.
                    """
    )
    public ResponseEntity<ApiResponse<GuestbookResponse>> add(
            @Parameter(description = "방명록 주인 Custom ID", example = "hong123") @PathVariable String customId,
            @Valid @RequestBody GuestbookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.addEntry(customId, req, auth)));
    }

    @PatchMapping("/users/{customId}/guestbook")
    @Operation(
            summary = "방명록 수정",
            description = """
                    로그인한 사용자가 본인이 작성한 방명록을 수정합니다. **JWT 인증 필요.**

                    **Path Variable:**
                    - `customId` (필수): 방명록 주인의 Custom ID

                    **Request Body (application/json):**
                    - `content` (필수): 수정할 내용 (최대 500자)

                    **Response:**
                    - `data` : 수정된 방명록 항목
                      - `id` : 방명록 ID (Long)
                      - `writerUuid` : 작성자 UUID (String)
                      - `writerName` : 작성자 닉네임 (String)
                      - `writerId` : 작성자 Custom ID (String)
                      - `writerProfileImage` : 작성자 프로필 이미지 URL (String)
                      - `content` : 수정된 방명록 내용 (String)
                      - `createdAt` : 작성 시각 (ISO 8601)
                      - `updatedAt` : 수정 시각 (ISO 8601)
                    """
    )
    public ResponseEntity<ApiResponse<GuestbookResponse>> update(
            @Parameter(description = "방명록 주인 Custom ID", example = "hong123") @PathVariable String customId,
            @Valid @RequestBody GuestbookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.updateEntry(customId, req, auth)));
    }
}
