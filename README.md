# activity-trade 营销活动交易系统（高并发秒杀）

面向面试展示的**高并发 Java 后端**实践项目：Redis 缓存三兄弟 + Lua 原子预扣、RocketMQ 事务/延迟消息、幂等与限流、对账兜底，配套用户端与管理端两个 Vue3 前端，端到端可演示。

## 秒起（本地一键跑通）

要求：JDK 23、Maven 3.9+、Node 18+、Docker、本机 MySQL 8（root/1234）或等价的中间件。

1. **中间件**：MySQL 建库 `mysql -uroot -p1234 < sql/schema.sql`；Redis 与 RocketMQ 可用既有实例（见 `docker/docker-compose.yml` 顶部说明）。
2. **后端**：`mvn -q clean install -DskipTests && mvn spring-boot:run -pl activity-trade-bootstrap`，默认 `8080`；端口占用时用 `-Dspring-boot.run.arguments=--server.port=18080` 并用环境变量 `VITE_PROXY_TARGET` 指向前端代理。
3. **前端**：
   - 用户端 `cd frontend/portal && npm i && npm run dev`（5173）。
   - 管理端 `cd frontend/admin && npm i && npm run dev`（5174）。
4. **端到端验证**（可选）：`BASE_URL=http://localhost:8080 MYSQL=mysql ./scripts/e2e.sh` —— 清库→建活动→预热→并发抢→支付→对账断言，全绿即通。

> 管理端账号：注册后 `UPDATE user SET role=1 WHERE username='xx'` 即可获得管理权限；管理端主要演示 活动管理/预热/库存视图/对账/QPS 监控。

## 架构

```
                 ┌──────────────┐   ┌──────────────┐
                 │ portal(用户) │   │ admin(管理)  │   Vue3 + Vite
                 └──────┬───────┘   └──────┬───────┘
                        └──────── /api ────┘
                 ┌──────────────────────────────┐
                 │   Nginx（可选：静态 + limit_req） │
                 └──────────────┬───────────────┘
┌────────────────── Spring Boot（模块化单体，10 个 Maven 模块）───────────────┐
│  接入层: 认证拦截(JWT) · 幂等切面 · 滑动窗口限流 · QPS 观测                    │
│  业务层: activity(活动/缓存) · stock(预减) · order(抢购/关单) · pay · rights │
│  中间件: Redis(Lua 预减/布隆/分布锁/Caffeine 二级)  RocketMQ(事务/延迟消息)    │
│  兜底层: settle(对账/回滚/死信重发) → MySQL(乐观锁兜底+幂等表)                │
└─────────────────────────────────────────────────────────────────────────────┘
```

技术选型：**JDK 23 · Spring Boot 3.4.7 · MyBatis-Plus 3.5.9 · Redis 7 · RocketMQ 5 · MySQL 8 · Vue3 + Element Plus + ECharts**。

## 高并发亮点（面试主线：三层防线）

1. **入口限流**：应用自研滑动窗口限流（Redis Lua 原子计数），单用户 20 QPS，配合 Nginx `limit_req` 双保险。
2. **缓存三兄弟（穿透/击穿/雪崩）**：
   - 穿透：全局布隆过滤器（Redisson RBloomFilter）拦截不存在商品 + 空值短 TTL 缓存；
   - 击穿：互斥锁（Redisson）单飞重建详情缓存，拿锁失败短暂轮询后兜底 DB；
   - 雪崩：Redis TTL ±10% 随机抖动 + Caffeine 本地二级缓存 + 预热热 key。
3. **Redis Lua 原子预减 + 售罄回滚占位**：`DECR` 预库存分段（`stock:act:sku:seg`），负数即回滚，绝对值作为售罄令牌。
4. **DB 乐观锁兜底防超卖**：`UPDATE activity_sku SET seckill_stock=seckill_stock-1 WHERE id=? AND seckill_stock>0`，CAS 失败视为售罄。
5. **RocketMQ 事务消息**：半消息 + 本地事务（建单 + DB 扣库存）→ 提交；回查接口兜底；消费端幂等（DB 唯一约束 + Redis SETNX）。
6. **最终一致兜底**：定时对账（5 分钟）扫描 trade_log 差异 ⇒ 回滚预扣/占位、重发死信；关单延迟消息（30 分钟超时自动取消并回补库存）。
7. **幂等设计**：注册/支付回调等入口 `@Idempotent`（Redis SETNX + 30s TTL），抢购重复参与由 DB 唯一索引拦截（错误码 2005）。

## 压测结论

> 详见 `docs/pressure-reports/M6.md` 与 `M12.md`（M12 为 10w 级全量爬坡）。

- 200 独立用户抢 100 库存：成功 100、售罄拒绝 1，**零超卖**（DB 断言通过）。
- 单用户滑动窗口：20 请求放行 / 20 限流，与配置严格一致。
- 多档并发爬坡（50→100→200→400→600）：入口吞吐随并发线性爬坡，P99 稳定（详见 M12 报告）。

## 目录速览

```text
activity-trade-bootstrap    启动入口 + Job
activity-trade-{common,infra,security}   公共/基础设施/认证限流
activity-trade-{activity,stock,order,pay,rights}   业务域（活动/库存/订单/支付/权益）
activity-trade-settle       对账补偿 + QPS 观测
frontend/{portal,admin}     用户端 / 管理端
scripts/                    e2e.sh（端到端演示）、压测脚本
deploy/nginx.conf           Nginx 部署示例（双前端 + /api 反代 + limit_req）
```

## License

Apache License 2.0 示例项目，仅供学习交流。