<template>
  <div class="page-container">
    <n-spin :show="loading">
      <template v-if="stats">
        <n-space justify="space-between" align="center" style="margin-bottom: 16px">
          <h2>{{ stats.title }} - {{ t('survey.statistics') }}</h2>
          <n-space>
            <n-button @click="exportExcel" :loading="exporting">{{ t('survey.export') }}</n-button>
            <n-button @click="router.back()">{{ t('common.back') }}</n-button>
          </n-space>
        </n-space>

        <n-card style="margin-bottom: 16px">
          <n-statistic :label="t('survey.responseCount')" :value="stats.totalResponses"/>
        </n-card>

        <n-space vertical size="large">
          <n-card v-for="qs in stats.questionStats" :key="qs.questionId" :title="qs.questionTitle" size="small">
            <template #header-extra>
              <n-tag size="small">{{ t(`survey.types.${qs.questionType}`) }}</n-tag>
            </template>

            <template v-if="qs.optionStats && qs.optionStats.length > 0">
              <div v-for="opt in qs.optionStats" :key="opt.optionId" style="margin-bottom: 12px">
                <n-space justify="space-between" style="margin-bottom: 4px">
                  <span>{{ opt.content }}</span>
                  <span>{{ opt.count }} ({{ opt.percentage.toFixed(1) }}%)</span>
                </n-space>
                <n-progress type="line" :percentage="opt.percentage" :show-indicator="false"
                            :color="getColor(opt.percentage)"/>
              </div>
            </template>

            <template v-else-if="qs.textAnswers && qs.textAnswers.length > 0">
              <n-list bordered>
                <n-list-item v-for="(text, idx) in qs.textAnswers" :key="idx">
                  {{ text }}
                </n-list-item>
              </n-list>
            </template>

            <n-empty v-else :description="t('common.noData')"/>
          </n-card>
        </n-space>
      </template>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import {surveyApi} from '@/api/survey'
import type {SurveyStatsDto} from '@/types'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()
const message = useMessage()

const loading = ref(true)
const exporting = ref(false)
const stats = ref<SurveyStatsDto | null>(null)

const colors = ['#18a058', '#2080f0', '#f0a020', '#d03050', '#8a2be2', '#ff6347']

function getColor(percentage: number): string {
  const idx = Math.floor(percentage / 20) % colors.length
  return colors[idx]
}

async function loadStats() {
  try {
    const res = await surveyApi.getStats(Number(route.params.id))
    stats.value = res.data.data
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  exporting.value = true
  try {
    const res = await surveyApi.exportExcel(Number(route.params.id))
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `survey_${route.params.id}_responses.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    message.error('Export failed')
  } finally {
    exporting.value = false
  }
}

onMounted(loadStats)
</script>
