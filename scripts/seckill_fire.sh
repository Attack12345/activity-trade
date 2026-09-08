#!/usr/bin/env bash
# 并发抢购压测（M6 验收）：逐用户 token 并行发 POST /api/seckill/order
# 用法: bash seckill_fire.sh <tokensFile> <outFile> [workers]
set -u
TOKENS="${1:?tokens file}"
OUT="${2:?out file}"
WORKERS="${3:-200}"
: > "$OUT"
cat "$TOKENS" | xargs -P "$WORKERS" -I{} curl -s -m 20 -X POST \
  http://localhost:18080/api/seckill/order \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer {}' \
  -d '{"activityId":"2097231620287918082","skuId":"3001"}' \
  -w '\n' >> "$OUT"
echo "done: $(wc -l < "$OUT") responses"