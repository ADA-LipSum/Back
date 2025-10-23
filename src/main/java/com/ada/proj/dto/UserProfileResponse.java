package com.ada.proj.dto;

import com.ada.proj.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String uuid;
    private String adminId;
    private String customId;
    private String userRealname;
    private String userNickname;
    private boolean useNickname;
    private String profileImage;
    private String profileBanner;
    private Role role;

    // user_data
    private String intro;
    private String techStack;
    private String links; // JSON 문자열
    private String badge;
    private Integer activityScore;
    private String contributionData; // JSON 문자열
}
