import axios, { AxiosError } from 'axios';
import type { ApiResult } from '../types';

// 轻量 toast（全局单例）
let toastEl: HTMLDivElement | null = null;
export function toast(message: string, duration = 2200) {
  if (!toastEl) {
    toastEl = document.createElement('div');
    toastEl.style.cssText =
      'position:fixed;top:20px;left:50%;transform:translateX(-50%);z-index:9999;max-width:80%;padding:10px 18px;border-radius:8px;background:rgba(23,23,23,.92);color:#fff;font-size:14px;box-shadow:0 4px 12px rgba(0,0,0,.2)';
    document.body.appendChild(toastEl);
  }
  toastEl.textContent = message;
  toastEl.style.display = 'block';
  setTimeout(() => {
    if (toastEl) toastEl.style.display = 'none';
  }, duration);
}

const http = axios.create({ baseURL: '/api', timeout: 12000 });

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('at_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 统一解包：code===0 返回 data；其余 toast 并 reject（错误对象携带 code）
http.interceptors.response.use(
  (res) => {
    const body = res.data as ApiResult<unknown>;
    if (body.code !== 0) {
      if (!res.config.headers['X-Silent']) toast(body.message);
      const err = new Error(body.message) as Error & { code?: number };
      err.code = body.code;
      return Promise.reject(err);
    }
    return body.data as never;
  },
  (err: AxiosError<ApiResult<unknown>>) => {
    const message = err.response?.data?.message || '网络异常，请稍后重试';
    if (!err.config?.headers?.['X-Silent']) toast(message);
    const e = new Error(message) as Error & { code?: number };
    e.code = err.response?.data?.code;
    return Promise.reject(e);
  }
);

export default http;