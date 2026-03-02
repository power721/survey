<template>
  <div class="card-container">
    <n-card :title="t('dashboard.title')">
      <n-spin :show="loading">
        <template v-if="stats">
          <n-grid :cols="5" :x-gap="16" style="margin-bottom: 24px">
            <n-gi>
              <n-statistic :label="t('dashboard.totalUsers')" :value="stats.totalUsers">
                <template #prefix>
                  <n-icon :component="PeopleOutline"/>
                </template>
              </n-statistic>
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dashboard.totalSurveys')" :value="stats.totalSurveys">
                <template #prefix>
                  <n-icon :component="DocumentTextOutline"/>
                </template>
              </n-statistic>
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dashboard.totalResponses')" :value="stats.totalResponses">
                <template #prefix>
                  <n-icon :component="ChatboxOutline"/>
                </template>
              </n-statistic>
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dashboard.totalVotes')" :value="stats.totalVotes">
                <template #prefix>
                  <n-icon :component="CheckboxOutline"/>
                </template>
              </n-statistic>
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dashboard.totalVoteRecords')" :value="stats.totalVoteRecords">
                <template #prefix>
                  <n-icon :component="BarChartOutline"/>
                </template>
              </n-statistic>
            </n-gi>
          </n-grid>

          <n-divider>{{ t('dashboard.trends') }}</n-divider>

          <n-grid :cols="3" :x-gap="16">
            <n-gi>
              <n-card :title="t('dashboard.userTrend')" size="small">
                <div v-for="point in stats.userTrend.slice(-7)" :key="point.date" style="margin-bottom: 8px">
                  <n-space justify="space-between" align="center">
                    <n-text depth="3">{{ point.date }}</n-text>
                    <n-tag :type="point.count > 0 ? 'success' : 'default'" size="small">{{ point.count }}</n-tag>
                  </n-space>
                </div>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card :title="t('dashboard.surveyTrend')" size="small">
                <div v-for="point in stats.surveyTrend.slice(-7)" :key="point.date" style="margin-bottom: 8px">
                  <n-space justify="space-between" align="center">
                    <n-text depth="3">{{ point.date }}</n-text>
                    <n-tag :type="point.count > 0 ? 'success' : 'default'" size="small">{{ point.count }}</n-tag>
                  </n-space>
                </div>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card :title="t('dashboard.responseTrend')" size="small">
                <div v-for="point in stats.responseTrend.slice(-7)" :key="point.date" style="margin-bottom: 8px">
                  <n-space justify="space-between" align="center">
                    <n-text depth="3">{{ point.date }}</n-text>
                    <n-tag :type="point.count > 0 ? 'success' : 'default'" size="small">{{ point.count }}</n-tag>
                  </n-space>
                </div>
              </n-card>
            </n-gi>
          </n-grid>
        </template>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import {BarChartOutline, ChatboxOutline, CheckboxOutline, DocumentTextOutline, PeopleOutline} from '@vicons/ionicons5'
import {dashboardApi} from '@/api/dashboard'
import type {DashboardStatsDto} from '@/types'

const {t} = useI18n()
const message = useMessage()

const loading = ref(false)
const stats = ref<DashboardStatsDto | null>(null)

async function loadStats() {
  loading.value = true
  try {
    const res = await dashboardApi.getStats()
    stats.value = res.data.data
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error loading dashboard stats')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>
