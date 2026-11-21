<template>
  <div class="register-page">
    <div class="register-card">
      <h2>创建账号</h2>

      <!-- 三端身份选择 -->
      <div class="role-switcher">
        <div
          v-for="r in roleOptions"
          :key="r.value"
          class="role-item"
          :class="{ active: form.role === r.value }"
          @click="form.role = r.value"
        >
          <span class="role-icon">{{ r.icon }}</span>
          <span class="role-name">{{ r.label }}</span>
          <span class="role-desc">{{ r.desc }}</span>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item v-if="form.role === 'AUTHOR'" label="笔名" prop="penName">
          <el-input v-model="form.penName" placeholder="留空则使用用户名作为笔名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6个字符" prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password placeholder="请再次输入密码" prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width:100%">
            注册并进入{{ currentRoleLabel }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const roleOptions = [
  { value: 'READER', label: '读者', icon: '📖', desc: '阅读与收藏' },
  { value: 'AUTHOR', label: '作家', icon: '✍️', desc: '创作与发布' },
  { value: 'ADMIN', label: '管理者', icon: '🛡️', desc: '平台管理' }
]

const loading = ref(false)
const formRef = ref(null)
const form = reactive({
  role: String(route.query.role || 'READER').toUpperCase(),
  username: '',
  password: '',
  confirm: '',
  penName: ''
})

if (!roleOptions.some(r => r.value === form.role)) form.role = 'READER'

const currentRoleLabel = computed(
  () => roleOptions.find(r => r.value === form.role)?.label || '读者'
)

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度需在 2-50 之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

watch(() => form.role, () => {
  formRef.value?.clearValidate?.()
})

const homeByRole = { ADMIN: '/admin', AUTHOR: '/author', READER: '/' }

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register(form.username, form.password, form.role, {
      penName: form.penName || undefined
    })
    ElMessage.success(`注册成功，已以${currentRoleLabel.value}身份登录`)
    router.push(homeByRole[form.role] || '/')
  } catch (e) {
    // api.js 拦截器已弹出后端具体错误
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
  padding: 24px 0;
}
.register-card {
  width: 100%;
  max-width: 460px;
  padding: 36px 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.register-card h2 { text-align: center; margin: 0 0 20px; }

.role-switcher {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 22px;
}
.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 12px 6px;
  border: 2px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.role-item:hover { border-color: #c6e2ff; }
.role-item.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.12);
}
.role-icon { font-size: 22px; }
.role-name { font-size: 15px; font-weight: 600; color: #303133; }
.role-desc { font-size: 11px; color: #909399; }

.register-footer { text-align: center; margin-top: 16px; font-size: 14px; }
.register-footer a { color: #409eff; }
</style>
