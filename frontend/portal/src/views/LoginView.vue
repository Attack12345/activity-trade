<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { authApi } from '../api';
import { toast } from '../api/http';
import { useAuthStore } from '../stores/auth';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const username = ref('');
const password = ref('');
const loading = ref(false);

async function submit() {
  if (!username.value || !password.value) {
    toast('请输入用户名和密码');
    return;
  }
  try {
    loading.value = true;
    const resp = await authApi.login({ username: username.value, password: password.value });
    auth.setLogin(resp);
    toast('登录成功');
    router.push((route.query.redirect as string) || '/');
  } catch (e) {
    // 已 toast
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div style="max-width: 360px; margin: 40px auto">
    <h2 class="title" style="text-align: center">登录</h2>
    <div class="card" style="margin-top: 18px">
      <div class="field">
        <label class="label">用户名</label>
        <input v-model="username" class="input" placeholder="请输入用户名" />
      </div>
      <div class="field">
        <label class="label">密码</label>
        <input v-model="password" type="password" class="input" placeholder="请输入密码" @keyup.enter="submit" />
      </div>
      <button class="btn" style="width: 100%" :disabled="loading" @click="submit">登录</button>
      <p class="muted" style="text-align: center; margin-top: 12px">
        还没有账号？<router-link to="/register">去注册</router-link>
      </p>
    </div>
  </div>
</template>