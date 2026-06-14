package com.ticketrush.domain.model;

/**
 * 票档状态。
 */
public enum SkuStatus {

    DRAFT,
    ON_SALE,
    SOLD_OUT,
    OFF_SALE;

    public boolean canSell() {
        return this == ON_SALE;
    }
}
