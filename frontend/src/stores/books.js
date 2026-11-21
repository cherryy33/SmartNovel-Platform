import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get, post, del } from '@/utils/api'

export const useBooksStore = defineStore('books', () => {
  const bookshelf = ref([])
  const history = ref([])

  async function fetchBookshelf() {
    const data = await get('/front/user/bookshelf')
    bookshelf.value = Array.isArray(data) ? data : (data?.records || [])
    return bookshelf.value
  }

  async function addToBookshelf(bookId) {
    await post('/front/user/bookshelf', { bookId })
    await fetchBookshelf()
  }

  async function removeFromBookshelf(bookId) {
    await del(`/front/user/bookshelf/${bookId}`)
    await fetchBookshelf()
  }

  async function toggleBookshelf(bookId) {
    const exists = bookshelf.value.some(b => (b.bookId || b.id) === bookId)
    if (exists) {
      await removeFromBookshelf(bookId)
    } else {
      await addToBookshelf(bookId)
    }
  }

  function isInBookshelf(bookId) {
    return bookshelf.value.some(b => (b.bookId || b.id) === bookId)
  }

  return {
    bookshelf, history,
    fetchBookshelf, addToBookshelf, removeFromBookshelf, toggleBookshelf, isInBookshelf
  }
})
