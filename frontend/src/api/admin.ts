import http from './http'
import type {ApiResponse} from '@/types'

export const adminApi = {
    getConfig() {
        return http.get<ApiResponse<Record<string, string>>>('/admin/config')
    },
    updateConfig(data: Record<string, string>) {
        return http.put<ApiResponse<void>>('/admin/config', data)
    },
}

export const configApi = {
    getPublicConfig() {
        return http.get<ApiResponse<Record<string, any>>>('/config/public')
    },
}
