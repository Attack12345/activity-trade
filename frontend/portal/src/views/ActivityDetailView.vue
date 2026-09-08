<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { activityApi, orderApi, seckillApi } from '../api';
import { toast } from '../api/http';
import { useAuthStore } from '../stores/auth';
import CountDown from '../components/CountDown.vue';
import PayDialog from '../components/PayDialog.vue';
import type { ActivityDetail, SkuVO } from '../types';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const detail = ref<ActivityDetail | null>(null);
const loading = ref(true);
const buying = ref(false);
const payingOrderNo = ref<string | null>(null);
// 抢购后的结果轮询（#14）
const polling = ref(false);
let pollTimer: number | undefined;

const activityId = computed(() => (route.params.id as string) ?? '');
const live = computed(() => detail.value?.status === 2);

async function load() {
  try {
    detail.value = await activityApi.detail(activityId.value);
  } catch (e) {
    toast('活动不存在或已下线');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
onBeforeUnmount(() => window.clearInterval(pollTimer));

function canBuy(sku: SkuVO) {
  return live.value && auth.isLogin && sku.stockStatus === 1;
}

async function buy(sku: SkuVO) {
  if (!auth.isLogin) {
    toast('请先登录');
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  if (buying.value || sku.stockStatus !== 1) return;
  buying.value = true;
  try {
    const resp = await seckillApi.order({ activityId: activityId.value, skuId: sku.skuId });
    toast(`抢购成功，订单号 ${resp.orderNo}，请尽快支付`);
    payingOrderNo.value = resp.orderNo;
    // 结果轮询：跟踪订单最终状态
    pollOrder(resp.orderNo);
  } catch (e) {
    const err = e as Error & { code?: number };
    if (err.code === 2004) toast('手慢了，已售罄');
    else if (err.code === 2005) toast('你已参与过该活动商品');
    else if (err.code === 2006) toast('请求过于频繁，请稍后再试');
    // 其余错误已在拦截器提示
  } finally {
    buying.value = false;
    // 按钮防抖：短暂禁用后刷新库存
    setTimeout(load, 1500);
  }
}

function pollOrder(orderNo: string) {
  if (pollTimer) window.clearInterval(pollTimer);
  polling.value = true;
  let times = 0;
  pollTimer = window.setInterval(async () => {
    times++;
    try {
      const order = await orderApi.result(orderNo);
      if (order.status !== 0 || times > 15) {
        window.clearInterval(pollTimer);
        polling.value = false;
        if (order.status === 1) toast('订单已支付');
        else if (order.status === 2) toast('订单已超时关闭');
      }
      await load();
    } catch (e) {
      window.clearInterval(pollTimer);
      polling.value = false;
    }
  }, 2000);
}
</script>

<template>
  <div v-if="loading" class="empty">加载中…</div>
  <template v-else-if="detail">
    <div class="card">
      <div class="title">{{ detail.name }}</div>
      <div class="row" style="margin-top: 6px">
        <CountDown :start-time="detail.startTime" :end-time="detail.endTime" />
        <span class="muted">{{ payingOrderNo ? `上次抢购订单：${payingOrderNo}` : '' }}</span>
      </div>
    </div>

    <div v-for="sku in detail.skus" :key="sku.skuId" class="card">
      <div class="row">
        <div style="display: flex; gap: 14px; align-items: center">
          <div class="item-img" style="width: 84px; height: 84px; margin: 0">{{ sku.title }}</div>
          <div>
            <div style="font-weight: 600">{{ sku.title }}</div>
            <div class="price" style="margin-top: 4px">
              ¥ {{ sku.seckillPrice }} <span class="muted" style="text-decoration: line-through">¥ {{ sku.originalPrice }}</span>
            </div>
            <div class="muted" style="margin-top: 4px">剩余 {{ sku.stockLeft }}</div>
          </div>
        </div>
        <button
          class="btn"
          :class="{ 'btn-danger': canBuy(sku) }"
          :disabled="!canBuy(sku) || buying"
          @click="buy(sku)"
        >
          {{ !live ? '未开始' : sku.stockStatus !== 1 ? '已售罄' : '立即抢购' }}
        </button>
      </div>
    </div>

    <div v-if="!auth.isLogin" class="card muted" style="text-align: center">
      登录后即可抢购 → <router-link to="/login">去登录</router-link>
    </div>

    <PayDialog
      v-if="payingOrderNo"
      :order-no="payingOrderNo"
      @close="payingOrderNo = null"
      @paid="() => { payingOrderNo = null; load(); }"
    />
  </template>
  <div v-else class="empty">活动不存在</div>
</template>