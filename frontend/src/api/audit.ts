import http from './http'
import type {ApiResponse, Page} from '@/types'

export interface AuditLog {
    id: number
    userId: number | null
    username: string
    action: string
    entityType: string | null
    entityId: number | null
    details: string | null
    ipAddress: string | null
    createdAt: string
}

export const auditApi = {
    getLogs(params?: { action?: string; entityType?: string; username?: string; page?: number; size?: number }) {
        return http.get<ApiResponse<Page<AuditLog>>>('/admin/audit-logs', {params})
    }
}
