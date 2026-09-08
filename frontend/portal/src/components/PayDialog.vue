<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import { payApi } from '../api';
import { toast } from '../api/http';
import type { PayMockResp } from '../types';

const props = defineProps<{ orderNo: string }>();
const emit = defineEmits<{ (e: 'close'): void; (e: 'paid'): void }>();

// 阶段：step=1 发起支付(mock) → step=2 已发起（展示 payNo/sign，等待确认）→ step=3 支付中(轮询) → done
const step = ref<1 | 2 | 3>(1);
const loading = ref(false);
const pay = ref<PayMockResp | null>(null);
let pollTimer: number | undefined;

const canConfirm = computed(() => !!pay.value && step.value === 2);

async function doMock() {
  try {
    loading.value = true;
    pay.value = await payApi.mock(props.orderNo);
    step.value = 2;
  } finally {
    loading.value = false;
  }
}

async function doCallback() {
  if (!pay.value) return;
  try {
    loading.value = true;
    await payApi.callback({
      payNo: pay.value.payNo,
      orderNo: props.orderNo,
      amount: pay.value.amount,
      sign: pay.value.sign
    });
    toast('支付成功');
    emit('paid');
    emit('close');
  } finally {
    loading.value = false;
  }
}

onBeforeUnmount(() => window.clearInterval(pollTimer));
</script>

<template>
  <div class="mask" @click.self="emit('close')">
    <div class="dialog">
      <div class="dialog-title">模拟支付</div>
      <p class="muted" style="margin-bottom: 12px">订单号：<span class="mono">{{ orderNo }}</span></p>

      <template v-if="step === 1">
        <button class="btn" :disabled="loading" @click="doMock">
          {{ loading ? '发起中…' : '发起支付（mock 预签名）' }}
        </button>
      </template>

      <template v-else-if="step === 2 && pay">
        <div class="field">
          <span class="label">支付单号 payNo</span>
          <div class="mono">{{ pay.payNo }}</div>
        </div>
        <div class="field">
          <span class="label">金额</span>
          <div>¥ {{ pay.amount }}</div>
        </div>
        <div class="field">
          <span class="label">预签名 sign（支付网关将随回调携带）</span>
          <div class="mono">{{ pay.sign }}</div>
        </div>
        <div class="row">
          <button class="btn btn-plain" @click="emit('close')">取消</button>
          <button class="btn" :disabled="loading || !canConfirm" @click="doCallback">
            {{ loading ? '处理中…' : '确认支付（回调验签）' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>