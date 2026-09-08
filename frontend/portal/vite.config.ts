import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 用户端 portal：dev 默认 5173，/api 代理到后端（可用 VITE_PROXY_TARGET 覆盖，本机 8080 被占用时常指向 18080）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});