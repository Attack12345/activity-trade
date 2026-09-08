export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface LoginResp {
  token: string;
  userId: number;
  nickname: string;
  role: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export interface Activity {
  id: string;
  name: string;
  status: number; // 0草稿 1预热 2进行中 3结束
  startTime: string;
  endTime: string;
  createdAt: string;
}

export interface StockSnapshot {
  activityId: string;
  items: { skuId: number; dbStock: number; redisStock: number }[];
  bloomAllReady: boolean;
}

export interface SettleReport {
  lastRunAt: string | null;
  ok: boolean;
  issues: string[];
  rolledLogs: number;
  resentDead: number;
}

export interface SkuUpsert {
  skuId: number;
  seckillPrice: number;
  originalPrice: number;
  seckillStock: number;
}

export interface PreheatResult {
  activityId: string;
  bloomAllKey: string;
  bloomActKey: string;
  stockKeys: string[];
  status: number;
}

export interface QpsSample {
  timestamp: number;
  count: number;
}