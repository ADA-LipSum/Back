package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(name = "GuestbookResponse", description = "방명록 항목")
public class GuestbookResponse {

    @Schema(description = "방명록 ID")
    private Long id;

    @Schema(description = "작성자 UUID")
    private String writerUuid;

    @Schema(description = "작성자 닉네임")
    private String writerName;

    @Schema(description = "작성자 Custom ID")
    private String writerId;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String writerProfileImage;

    @Schema(description = "방명록 내용")
    private String content;

    @Schema(description = "작성 시각")
    private Instant createdAt;

    @Schema(description = "수정 시각")
    private Instant updatedAt;
}
