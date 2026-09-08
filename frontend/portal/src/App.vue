<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';

const router = useRouter();
const auth = useAuthStore();

function logout() {
  auth.logout();
  router.push('/');
}
</script>

<template>
  <nav class="nav">
    <router-link to="/" class="nav-brand">活动抢购</router-link>
    <div class="nav-links">
      <router-link to="/" class="nav-link" active-class="active">活动</router-link>
      <router-link v-if="auth.isLogin" to="/orders" class="nav-link" active-class="active">订单</router-link>
      <router-link v-if="auth.isLogin" to="/rights" class="nav-link" active-class="active">权益</router-link>
      <template v-if="auth.isLogin">
        <span class="nav-user">{{ auth.nickname }}</span>
        <button class="btn btn-plain" @click="logout">退出</button>
      </template>
      <router-link v-else to="/login" class="nav-link">登录</router-link>
    </div>
  </nav>
  <main class="container">
    <router-view />
  </main>
</template>