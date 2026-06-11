package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.ProjectSelectionRequest;
import com.ada.proj.dto.UserProjectRequest;
import com.ada.proj.dto.UserProjectResponse;
import com.ada.proj.service.UserProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "프로젝트", description = "사용자 프로젝트 등록/조회/삭제 API")
public class UserProjectController {

    private final UserProjectService userProjectService;

    public UserProjectController(UserProjectService userProjectService) {
        this.userProjectService = userProjectService;
    }

    @GetMapping("/users/{uuid}/projects")
    @Operation(summary = "프로젝트 목록 조회", description = "특정 사용자의 프로젝트 목록을 최신순으로 조회합니다.")
    public ResponseEntity<ApiResponse<List<UserProjectResponse>>> list(
            @Parameter(description = "사용자 UUID") @PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.ok(userProjectService.listProjects(uuid)));
    }

    @PostMapping("/users/{uuid}/projects")
    @Operation(summary = "프로젝트 등록", description = "본인 또는 ADMIN이 프로젝트를 등록합니다.")
    public ResponseEntity<ApiResponse<UserProjectResponse>> add(
            @Parameter(description = "사용자 UUID") @PathVariable String uuid,
            @Valid @RequestBody UserProjectRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(userProjectService.addProject(uuid, req, auth)));
    }

    @PatchMapping("/users/{uuid}/projects/{projectId}")
    @Operation(summary = "프로젝트 수정", description = "본인 또는 ADMIN이 프로젝트를 수정합니다.")
    public ResponseEntity<ApiResponse<UserProjectResponse>> update(
            @Parameter(description = "사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Valid @RequestBody UserProjectRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(userProjectService.updateProject(uuid, projectId, req, auth)));
    }

    @DeleteMapping("/users/{uuid}/projects/{projectId}")
    @Operation(summary = "프로젝트 삭제", description = "본인 또는 ADMIN이 프로젝트를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            Authentication auth) {
        userProjectService.deleteProject(uuid, projectId, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("project deleted"));
    }

    @PatchMapping("/admin/user-projects/{projectId}/selection")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "프로젝트 순위 선정",
            description = """
                    관리자/선생님이 프로젝트를 우수 프로젝트(순위)로 선정하거나 선정을 해제합니다.
                    `selected`를 true로 설정하면 해당 프로젝트의 작성자에게 코인이 지급됩니다 (프로젝트당 1회).
                    """
    )
    public ResponseEntity<ApiResponse<UserProjectResponse>> select(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Valid @RequestBody ProjectSelectionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userProjectService.setSelected(projectId, req.getSelected())));
    }
}
