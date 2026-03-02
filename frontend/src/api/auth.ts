import http from './http'
import type {ApiResponse, AuthResponse, UserProfile} from '@/types'

export const authApi = {
    login(data: { username: string; password: string }) {
        return http.post<ApiResponse<AuthResponse>>('/auth/login', data)
    },
    register(data: { username: string; password: string; email?: string; nickname?: string }) {
        return http.post<ApiResponse<AuthResponse>>('/auth/register', data)
    },
    getProfile() {
        return http.get<ApiResponse<UserProfile>>('/auth/profile')
    },
    updateProfile(data: {
        nickname?: string;
        email?: string;
        avatar?: string;
        oldPassword?: string;
        newPassword?: string
    }) {
        return http.put<ApiResponse<UserProfile>>('/auth/profile', data)
    },
    getOAuth2Url(provider: string) {
        return http.get<ApiResponse<string>>(`/auth/oauth2/${provider}`)
    },
    oauth2Callback(provider: string, code: string) {
        return http.post<ApiResponse<AuthResponse>>(`/auth/oauth2/${provider}/callback`, {code})
    },
}
