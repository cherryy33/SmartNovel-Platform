<template>
  <div class="app-wrapper">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-inner">
        <!-- Logo -->
        <router-link to="/" class="logo">智阅小说</router-link>

        <!-- 主导航 -->
        <nav class="nav-links">
          <router-link to="/" class="nav-item" active-class="nav-active">首页</router-link>
          <router-link to="/categories" class="nav-item" active-class="nav-active">分类</router-link>
          <router-link to="/community" class="nav-item" active-class="nav-active">社区</router-link>
          <router-link to="/search" class="nav-item" active-class="nav-active">搜索</router-link>
        </nav>

        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            size="small"
            placeholder="搜索小说..."
            prefix-icon="Search"
            clearable
            @keyup.enter="goSearch"
          />
        </div>

        <!-- 右侧用户区域 -->
        <div class="user-area">
          <template v-if="authStore.isLoggedIn">
            <!-- 作家工作台入口：作者 或 管理员可见 -->
            <router-link
              v-if="authStore.isAuthor || authStore.isAdmin"
              to="/author"
              class="workspace-link"
            >
              作家工作台
            </router-link>

            <!-- 管理后台入口：仅管理员 -->
            <router-link v-if="authStore.isAdmin" to="/admin" class="workspace-link admin-link">
              🛡️ 管理后台
            </router-link>

            <!-- 申请成为作者：普通读者可见 -->
            <el-button
              v-if="authStore.isReader"
              size="small"
              type="primary"
              plain
              class="apply-author-btn"
              @click="applyDialogRef?.open()"
            >
              ✍️ 申请成为作者
            </el-button>

            <!-- 消息 -->
            <router-link to="/messages" class="icon-link">
              <el-icon :size="20"><Bell /></el-icon>
            </router-link>

            <!-- 用户下拉 -->
            <el-dropdown trigger="click" @command="handleUserCommand">
              <span class="user-dropdown-trigger">
                <el-avatar
                  :size="32"
                  :src="authStore.user.userPhoto"
                  :icon="UserFilled"
                />
                <span class="user-nickname">{{ authStore.user.nickName || authStore.user.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <!-- 管理后台置顶显示 -->
                  <el-dropdown-item v-if="authStore.isAdmin" command="admin">
                    <el-icon><Setting /></el-icon> 管理后台
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="authStore.isAuthor || authStore.isAdmin"
                    command="author"
                  >
                    <el-icon><EditPen /></el-icon> 作家工作台
                  </el-dropdown-item>
                  <el-dropdown-item v-if="authStore.isReader" command="apply-author">
                    <el-icon><EditPen /></el-icon> 申请成为作者
                  </el-dropdown-item>
                  <el-dropdown-item command="profile" divided>
                    <el-icon><User /></el-icon> 个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="bookshelf">
                    <el-icon><Collection /></el-icon> 我的书架
                  </el-dropdown-item>
                  <el-dropdown-item command="history">
                    <el-icon><Clock /></el-icon> 阅读历史
                  </el-dropdown-item>
                  <el-dropdown-item v-if="!authStore.isVip" command="vip" divided>
                    <el-icon><Star /></el-icon> VIP 升级
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <router-link to="/login">
              <el-button size="small" text>登录</el-button>
            </router-link>
            <router-link to="/register">
              <el-button size="small" type="primary">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="app-main">
      <router-view />
    </main>

    <!-- 读者申请成为作者弹窗 -->
    <AuthorApplyDialog ref="applyDialogRef" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AuthorApplyDialog from '@/components/AuthorApplyDialog.vue'
import {
  Bell, User, Collection, Clock, Star, SwitchButton,
  ArrowDown, UserFilled, Setting, EditPen
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')
const applyDialogRef = ref(null)

function goSearch() {
  const kw = searchKeyword.value.trim()
  if (kw) {
    router.push({ path: '/search', query: { q: kw } })
  }
}

function handleUserCommand(command) {
  const map = {
    profile: '/profile',
    bookshelf: '/bookshelf',
    history: '/history',
    vip: '/profile?tab=vip',
    author: '/author',
    admin: '/admin'
  }
  if (command === 'logout') {
    authStore.logout()
    router.push('/')
  } else if (command === 'apply-author') {
    applyDialogRef.value?.open()
  } else if (map[command]) {
    router.push(map[command])
  }
}
</script>

<style>
/* 全局重置 */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background: #f5f7fa;
  color: #303133;
}

#app {
  height: 100%;
}

a {
  text-decoration: none;
  color: inherit;
}
</style>

<style scoped>
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ===== Header ===== */
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 24px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  white-space: nowrap;
  letter-spacing: 1px;
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-item {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
  transition: color 0.2s, background 0.2s;
}

.nav-item:hover {
  color: #409eff;
  background: #ecf5ff;
}

.nav-active {
  color: #409eff;
  font-weight: 600;
}

.search-bar {
  flex: 1;
  max-width: 320px;
}

/* 右侧用户区域 */
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.workspace-link {
  font-size: 13px;
  color: #409eff;
  padding: 4px 10px;
  border: 1px solid #409eff;
  border-radius: 4px;
  transition: background 0.2s;
}

.workspace-link:hover {
  background: #ecf5ff;
}

.admin-link {
  color: #e6a23c;
  border-color: #e6a23c;
}

.admin-link:hover {
  background: #fdf6ec;
}

.icon-link {
  color: #606266;
  padding: 4px;
  display: flex;
  align-items: center;
}

.icon-link:hover {
  color: #409eff;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-dropdown-trigger:hover {
  background: #f5f7fa;
}

.user-nickname {
  font-size: 14px;
  color: #303133;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 主体 ===== */
.app-main {
  flex: 1;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-inner {
    gap: 12px;
    padding: 0 12px;
  }

  .logo {
    font-size: 18px;
  }

  .nav-links {
    gap: 0;
  }

  .nav-item {
    padding: 4px 8px;
    font-size: 13px;
  }

  .search-bar {
    max-width: 160px;
  }

  .user-nickname {
    display: none;
  }
}
</style>
