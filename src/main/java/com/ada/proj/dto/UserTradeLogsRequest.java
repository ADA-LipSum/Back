//C:\Users\russe\Documents\GitHub\Ada\Back\src\main\java\com\ada\proj\dto\UserTradeLogsRequest.java
package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "UserTradeLogsRequest", description = "특정 사용자 구매내역 조회 요청 바디")
public class UserTradeLogsRequest {

    @NotBlank
    @Schema(description = "조회 대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String userUuid;

    @Schema(description = "페이지(0부터, 기본 0)", example = "0")
    private Integer page;

    @Schema(description = "페이지 크기(기본 20)", example = "20")
    private Integer size;
}