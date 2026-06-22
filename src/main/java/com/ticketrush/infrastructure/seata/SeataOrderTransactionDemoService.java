package com.ticketrush.infrastructure.seata;

import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import com.ticketrush.common.id.OrderNoGenerator;
import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.TicketOrderRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketInventoryMapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Seata AT 模式示例服务。
 *
 * <p>主抢票链路优先使用 Redis/RocketMQ 的最终一致性方案。该服务用于演示
 * MySQL 库存预占和订单落库如何放在一个 Seata 全局事务中，用于说明
 * 强一致示例和最终一致性主链路的取舍。</p>
 */
@Service
public class SeataOrderTransactionDemoService {

    private static final String GLOBAL_TX_NAME = "ticketrush-seata-mysql-rush-order";

    private final TicketInventoryMapper ticketInventoryMapper;
    private final TicketOrderRepository ticketOrderRepository;
    private final OrderNoGenerator orderNoGenerator;
    private final Duration orderExpireTtl;
    private final Clock clock;

    @Autowired
    public SeataOrderTransactionDemoService(
            TicketInventoryMapper ticketInventoryMapper,
            TicketOrderRepository ticketOrderRepository,
            OrderNoGenerator orderNoGenerator,
            @Value("${ticketrush.order.expire-ttl:15m}") Duration orderExpireTtl
    ) {
        this(
                ticketInventoryMapper,
                ticketOrderRepository,
                orderNoGenerator,
                orderExpireTtl,
                Clock.systemDefaultZone()
        );
    }

    SeataOrderTransactionDemoService(
            TicketInventoryMapper ticketInventoryMapper,
            TicketOrderRepository ticketOrderRepository,
            OrderNoGenerator orderNoGenerator,
            Duration orderExpireTtl,
            Clock clock
    ) {
        this.ticketInventoryMapper = ticketInventoryMapper;
        this.ticketOrderRepository = ticketOrderRepository;
        this.orderNoGenerator = orderNoGenerator;
        this.orderExpireTtl = orderExpireTtl;
        this.clock = clock;
    }

    @GlobalTransactional(name = GLOBAL_TX_NAME, rollbackFor = Exception.class)
    public TicketOrder reserveMysqlInventoryAndCreatePendingOrder(InventoryDeductionCommand command) {
        if (command.strategy() != InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK) {
            throw new BusinessException(
                    ErrorCode.STOCK_DEDUCT_FAILED,
                    "Seata AT 示例只演示 MySQL 乐观锁库存策略"
            );
        }

        return ticketOrderRepository.findByIdempotentKey(command.idempotentKey())
                .orElseGet(() -> doReserveMysqlInventoryAndCreatePendingOrder(command));
    }

    private TicketOrder doReserveMysqlInventoryAndCreatePendingOrder(InventoryDeductionCommand command) {
        TicketInventory inventory = ticketInventoryMapper.findBySkuId(command.skuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_DEDUCT_FAILED, "库存不存在或未预热"));
        if (!inventory.hasEnoughAvailable(command.quantity())) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "可售库存不足");
        }

        int updatedRows = ticketInventoryMapper.reserveByOptimisticLock(
                command.skuId(),
                command.quantity(),
                inventory.version()
        );
        if (updatedRows != 1) {
            throw new BusinessException(ErrorCode.STOCK_DEDUCT_FAILED, "乐观锁版本冲突");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        TicketOrder order = new TicketOrder(
                null,
                orderNoGenerator.nextOrderNo(),
                command.userId(),
                command.eventId(),
                command.skuId(),
                command.quantity(),
                0L,
                command.strategy(),
                OrderStatus.PENDING,
                command.idempotentKey(),
                now,
                now.plus(orderExpireTtl),
                null,
                null
        );
        return ticketOrderRepository.save(order);
    }
}
