<template>
  <div class="settle-page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h3 class="title">对账中心</h3>
          <p class="desc">定时任务 + 手动触发，核对订单/支付/库存三方一致性，回滚非法预扣并重试死信。</p>
        </div>
        <el-button type="primary" :loading="running" @click="doRun">立即执行对账</el-button>
      </div>
    </el-card>

    <el-card shadow="never" v-loading="loading">
      <template v-if="report">
        <div class="overview">
          <div class="stat">
            <div class="stat-label">上次执行时间</div>
            <div class="stat-value">{{ report.lastRunAt || '尚未执行' }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">一致性状态</div>
            <el-tag :type="report.ok ? 'success' : 'danger'" size="large" effect="dark">
              {{ report.ok ? '一致 OK' : '存在差异' }}
            </el-tag>
          </div>
          <div class="stat">
            <div class="stat-label">回滚预扣</div>
            <div class="stat-value">{{ report.rolledLogs }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">重发死信</div>
            <div class="stat-value">{{ report.resentDead }}</div>
          </div>
        </div>

        <el-divider />

        <div class="issues">
          <div class="issues-title">问题明细（{{ report.issues.length }}）</div>
          <el-empty v-if="report.issues.length === 0" description="对账无异常" :image-size="72" />
          <el-alert
            v-for="(issue, i) in report.issues"
            :key="i"
            :title="issue"
            type="error"
            :closable="false"
            class="issue-item"
          />
        </div>
      </template>
      <div v-else class="empty">暂无对账结果，请点击「立即执行对账」。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { settleApi } from '../../api';
import type { SettleReport } from '../../types';

const loading = ref(false);
const running = ref(false);
const report = ref<SettleReport | null>(null);

const load = async () => {
  loading.value = true;
  try {
    report.value = await settleApi.report();
  } finally {
    loading.value = false;
  }
};

const doRun = async () => {
  running.value = true;
  try {
    report.value = await settleApi.run();
    ElMessage.success('对账完成');
  } finally {
    running.value = false;
  }
};

load();
</script>

<style scoped>
.card-head {
  flex: 1;
}
</style>