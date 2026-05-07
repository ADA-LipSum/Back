package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

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
public class PollResponse {

    private Long id;
    private String postUuid;
    private String question;
    private boolean anonymous;
    private LocalDateTime endsAt;
    private boolean ended;
    private int totalVotes;
    private Long myOptionId;
    private List<PollOptionResponse> options;
}
