package com.ticketrush.application.service;

/**
 * 抢票请求准入接口。
 *
 * <p>用于实现请求令牌或轻量排队策略。准入成功后才允许进入库存扣减链路。</p>
 */
public interface RushAdmissionGuard {

    Permit acquire(Long skuId);

    interface Permit extends AutoCloseable {

        @Override
        void close();
    }
}
