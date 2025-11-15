package com.ada.proj.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ada.proj.dto.PointsTransaction;

@Service
public class DefaultPointsService implements PointsService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPointsService.class);

    @Override
    public PointsTransaction usePoints(String userUuid, int amount, String reasonCode, String refUuid, String description) {
        log.warn("PointsService not fully implemented. Simulating points deduction. userUuid={}, amount={}, reasonCode={}",
                userUuid, amount, reasonCode);

        return PointsTransaction.create(userUuid, amount, reasonCode, refUuid, description);
    }
}
