package com.ada.proj.dto;

import com.ada.proj.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    private String adminId; // internal admin identifier for the user

    @NotBlank
    @Size(max = 10)
    private String userRealname;

    @NotBlank
    @Size(max = 10)
    private String userNickname;

    private Role role = Role.STUDENT;

    // optional: set initial custom login
    @Size(min = 3, max = 50)
    private String customId;

    @Size(min = 6, max = 255)
    private String password;
}
