import http from './http'
import type {
    ApiResponse,
    Page,
    VotePollCreateRequest,
    VotePollDto,
    VotePollListDto,
    VoteRecordDto,
    VoteSubmitRequest
} from '@/types'

export const voteApi = {
    create(data: VotePollCreateRequest) {
        return http.post<ApiResponse<VotePollDto>>('/votes', data)
    },
    update(id: number, data: VotePollCreateRequest) {
        return http.put<ApiResponse<VotePollDto>>(`/votes/${id}`, data)
    },
    getById(id: number) {
        return http.get<ApiResponse<VotePollDto>>(`/votes/${id}`)
    },
    getByShareId(shareId: string) {
        return http.get<ApiResponse<VotePollDto>>(`/votes/v/${shareId}`)
    },
    getMy(params?: { page?: number; size?: number }) {
        return http.get<ApiResponse<Page<VotePollListDto>>>('/votes/my', {params})
    },
    getPublic(params?: { page?: number; size?: number; username?: string }) {
        return http.get<ApiResponse<Page<VotePollListDto>>>('/votes/public', {params})
    },
    publish(id: number) {
        return http.post<ApiResponse<VotePollDto>>(`/votes/${id}/publish`)
    },
    close(id: number) {
        return http.post<ApiResponse<VotePollDto>>(`/votes/${id}/close`)
    },
    delete(id: number) {
        return http.delete<ApiResponse<void>>(`/votes/${id}`)
    },
    submit(shareId: string, data: VoteSubmitRequest) {
        return http.post<ApiResponse<VotePollDto>>(`/votes/v/${shareId}/submit`, data)
    },
    getRecords(id: number, params?: { page?: number; size?: number }) {
        return http.get<ApiResponse<Page<VoteRecordDto>>>(`/votes/${id}/records`, {params})
    },
}
