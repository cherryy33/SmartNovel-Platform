<template>
  <div class="login-page">
    <div class="login-card">
      <h2>登录智阅小说</h2>

      <!-- 三端身份选择 -->
      <div class="role-switcher">
        <div
          v-for="r in roleOptions"
          :key="r.value"
          class="role-item"
          :class="{ active: loginRole === r.value }"
          @click="selectRole(r.value)"
        >
          <span class="role-icon">{{ r.icon }}</span>
          <span class="role-name">{{ r.label }}</span>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>

      <!-- 第三方快捷登录 -->
      <div class="oauth-section">
        <div class="oauth-divider"><span>第三方快捷登录</span></div>
        <div class="oauth-buttons">
          <el-button circle @click="oauthLogin('github')" class="oauth-btn github" title="GitHub">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
          </el-button>
          <el-button circle @click="oauthLogin('wechat')" class="oauth-btn wechat" title="WeChat">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 01.598.082l1.584.926a.272.272 0 00.14.045c.134 0 .24-.11.24-.245 0-.06-.024-.12-.04-.178l-.325-1.233a.49.49 0 01.178-.554C23.028 18.48 24 16.82 24 14.98c0-3.21-2.931-5.952-7.062-6.122zm-2.18 2.769c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982z"/></svg>
          </el-button>
          <el-button circle @click="oauthLogin('qq')" class="oauth-btn qq" title="QQ">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2zm4.5 12.5c-.5 1.5-1.5 2.5-2.5 3-1 .5-2.5 1-4 1s-4-.5-6-2c-1-.75-1.5-2-1.5-3 0-.5.5-1 1.5-1s2 .5 3.5.5 3-.5 4.5-.5 3.5.5 4.5 0 1 .5 1 1.5z"/></svg>
          </el-button>
        </div>
      </div>

      <div class="login-footer">
        <router-link to="/register">没有账号？立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const roleOptions = [
  { value: 'READER', label: '读者', icon: '📖' },
  { value: 'AUTHOR', label: '作家', icon: '✍️' },
  { value: 'ADMIN', label: '管理者', icon: '🛡️' }
]
const loginRole = ref('READER')

const loading = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 各身份登录成功后的默认落地页
const homeByRole = { ADMIN: '/admin', AUTHOR: '/author', READER: '/' }

/**
 * 切换身份选项卡：
 * - 不改动用户已输入的用户名/密码（不再强制回填演示账号）
 * - 仅清理上一次的表单校验提示
 */
function selectRole(role) {
  loginRole.value = role
  formRef.value?.clearValidate?.()
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await authStore.login(form.username, form.password, loginRole.value)
    // 仅「作家」「管理者」入口校验身份；「读者」入口零门槛，密码正确即放行
    const needRoleCheck = loginRole.value === 'AUTHOR' || loginRole.value === 'ADMIN'
    const actual = authStore.role
    if (needRoleCheck && actual !== loginRole.value) {
      authStore.logout()
      ElMessage.error('该账号无对应身份权限')
      return
    }
    ElMessage.success('登录成功')
    // 读者入口一律进入读者首页；作家/管理者入口按身份分流（兼容 redirect）
    let target
    if (loginRole.value === 'READER') {
      target = '/'
    } else {
      const redirect = route.query.redirect
      target = redirect && redirect.startsWith(loginRole.value === 'ADMIN' ? '/admin' : '/author')
        ? redirect
        : (homeByRole[loginRole.value] || '/')
    }
    router.push(target)
    void data
  } catch (e) {
    // api.js 已弹后端具体错误消息（如「该账号无对应身份权限」），此处不重复
  } finally {
    loading.value = false
  }
}

async function oauthLogin(provider) {
  try {
    await authStore.oauthLogin(provider)
    ElMessage.success(`已通过 ${provider} 登录`)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    ElMessage.error('第三方登录失败')
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.login-card h2 { text-align: center; margin-bottom: 20px; }

/* 三端身份切换 */
.role-switcher {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}
.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
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
.role-icon { font-size: 20px; }
.role-name { font-size: 14px; font-weight: 600; color: #303133; }

/* OAuth 区域 */
.oauth-section { margin-top: 8px; }
.oauth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  color: #c0c4cc;
  font-size: 13px;
}
.oauth-divider::before, .oauth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #ebeef5;
}
.oauth-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}
.oauth-btn {
  width: 44px;
  height: 44px;
  border: 2px solid #ebeef5;
  transition: all 0.2s;
}
.oauth-btn:hover { transform: scale(1.1); }
.oauth-btn.github { color: #24292e; }
.oauth-btn.github:hover { border-color: #24292e; background: #f6f8fa; }
.oauth-btn.wechat { color: #07c160; }
.oauth-btn.wechat:hover { border-color: #07c160; background: #f0fff4; }
.oauth-btn.qq { color: #12b7f5; }
.oauth-btn.qq:hover { border-color: #12b7f5; background: #f0faff; }

.login-footer { text-align: center; margin-top: 16px; font-size: 14px; }
.login-footer a { color: #409eff; }
</style>