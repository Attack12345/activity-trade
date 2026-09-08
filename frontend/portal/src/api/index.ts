import http from './http';
import type { Activity, ActivityDetail, LoginResp, Order, PageResult, PayMockResp, SeckillResp, UserRight } from '../types';

const SILENT = { headers: { 'X-Silent': '1' } };

export const authApi = {
  register: (data: { username: string; password: string; nickname?: string }) =>
    http.post<never, LoginResp>('/auth/register', data),
  login: (data: { username: string; password: string }) =>
    http.post<never, LoginResp>('/auth/login', data)
};

export const activityApi = {
  list: (page = 1, size = 10) => http.get<never, PageResult<Activity>>('/activities', { params: { page, size } }),
  detail: (id: string) => http.get<never, ActivityDetail>(`/activities/${id}`)
};

export const seckillApi = {
  // 抢购错误码需页面自行处理（2004/2005/2006），静默不弹 toast
  order: (data: { activityId: string; skuId: string }) =>
    http.post<never, SeckillResp>('/seckill/order', data, SILENT)
};

export const orderApi = {
  mine: (page = 1, size = 10) => http.get<never, PageResult<Order>>('/orders/mine', { params: { page, size } }),
  result: (orderNo: string) => http.get<never, Order>(`/orders/result/${orderNo}`)
};

export const payApi = {
  mock: (orderNo: string) => http.post<never, PayMockResp>('/pay/mock', { orderNo }),
  callback: (data: { payNo: string; orderNo: string; amount: number; sign: string }) =>
    http.post<never, { payNo: string; status: number }>('/pay/callback', data)
};

export const rightsApi = {
  mine: (page = 1, size = 10) => http.get<never, PageResult<UserRight>>('/rights/mine', { params: { page, size } })
};