<template>
  <div class="profile-page">
    <h2>个人中心</h2>

    <div class="profile-tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人资料" name="profile">
          <div class="profile-card" v-if="authStore.user">
            <div class="avatar-section">
              <el-avatar :size="72" :src="authStore.user.userPhoto" :icon="UserFilled" />
              <el-button size="small" @click="editAvatar = true">更换头像</el-button>
            </div>

            <div class="profile-fields">
              <div class="profile-field">
                <span class="label">用户名</span>
                <span>{{ authStore.user.username }}</span>
              </div>
              <div class="profile-field">
                <span class="label">昵称</span>
                <el-input v-if="editing" v-model="editForm.nickName" size="small" style="width:200px" />
                <span v-else>{{ authStore.user.nickName || '—' }}</span>
              </div>
              <div class="profile-field">
                <span class="label">邮箱</span>
                <span>{{ authStore.user.email || '—' }}</span>
              </div>
              <div class="profile-field">
                <span class="label">VIP</span>
                <el-tag :type="authStore.user.isVip === 1 ? 'warning' : 'info'" size="small">
                  {{ authStore.user.isVip === 1 ? '👑 VIP' : '免费' }}
                </el-tag>
                <el-button v-if="authStore.user.isVip !== 1" size="small" type="warning" @click="showVipDialog = true" style="margin-left:8px">
                  升级 VIP
                </el-button>
              </div>
              <div class="profile-field">
                <span class="label">角色</span>
                <el-tag size="small" :type="authStore.isAdmin ? 'danger' : authStore.isAuthor ? 'success' : 'info'">
                  {{ roleText }}
                </el-tag>
              </div>
            </div>

            <div class="profile-buttons">
              <el-button
                v-if="authStore.isReader"
                type="success"
                size="small"
                plain
                @click="applyDialogRef?.open()"
              >✍️ 申请成为作者</el-button>
              <el-button v-if="!editing" type="primary" size="small" @click="startEdit">编辑资料</el-button>
              <template v-else>
                <el-button type="primary" size="small" @click="saveProfile" :loading="saving">保存</el-button>
                <el-button size="small" @click="editing = false">取消</el-button>
              </template>
              <el-button size="small" @click="showPasswordDialog = true">修改密码</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="兴趣标签" name="tags">
          <div class="tags-card">
            <p class="card-desc">选择你喜欢的标签，获得更精准的推荐</p>
            <div class="tag-select-grid">
              <el-check-tag
                v-for="t in allTags"
                :key="t.id"
                :checked="selectedTags.includes(t.id)"
                @change="(checked) => toggleTag(t.id, checked)"
                class="tag-check"
              >
                {{ t.name }}
              </el-check-tag>
            </div>
            <el-button type="primary" @click="saveTags" :loading="savingTags" style="margin-top:16px">保存标签</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="留言板" name="board">
          <div class="board-card">
            <div class="board-form">
              <el-input v-model="boardContent" type="textarea" :rows="3" placeholder="写留言..." />
              <el-button type="primary" size="small" @click="submitBoard" :loading="boarding" style="margin-top:8px">发表</el-button>
            </div>
            <div v-for="m in boardMessages" :key="m.id" class="board-item">
              <div class="board-header">
                <strong>{{ m.nickName || m.username }}</strong>
                <span class="board-time">{{ formatTime(m.createTime) }}</span>
              </div>
              <p class="board-content">{{ m.content }}</p>
              <p v-if="m.reply" class="board-reply">📢 管理员回复: {{ m.reply }}</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="380px">
      <el-form @submit.prevent="changePassword">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="changingPwd">确认</el-button>
      </el-form>
    </el-dialog>

    <!-- VIP 升级弹窗 -->
    <el-dialog v-model="showVipDialog" title="👑 VIP 升级" width="400px" center>
      <div class="vip-dialog-body">
        <el-icon :size="48" color="#e6a23c"><StarFilled /></el-icon>
        <p>解锁全站所有 VIP 章节！</p>
        <p class="vip-sub">一键升级，即刻畅读。</p>
      </div>
      <template #footer>
        <el-button @click="showVipDialog = false">取消</el-button>
        <el-button type="warning" @click="upgradeVipNow" :loading="vipLoading">立即升级</el-button>
      </template>
    </el-dialog>

    <!-- 申请成为作者弹窗 -->
    <AuthorApplyDialog ref="applyDialogRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post, put } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import AuthorApplyDialog from '@/components/AuthorApplyDialog.vue'
import { ElMessage } from 'element-plus'
import { UserFilled, StarFilled } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const applyDialogRef = ref(null)
const activeTab = ref('profile')

const roleText = computed(() => {
  if (authStore.isAdmin) return '管理者'
  if (authStore.isAuthor) return '作家'
  return '读者'
})

const editing = ref(false)
const saving = ref(false)
const editForm = ref({ nickName: '' })

const showPasswordDialog = ref(false)
const changingPwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '' })

const showVipDialog = ref(false)
const vipLoading = ref(false)

const allTags = ref([])
const selectedTags = ref([])
const savingTags = ref(false)

const boardContent = ref('')
const boarding = ref(false)
const boardMessages = ref([])

onMounted(async () => {
  try {
    const tags = await get('/front/tags')
    allTags.value = tags || []
    const userTags = await get('/front/user/tags')
    selectedTags.value = (userTags || []).map(t => t.id)
  } catch (e) { /* silent */ }
  try {
    const res = await get('/front/message-board', { pageNum: 1, pageSize: 30 })
    boardMessages.value = res.list || []
  } catch (e) { /* silent */ }
})

function startEdit() {
  editForm.value = { nickName: authStore.user?.nickName || '' }
  editing.value = true
}

async function saveProfile() {
  saving.value = true
  try {
    await put('/front/user/profile', { nickName: editForm.value.nickName })
    if (authStore.user) {
      authStore.user.nickName = editForm.value.nickName
      localStorage.setItem('user', JSON.stringify(authStore.user))
    }
    ElMessage.success('资料已更新')
    editing.value = false
  } finally { saving.value = false }
}

async function changePassword() {
  changingPwd.value = true
  try {
    await put('/front/user/password', pwdForm.value)
    ElMessage.success('密码已修改')
    showPasswordDialog.value = false
    pwdForm.value = { oldPassword: '', newPassword: '' }
  } finally { changingPwd.value = false }
}

function toggleTag(tagId, checked) {
  if (checked) selectedTags.value.push(tagId)
  else selectedTags.value = selectedTags.value.filter(id => id !== tagId)
}

async function saveTags() {
  savingTags.value = true
  try {
    await post('/front/user/tags', selectedTags.value)
    ElMessage.success('标签已保存')
  } finally { savingTags.value = false }
}

async function submitBoard() {
  if (!boardContent.value.trim()) return
  boarding.value = true
  try {
    await post('/front/message-board', { content: boardContent.value })
    ElMessage.success('留言已发表')
    boardContent.value = ''
    const res = await get('/front/message-board', { pageNum: 1, pageSize: 30 })
    boardMessages.value = res.list || []
  } finally { boarding.value = false }
}

async function upgradeVipNow() {
  vipLoading.value = true
  try {
    await authStore.upgradeVip()
    ElMessage.success('VIP 已升级！')
    showVipDialog.value = false
  } finally { vipLoading.value = false }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString()
}
</script>

<style scoped>
.profile-tabs { margin-top: 16px; }
.profile-card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  max-width: 600px;
}
.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f3f5;
}
.profile-fields { margin-bottom: 16px; }
.profile-field {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #fafafa;
}
.label { width: 90px; color: #909399; font-size: 14px; flex-shrink: 0; }
.profile-buttons { display: flex; gap: 8px; }

/* Tags */
.tags-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  max-width: 600px;
}
.card-desc { color: #909399; font-size: 14px; margin-bottom: 12px; }
.tag-select-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-check { margin-right: 0 !important; }

/* Message Board */
.board-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  max-width: 600px;
}
.board-form { margin-bottom: 20px; }
.board-item {
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}
.board-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.board-time { color: #c0c4cc; font-size: 12px; }
.board-content { color: #303133; margin: 0 0 4px; }
.board-reply { background: #ecf5ff; padding: 6px 10px; border-radius: 6px; font-size: 13px; color: #409eff; margin: 4px 0 0; }

.vip-dialog-body { text-align: center; padding: 20px; }
.vip-dialog-body p { margin: 12px 0 0; font-size: 15px; }
.vip-sub { color: #909399; font-size: 13px; }
</style>