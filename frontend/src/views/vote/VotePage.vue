<template>
  <div class="card-container">
    <n-spin :show="loading">
      <template v-if="poll">
        <n-card :title="poll.title">
          <template #header-extra>
            <n-space>
              <n-tag :type="poll.voteType === 'SINGLE' ? 'info' : 'warning'" size="small">
                {{ poll.voteType === 'SINGLE' ? t('vote.single') : t('vote.multiple') }}
              </n-tag>
              <n-tag size="small" type="success">{{ t('vote.totalVotes') }}: {{ poll.totalVoteCount }}</n-tag>
            </n-space>
          </template>

          <p v-if="poll.description" style="margin-bottom: 16px; color: #666">{{ poll.description }}</p>

          <div v-if="poll.endTime" style="margin-bottom: 16px">
            <n-text depth="3">{{ t('vote.endTime') }}: {{ formatTime(poll.endTime) }}</n-text>
          </div>

          <!-- Voting form -->
          <template v-if="!hasVoted && !voted">
            <n-space vertical size="large" style="margin-bottom: 24px">
              <template v-if="poll.voteType === 'SINGLE'">
                <n-radio-group v-model:value="selectedSingle">
                  <n-space vertical>
                    <n-radio v-for="opt in poll.options" :key="opt.id" :value="opt.id">
                      <n-space align="center">
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 100px; max-height: 60px; border-radius: 4px" />
                        <span>{{ opt.content }}</span>
                        <n-text v-if="opt.maxVotes" depth="3" style="font-size: 12px">(max: {{ opt.maxVotes }})</n-text>
                      </n-space>
                    </n-radio>
                  </n-space>
                </n-radio-group>
              </template>

              <template v-else>
                <n-checkbox-group v-model:value="selectedMultiple">
                  <n-space vertical>
                    <n-checkbox v-for="opt in poll.options" :key="opt.id" :value="opt.id">
                      <n-space align="center">
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 100px; max-height: 60px; border-radius: 4px" />
                        <span>{{ opt.content }}</span>
                        <n-text v-if="opt.maxVotes" depth="3" style="font-size: 12px">(max: {{ opt.maxVotes }})</n-text>
                      </n-space>
                    </n-checkbox>
                  </n-space>
                </n-checkbox-group>
              </template>
            </n-space>

            <n-button type="primary" block size="large" :loading="submitting" @click="handleVote">
              {{ t('vote.submitVote') }}
            </n-button>
          </template>

          <!-- Already voted notice -->
          <n-alert v-if="hasVoted && !voted" type="info" style="margin-bottom: 16px">
            {{ t('vote.hasVoted') }}
          </n-alert>

          <!-- Results -->
          <template v-if="hasVoted || voted">
            <n-divider>{{ t('vote.results') }}</n-divider>
            <div v-for="opt in poll.options" :key="opt.id" style="margin-bottom: 16px">
              <n-space justify="space-between" style="margin-bottom: 4px">
                <n-space align="center">
                  <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 40px; max-height: 30px; border-radius: 2px" />
                  <span>{{ opt.content }}</span>
                </n-space>
                <span>{{ opt.voteCount }} {{ t('vote.votes') }} ({{ opt.percentage.toFixed(1) }}%)</span>
              </n-space>
              <n-progress type="line" :percentage="opt.percentage" :show-indicator="false"
                          :color="getBarColor(opt.percentage)" />
            </div>
          </template>
        </n-card>
      </template>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { voteApi } from '@/api/vote'
import type { VotePollDto } from '@/types'
import { Client } from '@stomp/stompjs'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const submitting = ref(false)
const poll = ref<VotePollDto | null>(null)
const voted = ref(false)
const selectedSingle = ref<number | null>(null)
const selectedMultiple = ref<number[]>([])
let stompClient: Client | null = null

const hasVoted = computed(() => poll.value?.hasVoted ?? false)

const colors = ['#18a058', '#2080f0', '#f0a020', '#d03050', '#8a2be2', '#ff6347']

function getBarColor(percentage: number): string {
  return colors[Math.floor(percentage / 20) % colors.length]
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

function getDeviceId(): string {
  let deviceId = localStorage.getItem('deviceId')
  if (!deviceId) {
    deviceId = 'dev_' + Math.random().toString(36).substring(2) + Date.now().toString(36)
    localStorage.setItem('deviceId', deviceId)
  }
  return deviceId
}

async function loadPoll() {
  try {
    const res = await voteApi.getByShareId(route.params.shareId as string)
    poll.value = res.data.data
    connectWebSocket()
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error loading vote')
  } finally {
    loading.value = false
  }
}

function connectWebSocket() {
  if (!poll.value) return
  try {
    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/websocket`
    stompClient = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient?.subscribe(`/topic/vote/${poll.value?.shareId}`, (msg) => {
          const updated = JSON.parse(msg.body) as VotePollDto
          if (poll.value) {
            poll.value.options = updated.options
            poll.value.totalVoteCount = updated.totalVoteCount
          }
        })
      },
    })
    stompClient.activate()
  } catch (e) {
    console.warn('WebSocket connection failed:', e)
  }
}

async function handleVote() {
  if (!poll.value) return

  const optionIds: number[] = poll.value.voteType === 'SINGLE'
    ? (selectedSingle.value ? [selectedSingle.value] : [])
    : selectedMultiple.value

  if (optionIds.length === 0) {
    message.warning(t('survey.options'))
    return
  }

  submitting.value = true
  try {
    const res = await voteApi.submit(route.params.shareId as string, {
      optionIds,
      deviceId: getDeviceId(),
    })
    poll.value = res.data.data
    voted.value = true
    message.success(t('vote.voteSuccess'))
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    submitting.value = false
  }
}

onMounted(loadPoll)

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>
