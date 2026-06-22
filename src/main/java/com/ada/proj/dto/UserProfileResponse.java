package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String uuid;
    private String adminId;
    private String customId;
    private String userRealname;
    private String userNickname;
    private boolean useNickname;
    private String profileImage;
    private String profileBanner;
    private String profileImageOutlineColor;
    private Role role;
    private String githubAccount;

    // user_data
    private String intro;
    private List<String> techStack;
    private String badge;
    private Integer activityScore;
    private String contributionData; // JSON 문자열
    private SocialLinks socialLinks;
    private ActivitySummary activitySummary;
}
