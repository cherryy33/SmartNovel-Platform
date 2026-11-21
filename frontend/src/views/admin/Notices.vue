<template>
  <div class="admin-notices">
    <div class="page-header">
      <div>
        <h2>📢 公告管理</h2>
        <span class="hint">发布系统公告，启用后将在读者端展示</span>
      </div>
      <el-button type="primary" @click="openAdd">添加公告</el-button>
    </div>

    <el-table
      :data="notices"
      border
      stripe
      style="width: 100%"
      v-loading="loading"
      empty-text="暂无公告数据"
    >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="公告标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" label="公告内容" min-width="280" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeNotice(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="load"
      style="margin-top: 16px; justify-content: flex-end"
    />

    <!-- 新增/编辑公告弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑公告' : '添加公告'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入公告内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { get, post, put, del } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const notices = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ title: '', content: '', status: 1 })

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题最长 100 个字', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

onMounted(() => load(1))

async function load(page = 1) {
  pageNum.value = page
  loading.value = true
  try {
    const res = await get('/admin/notices', { pageNum: page, pageSize: pageSize.value })
    // 后端返回分页对象 { list, total }，而不是数组
    notices.value = res?.list || []
    total.value = Number(res?.total || 0)
  } catch (e) {
    notices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  form.title = ''
  form.content = ''
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.title = row.title || ''
  form.content = row.content || ''
  form.status = row.status === 0 ? 0 : 1
  dialogVisible.value = true
}

function resetForm() {
  editingId.value = null
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { title: form.title.trim(), content: form.content.trim(), status: form.status }
    if (editingId.value) {
      await put(`/admin/notices/${editingId.value}`, payload)
      ElMessage.success('公告已更新')
    } else {
      await post('/admin/notices', payload)
      ElMessage.success('公告已创建')
    }
    dialogVisible.value = false
    await load(pageNum.value)
  } catch (e) {
    // api.js 已提示具体错误
  } finally {
    saving.value = false
  }
}

async function removeNotice(row) {
  try {
    await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await del(`/admin/notices/${row.id}`)
    ElMessage.success('公告已删除')
    await load(pageNum.value)
  } catch (e) {
    // api.js 已提示具体错误
  }
}

function formatTime(t) {
  if (!t) return '—'
  return new Date(t).toLocaleString('zh-CN')
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0 0 4px; font-size: 20px; }
.hint { color: #909399; font-size: 13px; }
</style>
