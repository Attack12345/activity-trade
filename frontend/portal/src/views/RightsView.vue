<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { rightsApi } from '../api';
import type { UserRight } from '../types';

const list = ref<UserRight[]>([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await rightsApi.mine(1, 50);
    list.value = data.list;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="row" style="margin-bottom: 16px">
    <h2 class="title" style="margin: 0">我的权益</h2>
  </div>
  <div v-if="loading" class="empty">加载中…</div>
  <div v-else-if="list.length === 0" class="empty">暂无权益，下单并支付后自动发放</div>
  <div v-else>
    <div v-for="r in list" :key="r.rightNo" class="card">
      <div class="row">
        <div>
          <div style="font-weight: 600">秒杀优惠券</div>
          <div class="muted" style="margin-top: 4px">
            <span class="mono">券号 {{ r.rightNo }}</span>
            <span style="margin-left: 10px">订单 {{ r.orderNo }}</span>
          </div>
        </div>
        <span class="badge badge-paid">{{ r.status === 1 ? '已发放' : '发放失败' }}</span>
      </div>
    </div>
  </div>
</template>