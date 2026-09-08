<template>
  <div class="metrics-page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h3 class="title">QPS 监控</h3>
          <p class="desc">后端每 10 秒落一个采样点，保留 2 小时；折线展示入口真实请求吞吐。</p>
        </div>
        <el-radio-group v-model="minutes" @change="reload">
          <el-radio-button :value="5">5 分钟</el-radio-button>
          <el-radio-button :value="15">15 分钟</el-radio-button>
          <el-radio-button :value="60">1 小时</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-card shadow="never" v-loading="loading">
      <div ref="chartRef" class="chart" />
      <el-empty v-if="!loading && points.length === 0" description="暂无采样数据，请先发起请求产生流量" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import { metricsApi } from '../../api';
import type { QpsSample } from '../../types';

const minutes = ref(5);
const loading = ref(false);
const points = ref<QpsSample[]>([]);
const chartRef = ref<HTMLDivElement>();
let chart: echarts.ECharts | null = null;
let timer: number | undefined;

const load = async () => {
  loading.value = true;
  try {
    points.value = await metricsApi.qps(minutes.value);
    render();
  } finally {
    loading.value = false;
  }
};

const reload = () => load();

// 每 10s/请求采样 → 折算为每秒 QPS
const chartData = computed(() =>
  points.value.map((p) => ({ time: formatTime(p.timestamp), qps: +(p.count / 10).toFixed(1) }))
);

const formatTime = (ts: number) => {
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const render = () => {
  if (!chartRef.value) return;
  if (!chart) chart = echarts.init(chartRef.value);
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: chartData.value.map((d) => d.time) },
    yAxis: { type: 'value', name: 'QPS' },
    series: [
      {
        name: '入口请求 QPS',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: chartData.value.map((d) => d.qps),
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 2 }
      }
    ]
  });
};

const onResize = () => chart?.resize();

onMounted(() => {
  load();
  window.addEventListener('resize', onResize);
  timer = window.setInterval(() => load(), 10_000);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  if (timer) window.clearInterval(timer);
  chart?.dispose();
  chart = null;
});
</script>

<style scoped>
.metrics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.title {
  margin: 0 0 4px;
}
.desc {
  margin: 0;
  color: #909399;
  font-size: 13px;
}
.card-head {
  flex: 1;
}
.chart {
  height: 360px;
  width: 100%;
}
</style>