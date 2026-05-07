package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PollResponse;
import com.ada.proj.dto.PollVoteRequest;
import com.ada.proj.service.PollService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/polls")
public class PollController {

    private final PollService pollService;

    @GetMapping("/posts/{postUuid}")
    public ResponseEntity<ApiResponse<PollResponse>> detail(
            @PathVariable String postUuid,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(pollService.getByPostUuid(postUuid, authentication)));
    }

    @PostMapping("/posts/{postUuid}/votes")
    public ResponseEntity<ApiResponse<PollResponse>> vote(
            @PathVariable String postUuid,
            @Valid @RequestBody PollVoteRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(pollService.vote(postUuid, request, authentication)));
    }
}
