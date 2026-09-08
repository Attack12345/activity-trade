#!/usr/bin/env bash
# 并发抢购压测：逐用户 token 并行发 POST /api/seckill/order
# 用法: bash seckill_fire.sh <tokensFile> <outFile> <activityId> <skuId> [workers]
# 注意：tokens 文件必须为 LF 行尾（否则 \r 混入 Authorization 头导致 400）
set -u
TOKENS="${1:?tokens file}"
OUT="${2:?out file}"
AID="${3:?activityId}"
SKU="${4:?skuId}"
WORKERS="${5:-200}"
: > "$OUT"
cat "$TOKENS" | xargs -P "$WORKERS" -I{} curl -s -m 20 -X POST \
  http://localhost:18080/api/seckill/order \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer {}" \
  -d "{\"activityId\":\"$AID\",\"skuId\":\"$SKU\"}" \
  -w '\n' >> "$OUT"
echo "done: $(wc -l < "$OUT") responses"