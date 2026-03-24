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
    @Operation(summary = "방명록 조회", description = "특정 사용자의 방명록을 최신순으로 전체 조회합니다.")
    public ResponseEntity<ApiResponse<List<GuestbookResponse>>> list(
            @Parameter(description = "프로필 주인 Custom ID") @PathVariable String customId) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.listEntries(customId)));
    }

    @PostMapping("/users/{customId}/guestbook")
    @Operation(summary = "방명록 작성", description = "로그인한 사용자가 방명록을 작성합니다.")
    public ResponseEntity<ApiResponse<GuestbookResponse>> add(
            @Parameter(description = "프로필 주인 Custom ID") @PathVariable String customId,
            @Valid @RequestBody GuestbookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.addEntry(customId, req, auth)));
    }

    @PatchMapping("/users/{customId}/guestbook")
    @Operation(summary = "방명록 수정", description = "로그인한 사용자가 본인이 작성한 방명록을 수정합니다.")
    public ResponseEntity<ApiResponse<GuestbookResponse>> update(
            @Parameter(description = "프로필 주인 Custom ID") @PathVariable String customId,
            @Valid @RequestBody GuestbookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.updateEntry(customId, req, auth)));
    }

    @DeleteMapping("/users/{customId}/guestbook")
    @Operation(summary = "방명록 삭제", description = "로그인한 사용자가 본인이 작성한 방명록을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "프로필 주인 Custom ID") @PathVariable String customId,
            Authentication auth) {
        guestbookService.deleteEntry(customId, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("entry deleted"));
    }
}
