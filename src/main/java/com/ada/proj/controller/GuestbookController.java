package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.GuestbookRequest;
import com.ada.proj.dto.GuestbookResponse;
import com.ada.proj.service.GuestbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "방명록", description = "사용자 프로필 방명록 API")
public class GuestbookController {

    private final GuestbookService guestbookService;

    public GuestbookController(GuestbookService guestbookService) {
        this.guestbookService = guestbookService;
    }

    @GetMapping("/users/{uuid}/guestbook")
    @Operation(summary = "방명록 조회", description = "특정 사용자의 방명록을 최신순으로 조회합니다. 페이지네이션 지원 (?page=0&size=10)")
    public ResponseEntity<ApiResponse<Page<GuestbookResponse>>> list(
            @Parameter(description = "프로필 주인 UUID") @PathVariable String uuid,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.listEntries(uuid, pageable)));
    }

    @PostMapping("/users/{uuid}/guestbook")
    @Operation(summary = "방명록 작성", description = "로그인한 사용자가 방명록을 작성합니다.")
    public ResponseEntity<ApiResponse<GuestbookResponse>> add(
            @Parameter(description = "프로필 주인 UUID") @PathVariable String uuid,
            @Valid @RequestBody GuestbookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.addEntry(uuid, req, auth)));
    }

    @PatchMapping("/users/{uuid}/guestbook/{entryId}")
    @Operation(summary = "방명록 수정", description = "작성자(또는 ADMIN)가 본인 방명록을 수정합니다.")
    public ResponseEntity<ApiResponse<GuestbookResponse>> update(
            @Parameter(description = "프로필 주인 UUID") @PathVariable String uuid,
            @Parameter(description = "방명록 항목 ID") @PathVariable Long entryId,
            @Valid @RequestBody GuestbookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(guestbookService.updateEntry(uuid, entryId, req, auth)));
    }

    @DeleteMapping("/users/{uuid}/guestbook/{entryId}")
    @Operation(summary = "방명록 삭제", description = "작성자, 프로필 주인, 또는 ADMIN이 방명록을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "프로필 주인 UUID") @PathVariable String uuid,
            @Parameter(description = "방명록 항목 ID") @PathVariable Long entryId,
            Authentication auth) {
        guestbookService.deleteEntry(uuid, entryId, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("entry deleted"));
    }
}
