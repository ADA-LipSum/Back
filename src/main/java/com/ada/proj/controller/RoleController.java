package com.ada.proj.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "권한 관리", description = "역할 목록 및 역할별 사용자 조회 API")
public class RoleController {

    private final UserService userService;

    public RoleController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/roles")
    @Operation(
            summary = "역할 목록 조회",
            description = """
                    시스템에 정의된 역할 목록을 반환합니다.

                    **Request Body:** 없음

                    **Response:**
                    - `data`: 역할 목록 배열 (STUDENT | TEACHER | ADMIN)
                    """
    )
    public ResponseEntity<ApiResponse<List<Role>>> listRoles() {
        List<Role> roles = Arrays.asList(Role.values());
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @GetMapping("/roles/{role}")
    @Operation(
            summary = "역할별 유저 조회",
            description = """
                    특정 역할을 가진 사용자 목록을 반환합니다.

                    **Path Variable:**
                    - `role` (필수): 조회할 역할 (STUDENT | TEACHER | ADMIN)

                    **Response:**
                    - `data`: 해당 역할의 User 목록 배열
                    """
    )
    public ResponseEntity<ApiResponse<List<User>>> listUsersByRole(@PathVariable("role") Role role) {
        List<User> users = userService.listUsers(role, null);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}
