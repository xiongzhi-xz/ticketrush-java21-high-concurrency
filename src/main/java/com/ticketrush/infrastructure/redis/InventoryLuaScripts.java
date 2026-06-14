package com.ticketrush.infrastructure.redis;

/**
 * 库存 Lua 脚本位置。
 */
public final class InventoryLuaScripts {

    public static final String RESERVE_STOCK = "lua/reserve_stock.lua";
    public static final String UNLOCK_IF_OWNER = "lua/unlock_if_owner.lua";

    private InventoryLuaScripts() {
    }
}
