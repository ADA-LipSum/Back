package com.ada.proj.dto;

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
public class PollOptionResponse {

    private Long id;
    private String text;
    private int voteCount;
    private List<PollVoterResponse> voters;
}
