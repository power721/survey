import http from './http'
import type {ApiResponse, DashboardStatsDto} from '@/types'

export const dashboardApi = {
    getStats() {
        return http.get<ApiResponse<DashboardStatsDto>>('/admin/dashboard/stats')
    }
}
