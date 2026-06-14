package com.ticketrush.job;

import com.ticketrush.application.dto.CloseExpiredOrdersResult;
import com.ticketrush.application.service.OrderTimeoutCloseApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时关闭任务。
 */
@Component
@ConditionalOnProperty(
        prefix = "ticketrush.order.timeout-close",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderTimeoutCloseJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutCloseJob.class);

    private final OrderTimeoutCloseApplicationService orderTimeoutCloseApplicationService;
    private final int batchSize;

    public OrderTimeoutCloseJob(
            OrderTimeoutCloseApplicationService orderTimeoutCloseApplicationService,
            @Value("${ticketrush.order.timeout-close.batch-size:100}") int batchSize
    ) {
        this.orderTimeoutCloseApplicationService = orderTimeoutCloseApplicationService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketrush.order.timeout-close.fixed-delay-ms:30000}")
    public void closeExpiredOrders() {
        CloseExpiredOrdersResult result = orderTimeoutCloseApplicationService.closeExpiredOrders(batchSize);
        if (result.scannedCount() > 0 || result.failedCount() > 0) {
            log.info(
                    "订单超时关闭任务完成 scanned={}, closed={}, released={}, skipped={}, failed={}",
                    result.scannedCount(),
                    result.closedCount(),
                    result.releasedStockCount(),
                    result.skippedCount(),
                    result.failedCount()
            );
        }
    }
}
