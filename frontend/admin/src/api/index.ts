import http from './http';
import type {
  Activity,
  LoginResp,
  PageResult,
  PreheatResult,
  QpsSample,
  SettleReport,
  SkuUpsert,
  StockSnapshot
} from '../types';

export const authApi = {
  login: (data: { username: string; password: string }) => http.post<never, LoginResp>('/auth/login', data)
};

export const adminActivityApi = {
  list: (page = 1, size = 10, status?: number) =>
    http.get<never, PageResult<Activity>>('/admin/activities', { params: { page, size, status } }),
  create: (data: { name: string; startTime: string; endTime: string }) =>
    http.post<never, Activity>('/admin/activities', data),
  update: (id: string, data: { name?: string; startTime?: string; endTime?: string }) =>
    http.put<never, Activity>(`/admin/activities/${id}`, data),
  changeStatus: (id: string, status: number) =>
    http.post<never, Activity>(`/admin/activities/${id}/status`, { status }),
  saveSkus: (id: string, skus: SkuUpsert[]) =>
    http.post<never, number>(`/admin/activities/${id}/skus`, skus),
  preheat: (id: string) => http.post<never, PreheatResult>(`/admin/activities/${id}/preheat`, {}),
  stock: (id: string) => http.get<never, StockSnapshot>(`/admin/activities/${id}/stock`)
};

export const settleApi = {
  report: () => http.get<never, SettleReport>('/settle/report'),
  run: () => http.post<never, SettleReport>('/settle/admin/run', {})
};

export const metricsApi = {
  qps: (minutes = 10) => http.get<never, QpsSample[]>(`/metrics/qps?minutes=${minutes}`)
};