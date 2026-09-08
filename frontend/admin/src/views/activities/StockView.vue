<template>
  <div class="stock-page">
    <el-page-header @back="router.back()" class="page-header">
      <template #content>
        <span class="header-title">库存与预热 · {{ name }}</span>
      </template>
    </el-page-header>

    <el-card shadow="never">
      <div class="actions">
        <el-button type="warning" :loading="preheating" @click="doPreheat">执行缓存预热</el-button>
        <el-button type="primary" :loading="loading" @click="load">刷新快照</el-button>
        <el-tag v-if="snapshot" :type="snapshot.bloomAllReady ? 'success' : 'info'" effect="dark">
          {{ snapshot.bloomAllReady ? '全局布隆已就绪' : '布隆过滤器未初始化（需预热）' }}
        </el-tag>
        <span class="save-hint">提示：预热需要活动处于「预热」状态，先返回活动列表流转状态。</span>
      </div>
    </el-card>

    <el-card shadow="never" v-loading="loading">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="DB 与 Redis 预库存存在预扣差异属正常现象，对账任务会兜底回滚；关注 redisStock 是否随抢购递减。"
        class="tip"
      />
      <el-table :data="rows" stripe>
        <el-table-column prop="skuId" label="skuId" width="160" />
        <el-table-column label="DB 配置库存">
          <template #default="{ row }">
            <el-tag type="info">{{ row.dbStock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Redis 预库存">
          <template #default="{ row }">
            <el-tag :type="row.redisStock > 0 ? 'success' : 'danger'">{{ row.redisStock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="消费进度">
          <template #default="{ row }">
            <el-progress
              :percentage="consumedPercent(row)"
              :stroke-width="14"
              :format="() => `${row.dbStock - row.redisStock} / ${row.dbStock}`"
            />
          </template>
        </el-table-column>
      </el-table>
      <div v-if="rows.length === 0 && !loading" class="empty">该活动暂无 SKU，请先配置。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { adminActivityApi } from '../../api';
import type { StockSnapshot } from '../../types';

const props = defineProps<{ id: string }>();
const route = useRoute();
const router = useRouter();
const name = computed(() => String(route.query.name ?? props.id));

const snapshot = ref<StockSnapshot | null>(null);
const loading = ref(false);
const preheating = ref(false);

const rows = computed(() => snapshot.value?.items ?? []);

const load = async () => {
  loading.value = true;
  try {
    snapshot.value = await adminActivityApi.stock(props.id);
  } finally {
    loading.value = false;
  }
};

const consumedPercent = (row: { dbStock: number; redisStock: number }) => {
  if (!row.dbStock) return 0;
  const consumed = Math.max(0, Math.min(row.dbStock - row.redisStock, row.dbStock));
  return Math.round((consumed / row.dbStock) * 100);
};

const doPreheat = async () => {
  await ElMessageBox.confirm('执行预热将重建布隆过滤器、活动缓存与 Redis 预库存分段，确定继续？', '缓存预热', {
    type: 'warning'
  });
  preheating.value = true;
  try {
    const result = await adminActivityApi.preheat(props.id);
    ElMessage.success(`预热完成：预库存分段 ${result.stockKeys?.length ?? 0} 个（已按 TTL 抖动写入 Redis）`);
    load();
  } finally {
    preheating.value = false;
  }
};

load();
</script>

<style scoped>
.stock-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header {
  background: #fff;
  border-radius: 8px;
  padding: 8px 16px;
}
.header-title {
  font-weight: 600;
  font-size: 16px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.save-hint {
  color: #999;
  font-size: 12px;
  margin-left: auto;
}
.tip {
  margin-bottom: 12px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 24px 0;
}
</style>