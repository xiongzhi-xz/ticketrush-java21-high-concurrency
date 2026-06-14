-- Redis 分布式锁安全释放脚本
--
-- KEYS[1] 锁 Key
-- ARGV[1] 锁持有者 token
--
-- 返回：
-- 1 释放成功
-- 0 锁不存在或持有者不匹配

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end

return 0
