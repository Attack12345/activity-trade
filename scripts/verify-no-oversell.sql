-- =====================================================================
-- 零超卖断言（M6~M9 回归项）
-- 用法：mysql -uroot -p ... activity_trade < scripts/verify-no-oversell.sql
-- 输出为空 = 通过。任一不变量破坏都会产生行：
--   1) 库存不得为负
--   2) 库存扣减日志「占用/确认」数 == 进行中(未关闭)订单数
-- =====================================================================
SELECT 'NEGATIVE_STOCK' AS check_name, s.activity_id, s.sku_id, s.seckill_stock
FROM activity_sku s
WHERE s.seckill_stock < 0

UNION ALL

SELECT 'OCCUPY_MISMATCH' AS check_name, s.activity_id, s.sku_id,
       COALESCE(o.cnt, 0) - COALESCE(d.cnt, 0) AS diff
FROM activity_sku s
LEFT JOIN (
    SELECT activity_id, sku_id, COUNT(*) AS cnt
    FROM orders WHERE status IN (0, 1)
    GROUP BY activity_id, sku_id
) o ON o.activity_id = s.activity_id AND o.sku_id = s.sku_id
LEFT JOIN (
    SELECT activity_id, sku_id, COUNT(*) AS cnt
    FROM stock_deduct_log WHERE status IN (0, 1)
    GROUP BY activity_id, sku_id
) d ON d.activity_id = s.activity_id AND d.sku_id = s.sku_id
WHERE COALESCE(o.cnt, 0) <> COALESCE(d.cnt, 0);