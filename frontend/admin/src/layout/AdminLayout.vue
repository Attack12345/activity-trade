<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <span class="brand-title">活动交易</span>
        <span class="brand-sub">管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/activities">
          <el-icon><Grid /></el-icon>
          <span>活动管理</span>
        </el-menu-item>
        <el-menu-item index="/settle">
          <el-icon><Files /></el-icon>
          <span>对账中心</span>
        </el-menu-item>
        <el-menu-item index="/metrics">
          <el-icon><TrendCharts /></el-icon>
          <span>QPS 监控</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">活动秒杀 · 高并发实践</div>
        <div class="header-right">
          <el-tag v-if="auth.role === 1" type="danger" size="small" effect="dark">管理员</el-tag>
          <span class="nickname">{{ auth.nickname || 'admin' }}</span>
          <el-button link type="primary" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { Grid, Files, TrendCharts } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const activeMenu = computed(() => {
  if (route.path.startsWith('/activities')) return '/activities';
  return route.path;
});

const onLogout = async () => {
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' });
  auth.logout();
  router.push('/login');
};
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.aside {
  background: #001529;
  display: flex;
  flex-direction: column;
}
.brand {
  height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 20px;
  color: #fff;
}
.brand-title {
  font-size: 17px;
  font-weight: 600;
}
.brand-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}
.menu {
  border-right: none;
  flex: 1;
}
.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
}
.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #1677ff;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nickname {
  color: #333;
}
.main {
  background: #f5f6fa;
}
</style>