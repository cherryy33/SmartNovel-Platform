<template>
  <div class="admin-comments">
    <div class="page-header"><h2>评论管理</h2></div>

    <div class="toolbar">
      <el-select v-model="auditStatus" placeholder="Audit Status" clearable @change="load">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
    </div>

    <el-table :data="comments" border stripe style="width:100%" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickName" label="用户" />
      <el-table-column prop="content" label="内容" />
      <el-table-column label="Status" width="80">
        <template #default="{ row }">
          <el-tag :type="row.auditStatus === 1 ? 'success' : row.auditStatus === 2 ? 'danger' : 'warning'" size="small">
            {{ row.auditStatus === 1 ? '正常' : row.auditStatus === 2 ? '已拒' : '待审' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="audit(row.id, 1)">通过</el-button>
          <el-button size="small" type="danger" @click="audit(row.id, 2)">拒绝</el-button>
          <el-popconfirm title="删除这条评论？" @confirm="removeComment(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
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
import { get, post, del } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const comments = ref([])
const total = ref(0)
const auditStatus = ref(null)
const pageSize = 20

onMounted(() => load(1))

async function load(pageNum = 1) {
  const res = await get('/admin/comments', { pageNum, pageSize, auditStatus: auditStatus.value })
  comments.value = res.list || []
  total.value = res.total || 0
}

async function audit(commentId, status) {
  await post(`/admin/comments/${commentId}/audit`, { status })
  ElMessage.success('已完成')
  load()
}

async function removeComment(commentId) {
  await ElMessageBox.confirm('确认删除？', '确认', { type: 'warning' })
  await del(`/admin/comments/${commentId}`)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
</style>