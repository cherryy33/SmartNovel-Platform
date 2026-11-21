<template>
  <div class="admin-categories">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="showAddDialog = true">添加分类</el-button>
    </div>

    <div class="cat-grid">
      <div v-for="c in categories" :key="c.id" class="cat-card">
        <span class="cat-name">{{ c.name }}</span>
        <span class="cat-work">方向: {{ c.workDirection || 0 }}</span>
        <span class="cat-sort">排序: {{ c.sort }}</span>
        <div class="cat-actions">
          <el-button size="small" @click="edit(c)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeCategory(c.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="showAddDialog" :title="editing ? '编辑分类' : '添加分类'" width="400px">
      <el-form @submit.prevent="save">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="创作方向">
          <el-input-number v-model="form.workDirection" :min="0" />
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

const categories = ref([])
const showAddDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const form = ref({ name: '', workDirection: 0, sort: 10 })

onMounted(() => load())

async function load() {
  categories.value = await get('/admin/categories') || []
}

function edit(row) {
  editing.value = row
  form.value = { name: row.name, workDirection: row.workDirection, sort: row.sort }
  showAddDialog.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await put(`/admin/categories/${editing.value.id}`, form.value)
      ElMessage.success('已更新')
    } else {
      await post('/admin/categories', form.value)
      ElMessage.success('已创建')
    }
    showAddDialog.value = false
    editing.value = null
    load()
  } finally {
    saving.value = false
  }
}

async function removeCategory(id) {
  await ElMessageBox.confirm('确认删除？', '确认', { type: 'warning' })
  await del(`/admin/categories/${id}`)
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
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.cat-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.cat-name { font-weight: 600; }
.cat-work, .cat-sort { color: #c0c4cc; font-size: 13px; }
.cat-actions { margin-left: auto; display: flex; gap: 4px; }
</style>