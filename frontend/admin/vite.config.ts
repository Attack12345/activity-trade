import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

// 管理端 admin：dev 默认 5174，/api 代理到后端（VITE_PROXY_TARGET 可覆盖）
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  return {
    plugins: [vue()],
    server: {
      port: 5174,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  };
});