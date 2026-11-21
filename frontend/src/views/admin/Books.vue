<template>
  <div class="admin-books">
    <div class="page-header"><h2>作品管理</h2></div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索作品名称" clearable @keyup.enter="load" style="width:240px" />
      <el-select v-model="auditStatus" placeholder="审核状态" clearable @change="load">
        <el-option label="待审核" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-button type="primary" @click="load">搜索</el-button>
    </div>

    <el-table :data="books" border stripe style="width:100%" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="bookName" label="作品名称" />
      <el-table-column prop="authorName" label="作者" />
      <el-table-column prop="categoryName" label="分类" />
      <el-table-column prop="visitCount" label="阅读量" width="80" />
      <el-table-column label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag :type="auditStatusType(row.auditStatus)" size="small">
            {{ auditStatusText(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="approve(row)">通过</el-button>
          <el-button size="small" type="danger" @click="reject(row)">拒绝</el-button>
          <el-button size="small" type="danger" @click="removeBook(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      layout="prev, next"
      :total="total"
      :page-size="pageSize"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, put, del } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const books = ref([])
const total = ref(0)
const keyword = ref('')
const auditStatus = ref(null)
const pageSize = 20

onMounted(() => load(1))

async function load(pageNum = 1) {
  const res = await get('/admin/books', { pageNum, pageSize, keyword: keyword.value, auditStatus: auditStatus.value })
  books.value = res.list || []
  total.value = res.total || 0
}

async function approve(row) {
  await put(`/admin/books/${row.id}/status`, { auditStatus: 1 })
  row.auditStatus = 1
  ElMessage.success('已通过')
}

async function reject(row) {
  await put(`/admin/books/${row.id}/status`, { auditStatus: 2 })
  row.auditStatus = 2
  ElMessage.success('已拒绝')
}

async function removeBook(bookId) {
  await ElMessageBox.confirm('删除这部作品？', '确认', { type: 'warning' })
  await del(`/admin/books/${bookId}`)
  ElMessage.success('已删除')
  load()
}

function auditStatusType(s) { return s === 1 ? 'success' : s === 2 ? 'danger' : 'warning' }
function auditStatusText(s) { return s === 1 ? '已发布' : s === 2 ? '已拒绝' : '待审核' }
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>