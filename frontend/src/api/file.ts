import http from './http'
import type { ApiResponse } from '@/types'

export const fileApi = {
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<ApiResponse<{ url: string; name: string }>>('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
