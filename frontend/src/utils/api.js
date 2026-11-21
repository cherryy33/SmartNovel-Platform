import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// Request interceptor: attach token
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// Response interceptor: unwrap RestResp, handle errors
service.interceptors.response.use(
  response => {
    const res = response.data
    // RestResp: { code: "00000", message: "...", data: ... }
    if (res.code && res.code !== '00000') {
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    // Return the data field directly, or full response if no RestResp wrapper
    return res.data !== undefined ? res.data : res
  },
  error => {
    const msg = error.code === 'ECONNABORTED'
      ? 'AI 生成耗时较长，请稍后再试'
      : (error.response?.data?.message || error.message || 'Network error')
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export const get = (url, params) => service.get(url, { params })
export const post = (url, data) => service.post(url, data)
export const put = (url, data) => service.put(url, data)
export const del = (url) => service.delete(url)

// AI 接口需要等待大模型生成，单独放宽超时（后端读超时 180s）
export const postAi = (url, data) => service.post(url, data, { timeout: 200000 })

/**
 * 流式调用 AI 接口（服务端纯文本分块输出）。
 * 每收到一段增量文本就回调 onChunk；传入 AbortSignal 可中途终止。
 */
export async function streamAi(url, data, onChunk, signal) {
  const token = localStorage.getItem('token')
  try {
    const resp = await fetch(`/api${url}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(data),
      signal
    })
    if (!resp.ok) {
      ElMessage.error(`AI 请求失败（${resp.status}）`)
      return
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      const text = decoder.decode(value, { stream: true })
      if (text) onChunk(text)
    }
    const tail = decoder.decode()
    if (tail) onChunk(tail)
  } catch (e) {
    if (e.name !== 'AbortError') ElMessage.error(e.message || 'AI 请求失败')
  }
}

export default service
