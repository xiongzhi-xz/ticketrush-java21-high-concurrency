package com.ticketrush.domain.model;

/**
 * 库存扣减结果。
 */
public record InventoryDeductionResult(
        boolean success,
        Long skuId,
        Integer quantity,
        InventoryDeductionStrategy strategy,
        Integer remainingStock,
        String message
) {

    public static InventoryDeductionResult success(
            Long skuId,
            Integer quantity,
            InventoryDeductionStrategy strategy,
            Integer remainingStock
    ) {
        return new InventoryDeductionResult(true, skuId, quantity, strategy, remainingStock, "库存预占成功");
    }

    public static InventoryDeductionResult failure(
            Long skuId,
            Integer quantity,
            InventoryDeductionStrategy strategy,
            Integer remainingStock,
            String message
    ) {
        return new InventoryDeductionResult(false, skuId, quantity, strategy, remainingStock, message);
    }
}
