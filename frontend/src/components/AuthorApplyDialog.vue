<template>
  <el-dialog
    v-model="visible"
    title="申请成为作者"
    width="440px"
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <!-- 审核中 -->
    <div v-if="status === 'pending'" class="apply-state">
      <el-icon :size="44" color="#e6a23c"><Clock /></el-icon>
      <p class="state-title">正在等待管理员审核</p>
      <p class="state-sub">你的申请已提交，审核通过后即可进入作家工作台创作。</p>
      <el-tag type="warning" size="small">申请笔名：{{ info.penName || '—' }}</el-tag>
    </div>

    <!-- 已通过 -->
    <div v-else-if="status === 'approved'" class="apply-state">
      <el-icon :size="44" color="#67c23a"><CircleCheckFilled /></el-icon>
      <p class="state-title">恭喜，你已成为作家！</p>
      <p class="state-sub">笔名：{{ info.penName || '—' }}</p>
      <el-button type="success" @click="goWorkspace">前往作家工作台</el-button>
    </div>

    <!-- 未申请 / 被驳回：展示表单 -->
    <div v-else>
      <el-alert
        v-if="status === 'rejected'"
        type="error"
        :closable="false"
        show-icon
        class="reject-alert"
        :title="`上次申请被驳回：${info.reviewComment || '未填写原因'}`"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="笔名" prop="penName">
          <el-input v-model="form.penName" placeholder="请输入作家笔名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="创作简介" prop="intro">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="4"
            placeholder="介绍一下你的创作方向、擅长题材等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <template v-if="status === 'form' || status === 'rejected'">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ status === 'rejected' ? '重新提交申请' : '提交申请' }}
        </el-button>
      </template>
      <template v-else-if="status === 'pending'">
        <el-button @click="visible = false">我知道了</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { get, post } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { Clock, CircleCheckFilled } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const info = ref({})
// form | pending | approved | rejected
const status = ref('form')

const form = reactive({ penName: '', intro: '' })

const rules = {
  penName: [
    { required: true, message: '请输入笔名', trigger: 'blur' },
    { max: 50, message: '笔名最长 50 个字', trigger: 'blur' }
  ],
  intro: [{ max: 500, message: '简介最长 500 个字', trigger: 'blur' }]
}

/** 打开弹窗并加载当前申请状态 */
async function open() {
  visible.value = true
  loading.value = true
  info.value = {}
  form.penName = ''
  form.intro = ''
  status.value = 'form'
  try {
    const data = await get('/author/apply/status')
    if (data) {
      info.value = data
      applyStatusToView(data)
    }
  } catch (e) {
    status.value = 'form'
  } finally {
    loading.value = false
  }
}

function applyStatusToView(data) {
  const st = Number(data.status)
  if (st === 0) {
    status.value = 'pending'
  } else if (st === 1) {
    status.value = 'approved'
  } else if (st === 2) {
    status.value = 'rejected'
    form.penName = data.penName || ''
    form.intro = data.intro || ''
  } else {
    status.value = 'form'
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await post('/author/apply', { penName: form.penName, intro: form.intro })
    ElMessage.success('申请已提交，请等待管理员审核')
    status.value = 'pending'
    info.value = { ...info.value, penName: form.penName }
  } catch (e) {
    // api.js 已提示具体错误
  } finally {
    submitting.value = false
  }
}

function goWorkspace() {
  visible.value = false
  router.push('/author')
}

function onClosed() {
  loading.value = false
  submitting.value = false
}

defineExpose({ open, status })
</script>

<style scoped>
.apply-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 4px;
  text-align: center;
}
.state-title { margin: 4px 0 0; font-size: 16px; font-weight: 600; color: #303133; }
.state-sub { margin: 0; font-size: 13px; color: #909399; }
.reject-alert { margin-bottom: 14px; }
</style>
