-- 抢票请求准入令牌释放脚本
--
-- KEYS[1] 票档准入 Key
--
-- 返回：
-- 1 释放成功
-- 0 无需释放

local current = tonumber(redis.call('get', KEYS[1]) or '0')

if current <= 0 then
    return 0
end

local next_count = redis.call('decr', KEYS[1])
if next_count <= 0 then
    redis.call('del', KEYS[1])
end

return 1
