package com.ada.proj.service;

import com.ada.proj.dto.PointsTransaction;

public interface PointsService {

    PointsTransaction usePoints(
            String userUuid,
            int amount,
            String reasonCode,
            String refUuid,
            String description
    );
}
