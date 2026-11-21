<template>
  <div class="admin-users">
    <div class="page-header"><h2>用户管理</h2></div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable @keyup.enter="load" style="width:240px" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>

    <el-table :data="users" border stripe style="width:100%" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickName" label="昵称" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="VIP" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.isVip === 1" @change="toggleVip(row, $event)" />
        </template>
      </el-table-column>
      <el-table-column label="Status" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row, $event)" />
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
import { get, put } from '@/utils/api'
import { ElMessage } from 'element-plus'

const users = ref([])
const total = ref(0)
const keyword = ref('')
const pageSize = 20

onMounted(() => load(1))

async function load(pageNum = 1) {
  const res = await get('/admin/users', { pageNum, pageSize, keyword: keyword.value })
  users.value = res.list || []
  total.value = res.total || 0
}

async function toggleVip(row, val) {
  await put(`/admin/users/${row.id}/vip`, { isVip: val ? 1 : 0 })
  row.isVip = val ? 1 : 0
  ElMessage.success('Updated')
}

async function toggleStatus(row, val) {
  await put(`/admin/users/${row.id}/status`, { status: val ? 1 : 0 })
  row.status = val ? 1 : 0
  ElMessage.success('Updated')
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>