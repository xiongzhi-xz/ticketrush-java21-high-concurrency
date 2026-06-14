package com.ticketrush.domain.model;

/**
 * 票务活动状态。
 */
public enum EventStatus {

    DRAFT,
    SCHEDULED,
    SELLING,
    SOLD_OUT,
    FINISHED,
    CANCELED;

    public boolean canSell() {
        return this == SELLING || this == SCHEDULED;
    }
}
