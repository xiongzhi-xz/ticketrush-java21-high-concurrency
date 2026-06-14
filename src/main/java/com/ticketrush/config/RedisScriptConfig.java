package com.ticketrush.config;

import com.ticketrush.infrastructure.redis.InventoryLuaScripts;
import com.ticketrush.infrastructure.redis.RushLuaScripts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis Lua 脚本配置。
 */
@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<Long> reserveStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(InventoryLuaScripts.RESERVE_STOCK));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> unlockInventoryLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(InventoryLuaScripts.UNLOCK_IF_OWNER));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> acquireAdmissionTokenScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(RushLuaScripts.ACQUIRE_ADMISSION_TOKEN));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> releaseAdmissionTokenScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(RushLuaScripts.RELEASE_ADMISSION_TOKEN));
        script.setResultType(Long.class);
        return script;
    }
}
