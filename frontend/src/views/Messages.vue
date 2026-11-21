<template>
  <div class="messages-page">
    <h2>私信</h2>
    <div class="messages-layout" v-if="authStore.isLoggedIn">
      <!-- 联系人列表 -->
      <div class="contacts-panel">
        <div class="contacts-header">
          <h4>联系人</h4>
          <span v-if="unreadCount" class="unread-badge">{{ unreadCount }}</span>
        </div>
        <div v-for="contact in contacts" :key="contact.id || contact.senderId || contact.receiverId"
             class="contact-item" :class="{ active: activeContactId === getContactId(contact) }"
             @click="openConversation(getContactId(contact))">
          <el-avatar :size="32" :icon="UserFilled" />
          <div class="contact-info">
            <span class="contact-name">{{ getContactName(contact) }}</span>
            <span class="contact-preview">{{ contact.lastContent || contact.content || '' }}</span>
          </div>
          <span v-if="!contact.isRead && contact.senderId !== authStore.user?.id" class="contact-unread-dot" />
        </div>
        <p v-if="contacts.length === 0" class="empty-hint">暂无消息</p>
      </div>

      <!-- 对话区域 -->
      <div class="conversation-panel" v-if="activeContactId">
        <div class="conversation-header">
          <h4>{{ activeContactName }}</h4>
        </div>
        <div class="conversation-messages" ref="msgListRef">
          <div v-for="msg in conversation" :key="msg.id" class="conv-msg"
               :class="msg.senderId === authStore.user?.id ? 'sent' : 'received'">
            <div class="conv-msg-bubble">
              <p>{{ msg.content }}</p>
              <span class="conv-msg-time">{{ formatTime(msg.createTime) }}</span>
            </div>
          </div>
          <p v-if="conversation.length === 0" class="empty-hint">暂无对话内容</p>
        </div>
        <div class="conversation-input">
          <el-input v-model="newMessage" placeholder="输入消息..." size="small"
                    @keyup.enter="sendMsg" />
          <el-button type="primary" size="small" @click="sendMsg" :loading="sending">发送</el-button>
        </div>
      </div>

      <!-- 未选择对话 -->
      <div class="conversation-panel empty" v-else>
        <p class="empty-hint">选择联系人开始聊天</p>
      </div>
    </div>

    <div v-else class="empty-hint">
      <router-link to="/login">登录</router-link> 查看私信
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { get, post, put } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const authStore = useAuthStore()

const contacts = ref([])
const unreadCount = ref(0)
const activeContactId = ref(null)
const activeContactName = ref('')
const conversation = ref([])
const newMessage = ref('')
const sending = ref(false)
const msgListRef = ref(null)

onMounted(async () => {
  await loadContacts()
  try {
    const res = await get('/front/messages/unread-count')
    unreadCount.value = res?.count || 0
  } catch (e) { /* silent */ }
})

async function loadContacts() {
  try {
    contacts.value = await get('/front/messages/contacts') || []
  } catch (e) { /* silent */ }
}

function getContactId(contact) {
  const uid = authStore.user?.id
  return contact.senderId === uid ? contact.receiverId : contact.senderId
}

function getContactName(contact) {
  const uid = authStore.user?.id
  if (contact.senderId === uid) return contact.receiverNickName || contact.receiverUsername || `User ${contact.receiverId}`
  return contact.senderNickName || contact.senderUsername || `User ${contact.senderId}`
}

async function openConversation(contactId) {
  activeContactId.value = contactId
  activeContactName.value = contacts.value.find(c => getContactId(c) === contactId)
  try {
    const res = await get(`/front/messages/${contactId}`, { pageNum: 1, pageSize: 50 })
    conversation.value = (res.list || res || []).reverse()
    await nextTick()
    scrollToBottom()
  } catch (e) { conversation.value = [] }
}

async function sendMsg() {
  if (!newMessage.value.trim() || !activeContactId.value) return
  sending.value = true
  try {
    await post(`/front/messages/send/${activeContactId.value}`, { content: newMessage.value })
    conversation.value.push({
      senderId: authStore.user?.id,
      content: newMessage.value,
      createTime: new Date().toISOString()
    })
    newMessage.value = ''
    await nextTick()
    scrollToBottom()
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  if (msgListRef.value) {
    msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString()
}
</script>

<style scoped>
.messages-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 180px);
  min-height: 400px;
}
.contacts-panel {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.contacts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.contacts-header h4 { margin: 0; font-size: 15px; }
.unread-badge { background: #f56c6c; color: #fff; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.contact-item:hover { background: #f5f7fa; }
.contact-item.active { background: #ecf5ff; }
.contact-info { flex: 1; min-width: 0; }
.contact-name { font-size: 14px; font-weight: 500; display: block; }
.contact-preview { font-size: 12px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
.contact-unread-dot { width: 8px; height: 8px; border-radius: 50%; background: #409eff; flex-shrink: 0; }
.conversation-panel {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
}
.conversation-panel.empty { justify-content: center; align-items: center; }
.conversation-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.conversation-header h4 { margin: 0; font-size: 15px; }
.conversation-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.conv-msg { display: flex; max-width: 70%; }
.conv-msg.sent { align-self: flex-end; }
.conv-msg.received { align-self: flex-start; }
.conv-msg-bubble {
  padding: 8px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.4;
}
.conv-msg.sent .conv-msg-bubble { background: #409eff; color: #fff; border-bottom-right-radius: 4px; }
.conv-msg.received .conv-msg-bubble { background: #f0f2f5; color: #303133; border-bottom-left-radius: 4px; }
.conv-msg-bubble p { margin: 0; }
.conv-msg-time { font-size: 10px; opacity: 0.6; margin-top: 2px; display: block; }
.conversation-input {
  display: flex;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid #ebeef5;
}
.empty-hint { text-align: center; padding: 40px 0; color: #909399; }
.empty-hint a { color: #409eff; }
</style>