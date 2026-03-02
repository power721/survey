<template>
  <div class="page-container">
    <h2>{{ t('vote.myVoteHistory') }}</h2>

    <n-spin :show="loading">
      <n-space vertical size="large" style="margin-top: 16px">
        <n-card v-for="record in records" :key="record.id">
          <n-space vertical>
            <n-space align="center">
              <n-text strong>{{ record.pollTitle }}</n-text>
              <n-tag size="small" type="info">{{ t('vote.voted') }}</n-tag>
            </n-space>
            <n-space>
              <n-text depth="3">{{ t('vote.selectedOption') }}:</n-text>
              <n-text>{{ record.optionTitle }}</n-text>
            </n-space>
            <n-space>
              <n-text depth="3">{{ t('common.time') }}:</n-text>
              <n-text>{{ new Date(record.createdAt).toLocaleString() }}</n-text>
            </n-space>
            <n-button text type="primary" size="small" @click="router.push(`/v/${record.pollShareId}`)">
              {{ t('vote.viewPollDetails') }} →
            </n-button>
          </n-space>
        </n-card>

        <n-empty v-if="!loading && records.length === 0" :description="t('vote.noVoteHistory')"/>

        <n-pagination
            v-if="totalPages > 1"
            v-model:page="page"
            :page-count="totalPages"
            @update:page="loadHistory"
        />
      </n-space>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {voteApi} from '@/api/vote'
import type {VoteRecordDto} from '@/types'

const router = useRouter()
const {t} = useI18n()

const records = ref<VoteRecordDto[]>([])
const loading = ref(false)
const page = ref(1)
const totalPages = ref(0)

async function loadHistory() {
  loading.value = true
  try {
    const res = await voteApi.getMyHistory({page: page.value - 1, size: 10})
    records.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)
</script>
