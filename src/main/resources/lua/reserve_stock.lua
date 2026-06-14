-- Redis Lua 库存预占脚本
--
-- KEYS[1] 库存 Hash Key，例如 ticketrush:inventory:{skuId}
-- KEYS[2] 幂等 Key，例如 ticketrush:idempotent:{idempotentKey}
--
-- ARGV[1] 扣减数量
-- ARGV[2] 幂等 Key TTL 秒
--
-- 返回：
--  1  库存预占成功
--  0  库存不足
-- -1  重复请求
-- -2  库存 Key 不存在

local quantity = tonumber(ARGV[1])
local idempotent_ttl = tonumber(ARGV[2])

if redis.call('exists', KEYS[2]) == 1 then
    return -1
end

if redis.call('exists', KEYS[1]) == 0 then
    return -2
end

local available = tonumber(redis.call('hget', KEYS[1], 'available') or '0')
if available < quantity then
    return 0
end

redis.call('hincrby', KEYS[1], 'available', -quantity)
redis.call('hincrby', KEYS[1], 'locked', quantity)
redis.call('hincrby', KEYS[1], 'version', 1)
redis.call('set', KEYS[2], '1', 'EX', idempotent_ttl)

return 1
