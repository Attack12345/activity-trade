<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { orderApi } from '../api';
import PayDialog from '../components/PayDialog.vue';
import type { Order } from '../types';

const list = ref<Order[]>([]);
const total = ref(0);
const loading = ref(true);
const payOrderNo = ref<string | null>(null);
let pollTimer: number | undefined;

const pendingOrders = computed(() => list.value.filter((o) => o.status === 0));

function statusText(o: Order) {
  return o.status === 0 ? { cls: 'badge-wait', text: '待支付' }
    : o.status === 1 ? { cls: 'badge-paid', text: '已支付' }
    : o.status === 2 ? { cls: 'badge-closed', text: '已关闭' }
    : { cls: 'badge-live', text: '已完成' };
}

async function load() {
  const data = await orderApi.mine(1, 50);
  list.value = data.list;
  total.value = data.total;
}

onMounted(async () => {
  try {
    await load();
  } finally {
    loading.value = false;
  }
  // 轮询待支付订单状态（关单/支付兜底）
  pollTimer = window.setInterval(async () => {
    if (pendingOrders.value.length > 0 && !payOrderNo.value) {
      try {
        await load();
      } catch (e) {
        /* ignore */
      }
    }
  }, 3000);
});
onBeforeUnmount(() => window.clearInterval(pollTimer));
</script>

<template>
  <div class="row" style="margin-bottom: 16px">
    <h2 class="title" style="margin: 0">我的订单（{{ total }}）</h2>
  </div>
  <div v-if="loading" class="empty">加载中…</div>
  <div v-else-if="list.length === 0" class="empty">暂无订单，去 <router-link to="/">活动页</router-link> 抢购吧</div>
  <div v-else>
    <div v-for="o in list" :key="o.orderNo" class="card">
      <div class="row">
        <div>
          <div style="font-weight: 600">{{ o.skuTitle }}</div>
          <div class="muted" style="margin-top: 4px">
            <span class="mono">订单号 {{ o.orderNo }}</span>
            <span style="margin-left: 10px">{{ String(o.createdAt).replace('T', ' ') }}</span>
          </div>
        </div>
        <div style="text-align: right">
          <div class="price">¥ {{ o.price }}</div>
          <div style="margin-top: 4px">
            <span :class="['badge', statusText(o).cls]">{{ statusText(o).text }}</span>
          </div>
        </div>
      </div>
      <div v-if="o.status === 0" style="margin-top: 12px; text-align: right">
        <button class="btn" @click="payOrderNo = o.orderNo">去支付</button>
      </div>
    </div>
  </div>

  <PayDialog
    v-if="payOrderNo"
    :order-no="payOrderNo"
    @close="payOrderNo = null"
    @paid="() => { payOrderNo = null; load(); }"
  />
</template>