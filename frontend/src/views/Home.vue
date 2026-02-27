<template>
  <div class="page-container">
    <n-space vertical size="large">
      <n-card>
        <n-space vertical align="center" size="large" style="text-align: center; padding: 40px 0">
          <h1 style="font-size: 36px; font-weight: 700">{{ t('common.appName') }}</h1>
          <p style="font-size: 16px; color: #666; max-width: 600px">
            {{ locale === 'zh-CN' ? '轻松创建问卷调查和投票，收集数据，分析结果' : 'Easily create surveys and votes, collect data, analyze results' }}
          </p>
          <n-space>
            <n-button type="primary" size="large" @click="router.push('/surveys/public')">
              {{ t('survey.publicSurveys') }}
            </n-button>
            <n-button size="large" @click="router.push('/votes/public')">
              {{ t('vote.publicVotes') }}
            </n-button>
          </n-space>
        </n-space>
      </n-card>

      <n-grid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" :item-responsive="true">
        <n-gi span="2 m:1">
          <n-card :title="t('survey.title')" hoverable>
            <n-space vertical>
              <p>{{ locale === 'zh-CN' ? '支持单选、多选、填空、文本、数字、评分、日期等多种题型' : 'Supports single choice, multiple choice, text, number, rating, date and more' }}</p>
              <n-button type="primary" @click="router.push(authStore.isLoggedIn ? '/surveys/create' : '/login')">
                {{ t('survey.createSurvey') }}
              </n-button>
            </n-space>
          </n-card>
        </n-gi>
        <n-gi span="2 m:1">
          <n-card :title="t('vote.title')" hoverable>
            <n-space vertical>
              <p>{{ locale === 'zh-CN' ? '支持单选/多选投票，防刷票机制，实时结果展示' : 'Supports single/multiple choice voting, anti-fraud, real-time results' }}</p>
              <n-button type="primary" @click="router.push(authStore.isLoggedIn ? '/votes/create' : '/login')">
                {{ t('vote.createVote') }}
              </n-button>
            </n-space>
          </n-card>
        </n-gi>
      </n-grid>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const { t, locale } = useI18n()
const authStore = useAuthStore()
</script>
