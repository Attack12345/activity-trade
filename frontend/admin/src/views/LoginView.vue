<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">活动交易 · 管理端</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="onSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { authApi } from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ username: '', password: '' });
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const onSubmit = async () => {
  await formRef.value?.validate();
  loading.value = true;
  try {
    const resp = await authApi.login(form);
    if (resp.role !== 1) {
      ElMessage.error('该账号无管理端权限');
      return;
    }
    auth.setLogin(resp);
    ElMessage.success('登录成功');
    router.push((route.query.redirect as string) || '/activities');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3b8f 0%, #2a5adf 60%, #1677ff 100%);
}
.login-card {
  width: 380px;
  padding: 12px 8px;
}
.login-title {
  text-align: center;
  margin: 0 0 24px;
  color: #1f3b8f;
}
.submit {
  width: 100%;
}
</style>