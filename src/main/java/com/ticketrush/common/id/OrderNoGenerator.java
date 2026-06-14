package com.ticketrush.common.id;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成器。
 *
 * <p>当前使用时间戳 + 进程内序列号满足本地演示；生产环境可替换为雪花算法或号段服务。</p>
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int MAX_SEQUENCE = 9999;

    private final AtomicInteger sequence = new AtomicInteger();

    public String nextOrderNo() {
        int next = sequence.updateAndGet(value -> value >= MAX_SEQUENCE ? 0 : value + 1);
        return "TR" + LocalDateTime.now().format(FORMATTER) + "%04d".formatted(next);
    }
}
