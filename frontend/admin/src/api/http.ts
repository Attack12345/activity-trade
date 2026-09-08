import axios, { AxiosError } from 'axios';
import { ElMessage } from 'element-plus';
import type { ApiResult } from '../types';

const http = axios.create({ baseURL: '/api', timeout: 15000 });

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('at_admin_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (res) => {
    const body = res.data as ApiResult<unknown>;
    if (body.code !== 0) {
      if (!res.config.headers['X-Silent']) ElMessage.error(body.message);
      const err = new Error(body.message) as Error & { code?: number };
      err.code = body.code;
      return Promise.reject(err);
    }
    return body.data as never;
  },
  (err: AxiosError<ApiResult<unknown>>) => {
    const message = err.response?.data?.message || '网络异常，请稍后重试';
    if (!err.config?.headers?.['X-Silent']) ElMessage.error(message);
    const e = new Error(message) as Error & { code?: number };
    e.code = err.response?.data?.code;
    return Promise.reject(e);
  }
);

export default http;