<template>
  <div class="page-container">
    <n-space justify="space-between" align="center" style="margin-bottom: 16px">
      <h2>{{ t('survey.mySurveys') }}</h2>
      <n-button type="primary" @click="router.push('/surveys/create')">{{ t('survey.createSurvey') }}</n-button>
    </n-space>

    <n-space style="margin-bottom: 16px">
      <n-input v-model:value="keyword" :placeholder="t('common.search')" clearable @clear="loadSurveys" @keyup.enter="loadSurveys" style="width: 300px" />
      <n-button @click="loadSurveys">{{ t('common.search') }}</n-button>
    </n-space>

    <n-spin :show="loading">
      <n-space vertical size="large">
        <n-card v-for="survey in surveys" :key="survey.id" hoverable>
          <template #header>
            <n-space align="center">
              <span>{{ survey.title }}</span>
              <n-tag :type="statusType(survey.status)" size="small">
                {{ statusLabel(survey.status) }}
              </n-tag>
              <n-tag v-if="survey.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
            </n-space>
          </template>
          <template #header-extra>
            <n-space>
              <n-button size="small" v-if="survey.status === 'DRAFT'" @click="publishSurvey(survey.id)">{{ t('survey.publish') }}</n-button>
              <n-button size="small" v-if="survey.status === 'PUBLISHED'" type="warning" @click="closeSurvey(survey.id)">{{ t('survey.close') }}</n-button>
              <n-button size="small" @click="router.push(`/surveys/${survey.id}/edit`)">{{ t('common.edit') }}</n-button>
              <n-button size="small" @click="router.push(`/surveys/${survey.id}/stats`)">{{ t('survey.statistics') }}</n-button>
              <n-button size="small" @click="router.push(`/surveys/${survey.id}/responses`)">{{ t('survey.responses') }}</n-button>
              <n-button size="small" type="error" @click="deleteSurvey(survey.id)">{{ t('common.delete') }}</n-button>
            </n-space>
          </template>
          <n-space vertical>
            <p v-if="survey.description">{{ survey.description }}</p>
            <n-space>
              <n-text depth="3">{{ t('survey.responseCount') }}: {{ survey.responseCount }}</n-text>
              <n-text depth="3">{{ t('survey.accessLevel') }}: {{ survey.accessLevel === 'PUBLIC' ? t('common.public') : t('common.private') }}</n-text>
              <n-text depth="3" v-if="survey.status === 'PUBLISHED'">
                {{ t('survey.fill') }}: <a :href="`/s/${survey.shareId}`" target="_blank">{{ baseUrl }}/s/{{ survey.shareId }}</a>
              </n-text>
            </n-space>
          </n-space>
        </n-card>

        <n-empty v-if="!loading && surveys.length === 0" :description="t('common.noData')" />

        <n-pagination
          v-if="totalPages > 1"
          v-model:page="page"
          :page-count="totalPages"
          @update:page="loadSurveys"
        />
      </n-space>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog } from 'naive-ui'
import { surveyApi } from '@/api/survey'
import type { SurveyDto } from '@/types'

const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const surveys = ref<SurveyDto[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const totalPages = ref(0)
const baseUrl = window.location.origin

function statusType(status: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'CLOSED') return 'error'
  return 'default'
}

function statusLabel(status: string) {
  if (status === 'PUBLISHED') return t('common.published')
  if (status === 'CLOSED') return t('common.closed')
  return t('common.draft')
}

async function loadSurveys() {
  loading.value = true
  try {
    const res = await surveyApi.getMy({ keyword: keyword.value || undefined, page: page.value - 1, size: 10 })
    surveys.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function publishSurvey(id: number) {
  try {
    await surveyApi.publish(id)
    message.success(t('survey.publish'))
    loadSurveys()
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  }
}

async function closeSurvey(id: number) {
  try {
    await surveyApi.close(id)
    message.success(t('survey.close'))
    loadSurveys()
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  }
}

function deleteSurvey(id: number) {
  dialog.warning({
    title: t('common.confirm'),
    content: t('common.confirm') + '?',
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        await surveyApi.delete(id)
        message.success(t('common.delete'))
        loadSurveys()
      } catch (e: any) {
        message.error(e?.response?.data?.message || 'Error')
      }
    },
  })
}

onMounted(loadSurveys)
</script>
