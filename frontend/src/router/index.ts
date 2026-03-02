import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from '@/stores/auth'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('@/views/Home.vue'),
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/auth/Login.vue'),
            meta: {guest: true},
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/auth/Register.vue'),
            meta: {guest: true},
        },
        // Survey routes
        {
            path: '/surveys',
            name: 'MySurveys',
            component: () => import('@/views/survey/MySurveys.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/surveys/create',
            name: 'CreateSurvey',
            component: () => import('@/views/survey/SurveyEditor.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/surveys/:id/edit',
            name: 'EditSurvey',
            component: () => import('@/views/survey/SurveyEditor.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/surveys/:id/stats',
            name: 'SurveyStats',
            component: () => import('@/views/survey/SurveyStats.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/surveys/:id/responses',
            name: 'SurveyResponses',
            component: () => import('@/views/survey/SurveyResponses.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/s/:shareId',
            name: 'FillSurvey',
            component: () => import('@/views/survey/FillSurvey.vue'),
        },
        {
            path: '/surveys/public',
            name: 'PublicSurveys',
            component: () => import('@/views/survey/PublicSurveys.vue'),
        },
        // Vote routes
        {
            path: '/votes',
            name: 'MyVotes',
            component: () => import('@/views/vote/MyVotes.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/votes/create',
            name: 'CreateVote',
            component: () => import('@/views/vote/VoteEditor.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/votes/:id/edit',
            name: 'EditVote',
            component: () => import('@/views/vote/VoteEditor.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/votes/:id/records',
            name: 'VoteRecords',
            component: () => import('@/views/vote/VoteRecords.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/v/:shareId',
            name: 'VotePage',
            component: () => import('@/views/vote/VotePage.vue'),
        },
        {
            path: '/votes/public',
            name: 'PublicVotes',
            component: () => import('@/views/vote/PublicVotes.vue'),
        },
        {
            path: '/admin/dashboard',
            name: 'Dashboard',
            component: () => import('@/views/admin/Dashboard.vue'),
            meta: {requiresAuth: true, requiresAdmin: true},
        },
        {
            path: '/admin/config',
            name: 'SystemConfig',
            component: () => import('@/views/admin/SystemConfig.vue'),
            meta: {requiresAuth: true, requiresAdmin: true},
        },
        {
            path: '/oauth2/callback/:provider',
            name: 'OAuth2Callback',
            component: () => import('@/views/auth/OAuth2Callback.vue'),
            meta: {guest: true},
        },
        {
            path: '/profile',
            name: 'Profile',
            component: () => import('@/views/auth/ProfileEdit.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: '/user/:username',
            name: 'UserPage',
            component: () => import('@/views/user/UserPage.vue'),
        },
    ],
})

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()
    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        next({name: 'Login', query: {redirect: to.fullPath}})
    } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
        next({name: 'Home'})
    } else if (to.meta.guest && authStore.isLoggedIn) {
        next({name: 'Home'})
    } else {
        next()
    }
})

export default router
