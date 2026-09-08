#!/usr/bin/env bash
# =====================================================================
# M12 端到端演示脚本：清库 → 建活动 → 预热 → 并发抢购 → 支付 → 对账断言
# 用法：
#   BASE_URL=http://localhost:8080 MYSQL=mysql ./scripts/e2e.sh
# 依赖：curl jq mysql（服务端已启动，MySQL/Redis/RocketMQ 就绪）
# 退出码：0=全绿，1=任一断言失败
# =====================================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MYSQL="${MYSQL:-mysql}"
MYSQL_ARGS="${MYSQL_ARGS:--h127.0.0.1 -P3306 -uroot -p1234}"
DB="${DB:-activity_trade}"
API="$BASE_URL/api"

RED='\033[31m'; GREEN='\033[32m'; NC='\033[0m'
PASS=0; FAIL=0
pass() { PASS=$((PASS+1)); echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { FAIL=$((FAIL+1)); echo -e "${RED}[FAIL]${NC} $1"; }

json() { jq -r "$1" 2>/dev/null; }

echo "== M12 e2e @ $BASE_URL =="

# ---------- 0. 清库 ----------
echo "--- 清库（业务表） ---"
$MYSQL $MYSQL_ARGS -e "
DELETE FROM $DB.stock_deduct_log;
DELETE FROM $DB.order_item;
DELETE FROM $DB.payment;
DELETE FROM $DB.user_right;
DELETE FROM $DB.orders;
DELETE FROM $DB.activity_sku;
DELETE FROM $DB.activity;
DELETE FROM $DB.product;
DELETE FROM $DB.trade_log;
DELETE FROM $DB.user;" || { fail "清库失败"; exit 1; }
pass "清库"

# ---------- 1. 管理员 ----------
echo "--- 管理员注册/提权/登录 ---"
$MYSQL $MYSQL_ARGS -e "DELETE FROM $DB.user WHERE username='m12admin';" 2>/dev/null
curl -s -X POST "$API/auth/register" -H 'Content-Type: application/json' \
  -d '{"username":"m12admin","password":"admin123456","nickname":"m12admin"}' >/dev/null
$MYSQL $MYSQL_ARGS -e "UPDATE $DB.user SET role=1 WHERE username='m12admin';"
TOKEN=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"m12admin","password":"admin123456"}' | json '.data.token')
[ -n "$TOKEN" ] && pass "管理员登录" || { fail "管理员登录"; exit 1; }
AUTH="Authorization: Bearer $TOKEN"

# ---------- 2. 建活动 + SKU ----------
echo "--- 建活动 + SKU ---"
NOW=$(date '+%Y-%m-%d %H:%M:%S')
END=$(date -v+2H '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -d '+2 hours' '+%Y-%m-%d %H:%M:%S')
ACT_ID=$(curl -s -X POST "$API/admin/activities" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"name\":\"M12 e2e seckill\",\"startTime\":\"$NOW\",\"endTime\":\"$END\"}" | json '.data.id')
[ -n "$ACT_ID" ] && [ "$ACT_ID" != "null" ] && pass "创建活动 id=$ACT_ID" || { fail "创建活动"; exit 1; }

curl -s -X POST "$API/admin/activities/$ACT_ID/skus" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '[{"skuId":1001,"seckillPrice":0.01,"originalPrice":99.9,"seckillStock":100},
       {"skuId":1002,"seckillPrice":0.02,"originalPrice":199,"seckillStock":100}]' >/dev/null
pass "配置 SKU"

# ---------- 3. 预热 ----------
echo "--- 预热 ---"
curl -s -X POST "$API/admin/activities/$ACT_ID/status" -H "$AUTH" -H 'Content-Type: application/json' -d '{"status":1}' >/dev/null
PRE=$(curl -s -X POST "$API/admin/activities/$ACT_ID/preheat" -H "$AUTH" -H 'Content-Type: application/json' -d '{}')
[ "$(echo "$PRE" | json '.code')" = "0" ] && pass "预热" || { fail "预热"; exit 1; }
curl -s -X POST "$API/admin/activities/$ACT_ID/status" -H "$AUTH" -H 'Content-Type: application/json' -d '{"status":2}' >/dev/null
pass "开抢（status=2）"

# ---------- 4. 并发抢购 ----------
echo "--- 20 用户并发抢购 sku=1001（库存 100） ---"
REG_LIST=""; TOKENS=""
for i in $(seq 1 20); do
  U="m12u$i"
  curl -s -X POST "$API/auth/register" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$U\",\"password\":\"user123456\",\"nickname\":\"U$i\"}" >/dev/null
  TK=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$U\",\"password\":\"user123456\"}" | json '.data.token')
  TOKENS="$TOKENS $TK"
done

HIT_DIR=$(mktemp -d)
IDX=0
for TK in $TOKENS; do
  IDX=$((IDX+1))
  ( curl -s -X POST "$API/seckill/order" -H "Authorization: Bearer $TK" -H 'Content-Type: application/json' \
      -d "{\"activityId\":\"$ACT_ID\",\"skuId\":1001}" \
      | jq -r 'if .code==0 then .data.orderNo else "FAIL_\(.code)" end' ) > "$HIT_DIR/$IDX" &
done
wait
HIT_FILE=$(mktemp)
cat "$HIT_DIR"/* > "$HIT_FILE"
OK_COUNT=$(grep -vc '^FAIL_' "$HIT_FILE")
[ "$OK_COUNT" -eq 20 ] && pass "抢购成功数=20（实际 $OK_COUNT）" || fail "抢购成功数=20（实际 $OK_COUNT）"
ORDER_NOS=$(grep -v '^FAIL_' "$HIT_FILE")

# ---------- 5. 支付（mock -> callback） ----------
echo "--- 支付已完成订单 ---"
PAY_OK=0
for order_no in $ORDER_NOS; do
  MOCK=$(curl -s -X POST "$API/pay/mock" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "{\"orderNo\":\"$order_no\"}")
  PAY_NO=$(echo "$MOCK" | json '.data.payNo')
  AMOUNT=$(echo "$MOCK" | json '.data.amount')
  SIGN=$(echo "$MOCK" | json '.data.sign')
  CB=$(curl -s -X POST "$API/pay/callback" -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"payNo\":\"$PAY_NO\",\"orderNo\":\"$order_no\",\"amount\":$AMOUNT,\"sign\":\"$SIGN\"}")
  [ "$(echo "$CB" | json '.code')" = "0" ] && PAY_OK=$((PAY_OK+1))
done
[ "$PAY_OK" -eq "$OK_COUNT" ] && pass "支付成功 $PAY_OK/$OK_COUNT" || fail "支付成功 $PAY_OK/$OK_COUNT"

# ---------- 6. 对账断言 ----------
echo "--- 对账与超卖断言 ---"
SETTLE=$(curl -s -X POST "$API/settle/admin/run" -H "$AUTH" -H 'Content-Type: application/json' -d '{}')
[ "$(echo "$SETTLE" | json '.data.ok')" = "true" ] && pass "对账一致 ok=true" || { fail "对账不一致"; echo "$SETTLE" | json '.data.issues[]'; }

NEG=$($MYSQL $MYSQL_ARGS -N -e "SELECT COUNT(*) FROM $DB.activity_sku WHERE seckill_stock < 0;")
UNPAID_GAP=$($MYSQL $MYSQL_ARGS -N -e "SELECT IFNULL(COUNT(*)-SUM(status IN (0,1)),0) FROM $DB.stock_deduct_log;")
echo "  neg_stock=$NEG, deduct_gap=$UNPAID_GAP"
[ "$NEG" = "0" ] && [ "$UNPAID_GAP" = "0" ] && pass "零超卖（负库存=0，占用数=订单数）" || fail "超卖检查异常"

echo ""
echo "== 结果：PASS=$PASS FAIL=$FAIL =="
[ "$FAIL" -eq 0 ] && echo -e "${GREEN}E2E ALL GREEN${NC}" || echo -e "${RED}E2E FAILED${NC}"
exit $((FAIL > 0 ? 1 : 0))