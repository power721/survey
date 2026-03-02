import http from './http'
import type {
    ApiResponse,
    Page,
    SurveyCreateRequest,
    SurveyDto,
    SurveyResponseDto,
    SurveyStatsDto,
    SurveySubmitRequest
} from '@/types'

export const surveyApi = {
    create(data: SurveyCreateRequest) {
        return http.post<ApiResponse<SurveyDto>>('/surveys', data)
    },
    update(id: number, data: SurveyCreateRequest) {
        return http.put<ApiResponse<SurveyDto>>(`/surveys/${id}`, data)
    },
    getById(id: number) {
        return http.get<ApiResponse<SurveyDto>>(`/surveys/${id}`)
    },
    getByShareId(shareId: string) {
        return http.get<ApiResponse<SurveyDto>>(`/surveys/s/${shareId}`)
    },
    getMy(params?: { keyword?: string; page?: number; size?: number }) {
        return http.get<ApiResponse<Page<SurveyDto>>>('/surveys/my', {params})
    },
    getPublic(params?: { page?: number; size?: number; username?: string }) {
        return http.get<ApiResponse<Page<SurveyDto>>>('/surveys/public', {params})
    },
    getTemplates(params?: { page?: number; size?: number }) {
        return http.get<ApiResponse<Page<SurveyDto>>>('/surveys/templates', {params})
    },
    publish(id: number) {
        return http.post<ApiResponse<SurveyDto>>(`/surveys/${id}/publish`)
    },
    close(id: number) {
        return http.post<ApiResponse<SurveyDto>>(`/surveys/${id}/close`)
    },
    delete(id: number) {
        return http.delete<ApiResponse<void>>(`/surveys/${id}`)
    },
    getMyResponse(shareId: string) {
        return http.get<ApiResponse<SurveyResponseDto | null>>(`/surveys/s/${shareId}/my-response`)
    },
    submit(shareId: string, data: SurveySubmitRequest) {
        return http.post<ApiResponse<SurveyResponseDto>>(`/surveys/s/${shareId}/submit`, data)
    },
    getResponses(id: number, params?: { page?: number; size?: number }) {
        return http.get<ApiResponse<Page<SurveyResponseDto>>>(`/surveys/${id}/responses`, {params})
    },
    getStats(id: number) {
        return http.get<ApiResponse<SurveyStatsDto>>(`/surveys/${id}/stats`)
    },
    exportExcel(id: number) {
        return http.get(`/surveys/${id}/export`, {responseType: 'blob'})
    },
}
