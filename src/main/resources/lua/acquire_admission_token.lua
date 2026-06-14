-- 抢票请求准入令牌脚本
--
-- KEYS[1] 票档准入 Key，例如 ticketrush:rush:admission:{skuId}
-- ARGV[1] 最大并发进入数
-- ARGV[2] 令牌 TTL 秒
--
-- 返回：
-- 1 准入成功
-- 0 准入失败，当前票档请求过多

local current = tonumber(redis.call('get', KEYS[1]) or '0')
local max_in_flight = tonumber(ARGV[1])
local ttl_seconds = tonumber(ARGV[2])

if current >= max_in_flight then
    return 0
end

local next_count = redis.call('incr', KEYS[1])
redis.call('expire', KEYS[1], ttl_seconds)

if next_count > max_in_flight then
    redis.call('decr', KEYS[1])
    return 0
end

return 1
