<template>
  <div class="activity-page">
    <!-- 筛选 + 新建 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="reload">
          <el-option v-for="(label, code) in STATUS_TEXT" :key="code" :label="label" :value="Number(code)" />
        </el-select>
        <el-button type="primary" @click="openCreate">新建活动</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="110" />
        <el-table-column prop="name" label="活动名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ STATUS_TEXT[row.status as number] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openSkus(row)">SKU 配置</el-button>
            <template v-if="row.status === 0">
              <el-button link type="warning" @click="changeStatus(row, 1)">进入预热</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button link type="warning" @click="preheat(row)">预热缓存</el-button>
              <el-button link type="success" @click="changeStatus(row, 2)">开始抢购</el-button>
            </template>
            <template v-else-if="row.status === 2">
              <el-button link type="danger" @click="changeStatus(row, 3)">结束</el-button>
            </template>
            <el-button link type="info" @click="goStock(row)">库存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        @current-change="onPage"
        @size-change="onSize"
      />
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editing ? '编辑活动' : '新建活动'" width="480px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="editForm.name" maxlength="64" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="editForm.startTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="editForm.endTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- SKU 配置弹窗 -->
    <el-dialog v-model="skuVisible" :title="`SKU 配置 · ${skuActivity?.name ?? ''}`" width="680px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="按 skuId 幂等 upsert：同一 skuId 重复提交将覆盖价格与库存。"
        class="sku-tip"
      />
      <el-table :data="skuRows" size="small" class="sku-table">
        <el-table-column label="skuId" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.skuId" :min="1" :controls="false" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="秒杀价" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.seckillPrice" :min="0.01" :precision="2" :step="1" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="原价" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.originalPrice" :min="0.01" :precision="2" :step="1" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="秒杀库存" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.seckillStock" :min="0" :step="10" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="skuRows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="sku-actions">
        <el-button @click="addSkuRow">添加商品</el-button>
      </div>
      <template #footer>
        <el-button @click="skuVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingSku" :disabled="skuRows.length === 0" @click="saveSkus">保存 SKU</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { adminActivityApi } from '../../api';
import type { Activity, SkuUpsert } from '../../types';

const STATUS_TEXT: Record<number, string> = { 0: '草稿', 1: '预热', 2: '进行中', 3: '结束' };
const STATUS_TAG: Record<number, 'info' | 'warning' | 'success' | 'danger'> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger'
};

const router = useRouter();
const loading = ref(false);
const rows = ref<Activity[]>([]);
const total = ref(0);
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined });

const statusTag = (s: number) => STATUS_TAG[s] ?? 'info';

const load = async () => {
  loading.value = true;
  try {
    const page = await adminActivityApi.list(query.page, query.size, query.status);
    rows.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
};
const reload = () => {
  query.page = 1;
  load();
};
const onPage = (p: number) => {
  query.page = p;
  load();
};
const onSize = (s: number) => {
  query.size = s;
  reload();
};

// ---------- 新建 / 编辑 ----------
const editVisible = ref(false);
const editFormRef = ref<FormInstance>();
const saving = ref(false);
const editing = ref<Activity | null>(null);
const editForm = reactive({ name: '', startTime: '', endTime: '' });
const editRules: FormRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
};

const openCreate = () => {
  editing.value = null;
  editForm.name = '';
  editForm.startTime = '';
  editForm.endTime = '';
  editVisible.value = true;
};

const openEdit = (row: Activity) => {
  editing.value = row;
  editForm.name = row.name;
  editForm.startTime = row.startTime;
  editForm.endTime = row.endTime;
  editVisible.value = true;
};

const saveEdit = async () => {
  await editFormRef.value?.validate();
  if (editForm.endTime <= editForm.startTime) {
    ElMessage.error('结束时间必须晚于开始时间');
    return;
  }
  saving.value = true;
  try {
    if (editing.value) {
      await adminActivityApi.update(String(editing.value.id), editForm);
      ElMessage.success('活动已更新');
    } else {
      await adminActivityApi.create(editForm);
      ElMessage.success('活动已创建，请配置 SKU 并进入预热');
    }
    editVisible.value = false;
    load();
  } finally {
    saving.value = false;
  }
};

// ---------- 状态流转 ----------
const changeStatus = async (row: Activity, status: number) => {
  const target = STATUS_TEXT[status];
  await ElMessageBox.confirm(`确认将活动「${row.name}」流转为「${target}」？`, '状态流转', { type: 'warning' });
  await adminActivityApi.changeStatus(String(row.id), status);
  ElMessage.success(`已流转为「${target}」`);
  load();
};

const preheat = async (row: Activity) => {
  await ElMessageBox.confirm(`确认对活动「${row.name}」执行缓存预热？将重建布隆过滤器与 Redis 预库存。`, '缓存预热', {
    type: 'warning'
  });
  const result = await adminActivityApi.preheat(String(row.id));
  ElMessage.success(`预热完成，预库存分段 ${result.stockKeys?.length ?? 0} 个`);
  load();
};

// ---------- SKU 配置 ----------
const skuVisible = ref(false);
const skuRows = ref<SkuUpsert[]>([]);
const skuActivity = ref<Activity | null>(null);
const savingSku = ref(false);

const addSkuRow = () => {
  skuRows.value.push({ skuId: 0, seckillPrice: 1, originalPrice: 1, seckillStock: 100 });
};

const openSkus = (row: Activity) => {
  skuActivity.value = row;
  skuRows.value = [{ skuId: 0, seckillPrice: 1, originalPrice: 1, seckillStock: 100 }];
  skuVisible.value = true;
};

const saveSkus = async () => {
  if (skuRows.value.some((r) => r.skuId <= 0)) {
    ElMessage.error('skuId 必须大于 0');
    return;
  }
  if (!skuActivity.value) return;
  savingSku.value = true;
  try {
    const count = await adminActivityApi.saveSkus(String(skuActivity.value.id), skuRows.value);
    ElMessage.success(`已保存 ${count} 项 SKU`);
    skuVisible.value = false;
  } finally {
    savingSku.value = false;
  }
};

// ---------- 库存视图 ----------
const goStock = (row: Activity) => router.push(`/activities/${row.id}/stock?name=${encodeURIComponent(row.name)}`);

onMounted(load);
</script>

<style scoped>
.activity-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar-row {
  display: flex;
  justify-content: space-between;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.sku-tip {
  margin-bottom: 12px;
}
.sku-table {
  margin-bottom: 12px;
}
.sku-actions {
  display: flex;
  justify-content: flex-end;
}
</style>