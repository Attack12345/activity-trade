# =====================================================================
# 冒烟（Windows PowerShell 版）：health + MySQL 表存在 + Redis PING
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\smoke.ps1
# 依赖：本机 mysql 客户端、Docker（容器 redis7）
# 本脚本为开发机专用，固定连接信息（MySQL root/1234，redis7/123456）。
# =====================================================================
$ErrorActionPreference = 'Stop'
$base  = 'http://localhost:8080'
$mysql = 'C:\Users\32621\Desktop\develop\mysql\mysql-8.0.42-winx64\bin\mysql.exe'
$redisContainer = 'redis7'
$redisPassword  = '123456'

Write-Output '==> 1. GET /api/health'
(Invoke-WebRequest -Uri ($base + '/api/health') -UseBasicParsing).Content
Write-Output ''

Write-Output '==> 2. MySQL activity_trade 表数量'
$sql = 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=''activity_trade'';'
$tableCount = & $mysql '-h127.0.0.1' '-P3306' '-uroot' '-p1234' '-N' '-e' $sql 2>$null
Write-Output ('tables=' + $tableCount)

Write-Output '==> 3. Redis PING'
docker exec $redisContainer redis-cli -a $redisPassword --no-auth-warning PING

Write-Output '==> smoke OK'