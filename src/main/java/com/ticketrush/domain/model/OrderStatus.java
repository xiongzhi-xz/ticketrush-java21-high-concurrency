package com.ticketrush.domain.model;

/**
 * 订单状态。
 */
public enum OrderStatus {

    PENDING,
    PAID,
    CANCELED,
    CLOSED;

    public boolean canClose() {
        return this == PENDING;
    }
}
