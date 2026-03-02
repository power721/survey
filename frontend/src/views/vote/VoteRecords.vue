<template>
  <div class="page-container">
    <n-space justify="space-between" align="center" style="margin-bottom: 16px">
      <n-space align="center">
        <n-button text @click="router.back()">← {{ t('common.back') }}</n-button>
        <h2>{{ t('vote.records') }}</h2>
        <span v-if="pollTitle" style="color: #666">- {{ pollTitle }}</span>
      </n-space>
    </n-space>

    <n-spin :show="loading">
      <n-data-table
          :columns="columns"
          :data="records"
          :bordered="false"
      />
      <n-space justify="end" style="margin-top: 16px">
        <n-pagination
            v-if="totalPages > 1"
            v-model:page="page"
            :page-count="totalPages"
            @update:page="loadRecords"
        />
      </n-space>
      <n-empty v-if="!loading && records.length === 0" :description="t('common.noData')"/>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {voteApi} from '@/api/vote'
import type {VoteRecordDto} from '@/types'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()

const pollId = Number(route.params.id)
const pollTitle = ref('')
const pollVoteType = ref('')
const records = ref<VoteRecordDto[]>([])
const loading = ref(false)
const page = ref(1)
const totalPages = ref(0)

const columns = computed(() => {
  const cols: any[] = [
    {
      title: '#',
      key: 'index',
      width: 60,
      render: (_: VoteRecordDto, index: number) => (page.value - 1) * 20 + index + 1
    },
    {
      title: t('vote.voter'),
      key: 'voter',
      render: (row: VoteRecordDto) => row.nickname || row.username || t('vote.anonymous')
    },
    {title: t('vote.votedOption'), key: 'optionTitle'},
  ]
  if (pollVoteType.value === 'SCORED') {
    cols.push({title: t('vote.votes'), key: 'voteCount'})
  }
  cols.push(
      {title: 'IP', key: 'ip'},
      {
        title: t('vote.votedAt'),
        key: 'createdAt',
        render: (row: VoteRecordDto) => new Date(row.createdAt).toLocaleString()
      },
  )
  return cols
})

async function loadRecords() {
  loading.value = true
  try {
    const res = await voteApi.getRecords(pollId, {page: page.value - 1, size: 20})
    records.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadPollInfo() {
  try {
    const res = await voteApi.getById(pollId)
    pollTitle.value = res.data.data.title
    pollVoteType.value = res.data.data.voteType
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadPollInfo()
  loadRecords()
})
</script>
