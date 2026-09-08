#!/usr/bin/env bash
# =====================================================================
# 冒烟：health + MySQL 表存在 + Redis PING
# 用法：BASE=http://localhost:8080 bash scripts/smoke.sh
# 依赖：curl、mysql 客户端；redis 通过 docker exec 现有容器 redis7 验证
# =====================================================================
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-1234}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis7}"
REDIS_PASSWORD="${REDIS_PASSWORD:-123456}"

echo "==> 1. GET /api/health"
curl -fsS "${BASE}/api/health"
echo

echo "==> 2. MySQL activity_trade 表数量"
"${MYSQL_BIN}" "-h${MYSQL_HOST}" "-P${MYSQL_PORT}" "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" -N -e \
  "SELECT CONCAT('tables=', COUNT(*)) FROM information_schema.tables WHERE table_schema='activity_trade';"

echo "==> 3. Redis PING"
docker exec "${REDIS_CONTAINER}" redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning PING

echo "==> smoke OK"