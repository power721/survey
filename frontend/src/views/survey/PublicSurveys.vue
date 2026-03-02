<template>
  <div class="page-container">
    <h2 style="margin-bottom: 16px">{{ t('survey.publicSurveys') }}</h2>

    <n-spin :show="loading">
      <n-grid :cols="1" :x-gap="16" :y-gap="16" responsive="screen" :item-responsive="true">
        <n-gi v-for="survey in surveys" :key="survey.id" span="1">
          <n-card hoverable style="cursor: pointer" @click="router.push(`/s/${survey.shareId}`)">
            <template #header>{{ survey.title }}</template>
            <template #header-extra>
              <n-space>
                <n-tag v-if="survey.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
                <n-tag size="small" type="info">{{ survey.responseCount }} {{ t('survey.responses') }}</n-tag>
                <n-text v-if="survey.startTime && new Date(survey.startTime).getTime() > Date.now()" depth="3">{{ t('common.startTime') }}: {{ new Date(survey.startTime).toLocaleString() }}</n-text>
                <n-text depth="3">{{ survey.creatorName }}</n-text>
              </n-space>
            </template>
            <p v-if="survey.description">{{ survey.description }}</p>
          </n-card>
        </n-gi>
      </n-grid>

      <n-empty v-if="!loading && surveys.length === 0" :description="t('common.noData')" style="margin-top: 40px" />

      <n-pagination
        v-if="totalPages > 1"
        v-model:page="page"
        :page-count="totalPages"
        @update:page="loadSurveys"
        style="margin-top: 16px; justify-content: center"
      />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { surveyApi } from '@/api/survey'
import type { SurveyDto } from '@/types'

const router = useRouter()
const { t } = useI18n()

const surveys = ref<SurveyDto[]>([])
const loading = ref(true)
const page = ref(1)
const totalPages = ref(0)

async function loadSurveys() {
  loading.value = true
  try {
    const res = await surveyApi.getPublic({ page: page.value - 1, size: 12 })
    surveys.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadSurveys)
</script>
