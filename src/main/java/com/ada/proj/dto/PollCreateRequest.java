package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PollCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String question;

    @NotEmpty
    @Size(min = 2, max = 10)
    private List<@NotBlank @Size(max = 80) String> options;

    @Future
    private LocalDateTime endsAt;

    private Boolean anonymous;
}
