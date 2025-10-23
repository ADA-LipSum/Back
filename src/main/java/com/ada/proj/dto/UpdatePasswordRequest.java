package com.ada.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 255)
    private String newPassword;
}
