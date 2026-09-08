// 与后端 Result/实体对齐的类型
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export interface LoginResp {
  token: string;
  userId: string;
  nickname: string;
  role: number;
}

export interface Activity {
  id: string;
  name: string;
  status: number; // 0草稿 1预热 2进行中 3结束
  startTime: string;
  endTime: string;
}

export interface SkuVO {
  skuId: string;
  title: string;
  image: string;
  seckillPrice: number;
  originalPrice: number;
  stockLeft: number;
  stockStatus: number; // 1可抢 0售罄
}

export interface ActivityDetail {
  id: string;
  name: string;
  status: number;
  startTime: string;
  endTime: string;
  skus: SkuVO[];
}

export interface SeckillResp {
  orderNo: string;
  status: number;
}

export interface PayMockResp {
  payNo: string;
  amount: number;
  sign: string;
}

export interface Order {
  id: string;
  orderNo: string;
  skuTitle: string;
  price: number;
  status: number; // 0待支付 1已支付 2已关闭 3已完成
  activityId: string;
  skuId: string;
  payTime: string | null;
  closedTime: string | null;
  createdAt: string;
}

export interface UserRight {
  rightNo: string;
  orderNo: string;
  rightType: number;
  status: number;
  createdAt: string;
}