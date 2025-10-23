package com.ada.proj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenReissueRequest {
    @NotBlank
    private String refreshToken;
}
