package com.ticketrush.application.service;

/**
 * 抢票流量保护接口。
 *
 * <p>应用层只感知“能否进入抢票核心链路”，具体限流、热点参数保护由基础设施层实现。</p>
 */
public interface RushTrafficGuard {

    Permit enter(Long skuId);

    interface Permit extends AutoCloseable {

        @Override
        void close();
    }
}
