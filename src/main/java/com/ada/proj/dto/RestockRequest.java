package com.ada.proj.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestockRequest {
    private String itemUuid;
    private int amount;
}