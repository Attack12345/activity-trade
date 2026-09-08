<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{ startTime: string; endTime: string }>();
const now = ref(Date.now());
let timer: number | undefined;

onMounted(() => {
  timer = window.setInterval(() => (now.value = Date.now()), 1000);
});
onBeforeUnmount(() => window.clearInterval(timer));

const state = computed(() => {
  const start = new Date(props.startTime).getTime();
  const end = new Date(props.endTime).getTime();
  if (now.value < start) return { type: 'pre', text: `${format(start - now.value)} 后开始` };
  if (now.value > end) return { type: 'end', text: '已结束' };
  return { type: 'live', text: `剩余 ${format(end - now.value)}` };
});

function format(ms: number) {
  const s = Math.max(0, Math.floor(ms / 1000));
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return d > 0 ? `${d}天 ${pad(h)}:${pad(m)}:${pad(sec)}` : `${pad(h)}:${pad(m)}:${pad(sec)}`;
}
</script>

<template>
  <span :class="['badge', state.type === 'pre' ? 'badge-pre' : state.type === 'live' ? 'badge-live' : 'badge-end']">
    {{ state.text }}
  </span>
</template>