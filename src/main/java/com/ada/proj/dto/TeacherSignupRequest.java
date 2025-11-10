package com.ada.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 선생님 자가 회원가입 요청 DTO
 */
@Data
public class TeacherSignupRequest {
    @NotBlank
    @Size(max = 50)
    private String teacherId; // 내부적으로 adminId 로 저장

    @NotBlank
    @Size(max = 10)
    private String userRealname;

    @NotBlank
    @Size(max = 10)
    private String userNickname;

    // 로그인에 사용할 커스텀 계정
    @NotBlank
    @Size(min = 3, max = 50)
    private String customId;

    @NotBlank
    @Size(min = 6, max = 255)
    private String password;
}
