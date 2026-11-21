<template>
  <div class="admin-community">
    <div class="page-header"><h2>社区管理</h2></div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="帖子" name="posts">
        <el-table :data="posts" border stripe style="width:100%" empty-text="暂无数据">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="nickName" label="用户" width="100" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="likeCount" label="点赞" width="70" />
          <el-table-column prop="replyCount" label="回复" width="70" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="viewReplies(row)">查看回复</el-button>
              <el-popconfirm title="确认删除？" @confirm="delPost(row.id)">
                <template #reference><el-button size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="postTotal > 0"
          layout="prev, next"
          :total="postTotal"
          :page-size="pageSize"
          @current-change="loadPosts"
        />
      </el-tab-pane>

      <el-tab-pane label="回复列表" name="replies">
        <div v-if="!currentPost" class="hint-text">← 点击帖子的"查看回复"来浏览回复</div>
        <div v-else>
          <h4 class="replies-title">帖子回复：{{ currentPost.title }}</h4>
          <el-table :data="replies" border stripe style="width:100%" empty-text="暂无数据">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="nickName" label="用户" width="100" />
            <el-table-column prop="content" label="内容" />
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-popconfirm title="确认删除？" @confirm="delReply(row.id)">
                  <template #reference><el-button size="small" type="danger">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, del } from '@/utils/api'
import { ElMessage } from 'element-plus'

const activeTab = ref('posts')
const posts = ref([])
const postTotal = ref(0)
const pageSize = 20

const currentPost = ref(null)
const replies = ref([])
const replyTotal = ref(0)

onMounted(() => loadPosts(1))

async function loadPosts(pageNum = 1) {
  const res = await get('/admin/community/posts', { pageNum, pageSize })
  posts.value = res.list || []
  postTotal.value = res.total || 0
}

async function delPost(postId) {
  await del(`/admin/community/posts/${postId}`)
  ElMessage.success('已删除')
  loadPosts()
}

async function viewReplies(post) {
  currentPost.value = post
  activeTab.value = 'replies'
  // Get post replies via the public API
  try {
    const res = await get(`/front/community/posts/${post.id}/replies`, { pageNum: 1, pageSize: 50 })
    replies.value = res.list || []
    replyTotal.value = res.total || 0
  } catch (e) {
    replies.value = []
  }
}

async function delReply(replyId) {
  await del(`/admin/community/replies/${replyId}`)
  ElMessage.success('已删除')
  if (currentPost.value) viewReplies(currentPost.value)
}

function onTabChange(tab) {
  if (tab === 'posts' && !posts.value.length) loadPosts(1)
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.hint-text { text-align: center; padding: 40px 0; color: #909399; }
.replies-title { margin: 0 0 12px; font-size: 15px; color: #409eff; }
</style>