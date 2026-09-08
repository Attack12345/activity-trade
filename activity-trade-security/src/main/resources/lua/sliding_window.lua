-- 滑动窗口限流（原子）
-- KEYS[1] = rl:sw:{userId}:{path}
-- ARGV[1] = nowMs  ARGV[2] = windowMs  ARGV[3] = limit
redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1] - ARGV[2])
local count = redis.call('ZCARD', KEYS[1])
if count >= tonumber(ARGV[3]) then
  return 0
end
local seq = redis.call('INCR', KEYS[1] .. ':seq')
redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. ':' .. seq)
redis.call('PEXPIRE', KEYS[1], ARGV[2])
return 1