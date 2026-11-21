<template>
  <div class="admin-tags">
    <div class="page-header">
      <h2>标签管理</h2>
      <el-button type="primary" @click="showAddDialog = true">添加标签</el-button>
    </div>

    <div class="tag-grid">
      <div v-for="t in tags" :key="t.id" class="tag-card">
        <span class="tag-name">{{ t.name }}</span>
        <span class="tag-sort">排序: {{ t.sort }}</span>
        <div class="tag-actions">
          <el-button size="small" @click="edit(t)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeTag(t.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="showAddDialog" :title="editing ? '编辑标签' : '添加标签'" width="400px">
      <el-form @submit.prevent="save">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post, put, del } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tags = ref([])
const showAddDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const form = ref({ name: '', sort: 0 })

onMounted(() => load())

async function load() {
  tags.value = await get('/admin/tags') || []
}

function edit(row) {
  editing.value = row
  form.value = { name: row.name, sort: row.sort }
  showAddDialog.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await put(`/admin/tags/${editing.value.id}`, form.value)
      ElMessage.success('已更新')
    } else {
      await post('/admin/tags', form.value)
      ElMessage.success('已创建')
    }
    showAddDialog.value = false
    editing.value = null
    load()
  } finally {
    saving.value = false
  }
}

async function removeTag(id) {
  await ElMessageBox.confirm('确认删除？', '确认', { type: 'warning' })
  await del(`/admin/tags/${id}`)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.tag-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.tag-name { font-weight: 600; }
.tag-sort { color: #c0c4cc; font-size: 13px; }
.tag-actions { margin-left: auto; display: flex; gap: 4px; }
</style>