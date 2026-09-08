-- 抢购预减库存（原子）
-- KEYS[1] = 预库存 key  (stock:{act}:{sku}:{seg})
-- KEYS[2] = 用户占位 key(buyer:{act}:{sku}:{userId})
-- ARGV[1] = 占位 TTL 秒
-- 返回: 1=成功; -1=售罄(已回补,无副作用); -2=已参与
if redis.call('SET', KEYS[2], '1', 'NX', 'EX', ARGV[1]) == false then
  return -2
end
local stock = redis.call('DECR', KEYS[1])
if stock < 0 then
  redis.call('INCR', KEYS[1])
  redis.call('DEL', KEYS[2])
  return -1
end
return 1