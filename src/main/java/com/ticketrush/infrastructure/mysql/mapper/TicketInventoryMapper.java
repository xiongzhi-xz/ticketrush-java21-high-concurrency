package com.ticketrush.infrastructure.mysql.mapper;

import com.ticketrush.domain.model.TicketInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 票档库存 MyBatis Mapper。
 *
 * <p>`reserveByOptimisticLock` 是 MySQL 乐观锁扣减入口：
 * SQL 必须同时校验 sku_id、version 和 available_stock，受影响行数为 1 才代表扣减成功。</p>
 */
@Mapper
public interface TicketInventoryMapper {

    Optional<TicketInventory> findBySkuId(@Param("skuId") Long skuId);

    int insert(TicketInventory inventory);

    int reserveByOptimisticLock(
            @Param("skuId") Long skuId,
            @Param("quantity") int quantity,
            @Param("version") long version
    );

    int confirm(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    int release(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
