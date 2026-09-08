<script setup lang="ts">
import { ref } from 'vue';
import { authApi } from '../api';
import { toast } from '../api/http';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const router = useRouter();
const auth = useAuthStore();

const username = ref('');
const password = ref('');
const nickname = ref('');
const loading = ref(false);

async function submit() {
  if (username.value.length < 4 || password.value.length < 6) {
    toast('用户名至少 4 位、密码至少 6 位');
    return;
  }
  try {
    loading.value = true;
    const resp = await authApi.register({ username: username.value, password: password.value, nickname: nickname.value });
    auth.setLogin(resp);
    toast('注册成功，已自动登录');
    router.push('/');
  } catch (e) {
    // 已 toast
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div style="max-width: 360px; margin: 40px auto">
    <h2 class="title" style="text-align: center">注册</h2>
    <div class="card" style="margin-top: 18px">
      <div class="field">
        <label class="label">用户名</label>
        <input v-model="username" class="input" placeholder="4-32 位" />
      </div>
      <div class="field">
        <label class="label">密码</label>
        <input v-model="password" type="password" class="input" placeholder="6-64 位" />
      </div>
      <div class="field">
        <label class="label">昵称</label>
        <input v-model="nickname" class="input" placeholder="选填" />
      </div>
      <button class="btn" style="width: 100%" :disabled="loading" @click="submit">注册</button>
      <p class="muted" style="text-align: center; margin-top: 12px">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </div>
  </div>
</template>