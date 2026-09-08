-- =====================================================================
-- activity_trade 完整建表脚本（幂等：按依赖顺序 DROP 后重建）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS activity_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE activity_trade;

DROP TABLE IF EXISTS trade_log;
DROP TABLE IF EXISTS user_right;
DROP TABLE IF EXISTS stock_deduct_log;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS activity_sku;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id BIGINT NOT NULL,
  username VARCHAR(32) NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  nickname VARCHAR(32) NOT NULL DEFAULT '',
  role TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通用户 1=admin',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_username (username)
) COMMENT='用户';

CREATE TABLE activity (
  id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1预热 2进行中 3结束',
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status)
) COMMENT='活动';

CREATE TABLE product (
  id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  img_url VARCHAR(255) NOT NULL DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) COMMENT='商品';

CREATE TABLE activity_sku (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  seckill_price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  seckill_stock INT NOT NULL COMMENT '实际库存(DB 为准)',
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_act_sku (activity_id, sku_id)
) COMMENT='活动商品';

CREATE TABLE orders (
  id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  sku_title VARCHAR(128) NOT NULL DEFAULT '',
  price DECIMAL(10,2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已关闭 3已完成',
  pay_time DATETIME NULL,
  closed_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_order_no (order_no),
  UNIQUE KEY uq_user_act_sku (user_id, activity_id, sku_id),
  KEY idx_user (user_id),
  KEY idx_status_created (status, created_at)
) COMMENT='订单';

CREATE TABLE order_item (
  id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  sku_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL DEFAULT '',
  price DECIMAL(10,2) NOT NULL,
  qty INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_order (order_no)
) COMMENT='订单明细';

CREATE TABLE payment (
  id BIGINT NOT NULL,
  pay_no VARCHAR(32) NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付',
  callback_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_pay_no (pay_no),
  KEY idx_order (order_no)
) COMMENT='支付';

CREATE TABLE stock_deduct_log (
  id BIGINT NOT NULL,
  biz_no VARCHAR(32) NOT NULL,
  activity_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  delta INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0占用 1确认 2回滚',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_biz (biz_no)
) COMMENT='库存扣减日志(对账)';

CREATE TABLE user_right (
  id BIGINT NOT NULL,
  right_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  right_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=优惠券',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发放 1已发放 2发放失败',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_right_no (right_no),
  KEY idx_user (user_id)
) COMMENT='权益';

CREATE TABLE trade_log (
  id BIGINT NOT NULL,
  biz_no VARCHAR(32) NOT NULL,
  topic VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0成功 1待补偿(死信/未达终态) 2已补偿',
  retry INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_biz (biz_no)
) COMMENT='业务消息/补偿日志';