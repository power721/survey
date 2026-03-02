<template>
  <div class="page-container">
    <n-space justify="space-between" align="center" style="margin-bottom: 16px">
      <h2>{{ t('survey.responses') }}</h2>
      <n-button @click="router.back()">{{ t('common.back') }}</n-button>
    </n-space>

    <n-spin :show="loading">
      <n-space vertical size="large">
        <n-card v-for="(resp, ri) in responses" :key="resp.id" size="small">
          <template #header>
            <n-space align="center">
              <n-tag type="info" size="small">#{{ (page - 1) * 10 + ri + 1 }}</n-tag>
              <n-text v-if="resp.nickname || resp.username" depth="2">{{ resp.nickname || resp.username }}</n-text>
              <n-text depth="3">{{ formatTime(resp.createdAt) }}</n-text>
              <n-text depth="3">IP: {{ resp.ip }}</n-text>
            </n-space>
          </template>
          <n-descriptions label-placement="left" bordered :column="1" size="small">
            <n-descriptions-item v-for="answer in resp.answers" :key="answer.id" :label="answer.questionTitle">
              <template v-if="answer.selectedOptionContent">{{ answer.selectedOptionContent }}</template>
              <template v-else-if="answer.selectedOptionContents && answer.selectedOptionContents.length">
                {{ answer.selectedOptionContents.join('、') }}
              </template>
              <template v-else-if="answer.textValue && answer.textValue.startsWith('/api/files/')">
                <n-a :href="answer.textValue" target="_blank">{{ answer.textValue.split('/').pop() }}</n-a>
              </template>
              <template v-else>{{ answer.textValue || '-' }}</template>
            </n-descriptions-item>
          </n-descriptions>
        </n-card>

        <n-empty v-if="!loading && responses.length === 0" :description="t('common.noData')"/>

        <n-pagination
            v-if="totalPages > 1"
            v-model:page="page"
            :page-count="totalPages"
            @update:page="loadResponses"
        />
      </n-space>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {surveyApi} from '@/api/survey'
import type {SurveyResponseDto} from '@/types'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()

const loading = ref(true)
const responses = ref<SurveyResponseDto[]>([])
const page = ref(1)
const totalPages = ref(0)

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

async function loadResponses() {
  loading.value = true
  try {
    const res = await surveyApi.getResponses(Number(route.params.id), {page: page.value - 1, size: 10})
    responses.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadResponses)
</script>
