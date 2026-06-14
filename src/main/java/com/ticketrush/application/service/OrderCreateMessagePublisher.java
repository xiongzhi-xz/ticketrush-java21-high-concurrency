package com.ticketrush.application.service;

import com.ticketrush.application.dto.OrderCreateMessage;

/**
 * 订单创建消息发布接口。
 */
public interface OrderCreateMessagePublisher {

    boolean publish(OrderCreateMessage message);
}
