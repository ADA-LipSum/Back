package com.ada.proj.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 10)
    private String nickname;
    private String profileImage;
    private String profileBanner;
}
