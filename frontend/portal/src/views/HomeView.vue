<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { activityApi } from '../api';
import CountDown from '../components/CountDown.vue';
import type { Activity } from '../types';

const list = ref<Activity[]>([]);
const loading = ref(true);

function statusBadge(a: Activity) {
  if (a.status === 1) return { cls: 'badge-pre', text: '即将开始' };
  if (a.status === 2) return { cls: 'badge-live', text: '进行中' };
  return { cls: 'badge-end', text: '已结束' };
}

onMounted(async () => {
  try {
    const data = await activityApi.list(1, 50);
    list.value = data.list;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="row" style="margin-bottom: 16px">
    <h2 class="title" style="margin: 0">活动列表</h2>
  </div>
  <div v-if="loading" class="empty">加载中…</div>
  <div v-else-if="list.length === 0" class="empty">暂无进行中的活动</div>
  <div v-else class="grid">
    <router-link
      v-for="a in list"
      :key="a.id"
      :to="`/activity/${a.id}`"
      class="card item"
      style="text-decoration: none; color: inherit"
    >
      <div class="item-img">{{ a.name }}</div>
      <div class="row">
        <div style="font-weight: 600">{{ a.name }}</div>
        <span :class="['badge', statusBadge(a).cls]">{{ statusBadge(a).text }}</span>
      </div>
      <div style="margin-top: 8px">
        <CountDown v-if="a.status === 1 || a.status === 2" :start-time="a.startTime" :end-time="a.endTime" />
        <span v-else class="muted">活动已结束</span>
      </div>
    </router-link>
  </div>
</template>