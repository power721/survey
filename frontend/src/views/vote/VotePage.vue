<template>
  <div class="card-container" ref="containerRef">
    <n-spin :show="loading">
      <template v-if="poll">
        <img v-if="poll.logoUrl" :src="poll.logoUrl" :alt="poll.title"
             style="width: 100%; display: block; border-radius: 8px 8px 0 0"/>
        <n-card :title="poll.title" :style="poll.logoUrl ? { borderTopLeftRadius: 0, borderTopRightRadius: 0 } : {}">
          <template #header-extra>
            <n-space>
              <template v-if="!isNotStarted">
                <n-tag :type="voteTypeTagType" size="small">{{ voteTypeLabel }}</n-tag>
                <n-tag v-if="poll.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
                <n-tag size="small" type="success">{{ t('vote.totalVotes') }}: {{ poll.totalVoteCount }}</n-tag>
                <n-tag v-if="isExpired" type="error" size="small">{{ t('vote.expired') }}</n-tag>
              </template>
            </n-space>
          </template>

          <n-space style="margin-bottom: 16px">
            <n-text v-if="poll.creatorName" depth="3">{{ t('common.creator') }}: {{ poll.creatorName }}</n-text>
            <n-text v-if="poll.startTime" depth="3">{{ t('common.startTime') }}: {{ new Date(poll.startTime).toLocaleString() }}</n-text>
            <n-text v-if="poll.endTime" depth="3">{{ t('vote.endTime') }}: {{ formatTime(poll.endTime) }}</n-text>
          </n-space>

          <n-alert v-if="countdownLabel && countdownText" :type="isNotStarted ? 'info' : 'warning'"
                   style="margin-bottom: 16px">
            {{ countdownLabel }}: {{ countdownText }}
          </n-alert>

          <p v-if="poll.description" style="margin-bottom: 16px; color: #666">{{ poll.description }}</p>

          <n-alert v-if="isNotStarted" type="warning" style="margin-bottom: 16px">
            {{ t('vote.notStarted') }}
          </n-alert>

          <n-alert v-if="isExpired && !hasVoted && !voted" type="warning" style="margin-bottom: 16px">
            {{ t('vote.votingClosed') }}
          </n-alert>

          <n-alert v-if="loginRequired" type="warning" style="margin-bottom: 16px">
            {{ t('vote.loginRequired') }}
            <n-button text type="primary" @click="router.push('/login')" style="margin-left: 8px">{{ t('common.login') }}</n-button>
          </n-alert>

          <!-- Voting form -->
          <template v-if="!hasVoted && !voted && !isExpired && !isNotStarted && !loginRequired">
            <n-space vertical size="large" style="margin-bottom: 24px">
              <!-- SINGLE: radio buttons -->
              <template v-if="poll.voteType === 'SINGLE'">
                <n-radio-group v-model:value="selectedSingle">
                  <n-space vertical>
                    <n-radio v-for="(opt, oi) in poll.options" :key="opt.id" :value="opt.id">
                      <div>
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" class="vote-option-image" @click.prevent.stop="openPreview(opt.imageUrl)" />
                        <span>{{ oi + 1 }}. {{ opt.title }}</span>
                        <n-text v-if="opt.content" depth="3" style="font-size: 12px; display: block">{{ opt.content }}</n-text>
                      </div>
                    </n-radio>
                  </n-space>
                </n-radio-group>
              </template>

              <!-- MULTIPLE: checkboxes -->
              <template v-else-if="poll.voteType === 'MULTIPLE'">
                <n-checkbox-group :value="selectedMultiple" @update:value="onMultipleChange">
                  <n-space vertical>
                    <n-checkbox v-for="(opt, oi) in poll.options" :key="opt.id" :value="opt.id"
                      :disabled="multipleReachedMax && !selectedMultiple.includes(opt.id)">
                      <div>
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" class="vote-option-image" @click.prevent.stop="openPreview(opt.imageUrl)" />
                        <span>{{ oi + 1 }}. {{ opt.title }}</span>
                        <n-text v-if="opt.content" depth="3" style="font-size: 12px; display: block">{{ opt.content }}</n-text>
                      </div>
                    </n-checkbox>
                  </n-space>
                </n-checkbox-group>
              </template>

              <!-- SCORED: input-number per option -->
              <template v-else-if="poll.voteType === 'SCORED'">
                <div v-for="(opt, oi) in poll.options" :key="opt.id" style="margin-bottom: 12px">
                  <n-space align="center" justify="space-between">
                    <n-space vertical :size="4">
                      <img v-if="opt.imageUrl" :src="opt.imageUrl" class="vote-option-image" @click="openPreview(opt.imageUrl)" />
                      <span>{{ oi + 1 }}. {{ opt.title }}</span>
                      <n-text v-if="opt.content" depth="3" style="font-size: 12px">{{ opt.content }}</n-text>
                    </n-space>
                    <n-input-number
                      :value="scoredVotes[opt.id] || 0"
                      :min="0"
                      :max="perOptionMax"
                      size="small"
                      style="width: 120px"
                      @update:value="(v: number | null) => setScoredVote(opt.id, v ?? 0)"
                    />
                  </n-space>
                </div>
              </template>
            </n-space>

            <n-space vertical align="center" style="margin-bottom: 12px">
              <n-text v-if="poll.voteType === 'MULTIPLE' && poll.maxOptions" depth="3">
                {{ t('vote.maxOptions') }}: {{ poll.maxOptions }}
              </n-text>
              <n-text v-if="poll.voteType === 'SCORED' && poll.maxTotalVotes" depth="3">
                {{ t('vote.remainingVotes') }}: {{ remainingVotes }}
              </n-text>
              <n-text v-if="poll.voteType === 'SCORED' && poll.maxVotesPerOption" depth="3">
                {{ t('vote.maxVotesPerOption') }}: {{ poll.maxVotesPerOption }}
              </n-text>
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
          <template v-if="hasVoted || voted || isExpired">
            <n-divider>{{ t('vote.results') }}</n-divider>
            <div v-for="(opt, oi) in poll.options" :key="opt.id" style="margin-bottom: 16px">
              <img v-if="opt.imageUrl" :src="opt.imageUrl" class="vote-option-image" style="margin-bottom: 6px" @click="openPreview(opt.imageUrl)" />
              <n-space justify="space-between" style="margin-bottom: 4px">
                <span>{{ oi + 1 }}. {{ opt.title }}</span>
                <span>{{ opt.voteCount }} {{ t('vote.votes') }} ({{ opt.percentage.toFixed(1) }}%)</span>
              </n-space>
              <n-progress type="line" :percentage="opt.percentage" :show-indicator="false"
                          :color="getBarColor(opt.percentage)" />
              <n-space v-if="opt.voters && opt.voters.length > 0" size="small" style="margin-top: 6px; flex-wrap: wrap">
                <n-tag v-for="(voter, vi) in opt.voters" :key="vi" size="tiny" :bordered="false" type="info">{{ voter }}</n-tag>
              </n-space>
            </div>
          </template>
        </n-card>
      </template>
    </n-spin>
  </div>

  <!-- Fullscreen image preview -->
  <div v-if="previewImage" class="image-preview-overlay" @click="closePreview">
    <img :src="previewImage" class="image-preview-full" @click.stop />
    <span class="image-preview-close" @click="closePreview">&times;</span>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watchEffect} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import {voteApi} from '@/api/vote'
import type {VotePollDto} from '@/types'
import {Client} from '@stomp/stompjs'
import {useAuthStore} from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const authStore = useAuthStore()

const containerRef = ref<HTMLElement | null>(null)
const loading = ref(true)
const previewImage = ref<string | null>(null)
const submitting = ref(false)
const poll = ref<VotePollDto | null>(null)
const countdownSeconds = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
const voted = ref(false)
const selectedSingle = ref<number | null>(null)
const selectedMultiple = ref<number[]>([])
const scoredVotes = reactive<Record<number, number>>({})
let stompClient: Client | null = null

const hasVoted = computed(() => poll.value?.hasVoted ?? false)

const isExpired = computed(() => {
  if (!poll.value?.endTime) return false
  return new Date(poll.value.endTime).getTime() < Date.now()
})

const isNotStarted = computed(() => {
  if (!poll.value?.startTime) return false
  return new Date(poll.value.startTime).getTime() > Date.now()
})

const countdownLabel = computed(() => {
  if (isNotStarted.value) return t('vote.startCountdown')
  if (!isExpired.value && poll.value?.endTime) return t('vote.endCountdown')
  return ''
})

const countdownText = computed(() => {
  const s = countdownSeconds.value
  if (s <= 0) return ''
  const days = Math.floor(s / 86400)
  const hours = Math.floor((s % 86400) / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  const secs = s % 60
  if (days > 0) return `${days}${t('vote.days')} ${hours}${t('vote.hours')} ${minutes}${t('vote.minutes')} ${secs}${t('vote.seconds')}`
  if (hours > 0) return `${hours}${t('vote.hours')} ${minutes}${t('vote.minutes')} ${secs}${t('vote.seconds')}`
  if (minutes > 0) return `${minutes}${t('vote.minutes')} ${secs}${t('vote.seconds')}`
  return `${secs}${t('vote.seconds')}`
})

function updateCountdown() {
  if (!poll.value) return
  const now = Date.now()
  if (isNotStarted.value && poll.value.startTime) {
    countdownSeconds.value = Math.max(0, Math.floor((new Date(poll.value.startTime).getTime() - now) / 1000))
  } else if (!isExpired.value && poll.value.endTime) {
    countdownSeconds.value = Math.max(0, Math.floor((new Date(poll.value.endTime).getTime() - now) / 1000))
  } else {
    countdownSeconds.value = 0
  }
}

function startCountdownTimer() {
  if (countdownTimer) clearInterval(countdownTimer)
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)
}

const loginRequired = computed(() => {
  return poll.value && !poll.value.anonymous && !authStore.isLoggedIn
})

function openPreview(url: string) {
  previewImage.value = url
}

function closePreview() {
  previewImage.value = null
}

const voteTypeLabel = computed(() => {
  if (!poll.value) return ''
  if (poll.value.voteType === 'SINGLE') return t('vote.single')
  if (poll.value.voteType === 'MULTIPLE') return t('vote.multiple')
  return t('vote.scored')
})

const voteTypeTagType = computed(() => {
  if (!poll.value) return 'default'
  if (poll.value.voteType === 'SINGLE') return 'info'
  if (poll.value.voteType === 'MULTIPLE') return 'warning'
  return 'success'
})

const multipleReachedMax = computed(() => {
  if (!poll.value?.maxOptions) return false
  return selectedMultiple.value.length >= poll.value.maxOptions
})

function onMultipleChange(values: number[]) {
  if (poll.value?.maxOptions && values.length > poll.value.maxOptions) {
    message.warning(`${t('vote.maxOptions')}: ${poll.value.maxOptions}`)
    return
  }
  selectedMultiple.value = values
}

const totalScoredVotes = computed(() => {
  return Object.values(scoredVotes).reduce((sum, v) => sum + (v || 0), 0)
})

const remainingVotes = computed(() => {
  if (!poll.value?.maxTotalVotes) return Infinity
  return poll.value.maxTotalVotes - totalScoredVotes.value
})

const perOptionMax = computed(() => {
  return poll.value?.maxVotesPerOption ?? poll.value?.maxTotalVotes ?? 999
})

function setScoredVote(optionId: number, value: number) {
  if (!poll.value) return
  const maxPerOption = poll.value.maxVotesPerOption ?? poll.value.maxTotalVotes ?? 999
  const currentOther = totalScoredVotes.value - (scoredVotes[optionId] || 0)
  const maxAllowed = poll.value.maxTotalVotes ? Math.min(maxPerOption, poll.value.maxTotalVotes - currentOther) : maxPerOption
  scoredVotes[optionId] = Math.max(0, Math.min(value, maxAllowed))
}

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
    startCountdownTimer()
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

  if (poll.value.voteType === 'SCORED') {
    // Scored mode: send votes map
    const votes: Record<number, number> = {}
    for (const [k, v] of Object.entries(scoredVotes)) {
      if (v > 0) votes[Number(k)] = v
    }
    if (Object.keys(votes).length === 0) {
      message.warning(t('survey.options'))
      return
    }

    submitting.value = true
    try {
      const res = await voteApi.submit(route.params.shareId as string, {
        votes,
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
  } else {
    // SINGLE or MULTIPLE mode
    const optionIds: number[] = poll.value.voteType === 'SINGLE'
      ? (selectedSingle.value ? [selectedSingle.value] : [])
      : selectedMultiple.value

    if (optionIds.length === 0) {
      message.warning(t('survey.options'))
      return
    }

    if (poll.value.voteType === 'MULTIPLE' && poll.value.maxOptions && optionIds.length > poll.value.maxOptions) {
      message.warning(`${t('vote.maxOptions')}: ${poll.value.maxOptions}`)
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
}

onMounted(loadPoll)

watchEffect(() => {
  const parent = containerRef.value?.parentElement
  if (parent && poll.value?.backgroundUrl) {
    parent.style.backgroundImage = `url(${poll.value.backgroundUrl})`
    parent.style.backgroundSize = 'cover'
    parent.style.backgroundPosition = 'center'
    parent.style.backgroundRepeat = 'no-repeat'
  }
})

onUnmounted(() => {
  const parent = containerRef.value?.parentElement
  if (parent) {
    parent.style.backgroundImage = ''
    parent.style.backgroundSize = ''
    parent.style.backgroundPosition = ''
    parent.style.backgroundRepeat = ''
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>

<style scoped>
.vote-option-image {
  display: block;
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  margin-bottom: 8px;
  object-fit: contain;
  cursor: pointer;
  transition: opacity 0.2s;
}

.vote-option-image:hover {
  opacity: 0.85;
}

.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.image-preview-full {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  cursor: default;
}

.image-preview-close {
  position: fixed;
  top: 16px;
  right: 24px;
  font-size: 36px;
  color: #fff;
  cursor: pointer;
  line-height: 1;
  user-select: none;
}
</style>
