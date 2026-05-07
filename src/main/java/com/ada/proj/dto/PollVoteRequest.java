package com.ada.proj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PollVoteRequest {

    @NotNull
    private Long optionId;
}
