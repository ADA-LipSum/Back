package com.ada.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomLoginRequest {
    @NotBlank
    private String customId;

    @NotBlank
    @Size(min = 6, max = 255)
    private String password;
}
