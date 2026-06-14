package com.ticketrush.application.command;

/**
 * 库存预热应用命令。
 */
public record PreloadInventoryCommand(
        Long skuId,
        Integer totalStock
) {
}
