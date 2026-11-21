<template>
  <div class="admin-authors">
    <div class="page-header">
      <h2>✍️ 作家入驻审核</h2>
      <span class="hint">审核读者提交的作家入驻申请，通过后该账号自动升级为作家</span>
    </div>

    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="load(1)">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="load(pageNum)">刷新</el-button>
    </div>

    <el-table :data="authors" border stripe style="width:100%" v-loading="loading" empty-text="暂无数据">
      <el-table-column prop="username" label="申请人账号" width="150" />
      <el-table-column prop="penName" label="笔名" width="140" />
      <el-table-column prop="intro" label="创作简介" min-width="220" show-overflow-tooltip />
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ row.statusText || statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驳回原因" min-width="160">
        <template #default="{ row }">
          <span v-if="row.reviewComment" class="reject-reason">{{ row.reviewComment }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <template v-if="isPending(row)">
            <el-button size="small" type="success" :disabled="auditing" @click="audit(row, true)">通过</el-button>
            <el-button size="small" type="danger" :disabled="auditing" @click="openReject(row)">驳回</el-button>
          </template>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="load"
      style="margin-top:16px"
    />

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectDialog" title="驳回申请" width="420px">
      <p class="reject-target">
        驳回「{{ rejectTarget?.username }}」的入驻申请（笔名：{{ rejectTarget?.penName }}）
      </p>
      <el-input
        v-model="rejectRemark"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="请填写驳回原因（可选），该原因会展示给申请人"
      />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/api'
import { ElMessage } from 'element-plus'

const authors = ref([])
const total = ref(0)
const pageSize = 20
const pageNum = ref(1)
const statusFilter = ref(0)
const loading = ref(false)

const rejectDialog = ref(false)
const rejectTarget = ref(null)
const rejectRemark = ref('')
const auditing = ref(false)

onMounted(() => load(1))

async function load(page = 1) {
  pageNum.value = page
  loading.value = true
  try {
    const res = await get('/admin/author/applies', {
      pageNum: page,
      pageSize,
      status: statusFilter.value
    })
    authors.value = res.list || []
    total.value = Number(res.total || 0)
  } catch (e) {
    authors.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function audit(row, approved) {
  if (auditing.value) return
  auditing.value = true
  try {
    await post(`/admin/author/apply/${row.id}/audit`, { approved, remark: '' })
    ElMessage.success(approved ? '已通过，该账号已升级为作家' : '已驳回')
    await load(pageNum.value)
  } catch (e) {
    // api.js 已提示具体错误
  } finally {
    auditing.value = false
  }
}

function openReject(row) {
  rejectTarget.value = row
  rejectRemark.value = ''
  rejectDialog.value = true
}

async function confirmReject() {
  if (!rejectTarget.value) return
  auditing.value = true
  try {
    await post(`/admin/author/apply/${rejectTarget.value.id}/audit`, {
      approved: false,
      remark: rejectRemark.value
    })
    ElMessage.success('已驳回，申请人可修改后重新提交')
    rejectDialog.value = false
    await load(pageNum.value)
  } catch (e) {
    // api.js 已提示
  } finally {
    auditing.value = false
  }
}

function statusLabel(s) {
  return statusNum(s) === 1 ? '已通过' : statusNum(s) === 2 ? '已驳回' : '待审核'
}

function statusType(s) {
  const n = statusNum(s)
  return n === 1 ? 'success' : n === 2 ? 'danger' : 'warning'
}

// 后端 Jackson 配置把数字序列化为字符串（如 "0"），必须归一化后再比较
function statusNum(s) {
  return s === null || s === undefined || s === '' ? null : Number(s)
}

// 仅待审核（status=0）的申请显示「通过/驳回」按钮
function isPending(row) {
  return statusNum(row.status) === 0
}

function formatTime(t) {
  if (!t) return '—'
  return new Date(t).toLocaleString('zh-CN')
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0; font-size: 20px; }
.hint { color: #909399; font-size: 13px; }
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.muted { color: #c0c4cc; }
.reject-reason { color: #f56c6c; font-size: 13px; }
.reject-target { margin: 0 0 12px; color: #606266; font-size: 14px; }
</style>
