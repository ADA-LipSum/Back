package com.ada.proj.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeSummaryResponse {

    private String id;
    private String title;
    private String authorName;
    private String authorProfileImage;
    private LocalDateTime createdAt;
}
