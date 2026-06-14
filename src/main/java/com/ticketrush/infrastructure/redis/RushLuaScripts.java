package com.ticketrush.infrastructure.redis;

/**
 * 抢票准入 Lua 脚本位置。
 */
public final class RushLuaScripts {

    public static final String ACQUIRE_ADMISSION_TOKEN = "lua/acquire_admission_token.lua";
    public static final String RELEASE_ADMISSION_TOKEN = "lua/release_admission_token.lua";

    private RushLuaScripts() {
    }
}
