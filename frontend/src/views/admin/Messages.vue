<template>
  <div class="admin-messages">
    <div class="page-header"><h2>留言板管理</h2></div>

    <el-table :data="messages" border stripe style="width:100%" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickName" label="用户" width="100" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="reply" label="回复" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已回复' : '新留言' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="!row.reply" size="small" @click="reply(row)">回复</el-button>
          <el-button size="small" type="danger" @click="removeMsg(row.id)">删除</el-button>
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

    <!-- 回复对话框 -->
    <el-dialog v-model="showReplyDialog" title="回复" width="400px">
      <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="写回复..." />
      <div style="margin-top:12px">
        <el-button type="primary" @click="doReply" :loading="replying">提交</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post, del } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const messages = ref([])
const total = ref(0)
const pageSize = 20
const showReplyDialog = ref(false)
const replying = ref(false)
const replyContent = ref('')
const currentMsg = ref(null)

onMounted(() => load(1))

async function load(pageNum = 1) {
  const res = await get('/admin/messages', { pageNum, pageSize })
  messages.value = res.list || []
  total.value = res.total || 0
}

function reply(row) {
  currentMsg.value = row
  replyContent.value = ''
  showReplyDialog.value = true
}

async function doReply() {
  if (!replyContent.value.trim()) { ElMessage.warning('请输入内容'); return }
  replying.value = true
  try {
    await post(`/admin/messages/${currentMsg.value.id}/reply`, { reply: replyContent.value })
    ElMessage.success('已回复')
    showReplyDialog.value = false
    currentMsg.value = null
    load()
  } finally {
    replying.value = false
  }
}

async function removeMsg(id) {
  await ElMessageBox.confirm('确认删除？', '确认', { type: 'warning' })
  await del(`/admin/messages/${id}`)
  ElMessage.success('已删除')
  load()
}
</script>
