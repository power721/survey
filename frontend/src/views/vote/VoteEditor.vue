<template>
  <div class="card-container">
    <n-card :title="isEdit ? t('vote.editVote') : t('vote.createVote')">
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item :label="t('vote.voteTitle')" path="title" required>
          <n-input v-model:value="form.title" :placeholder="t('vote.voteTitle')"/>
        </n-form-item>
        <n-form-item :label="t('vote.description')">
          <SimpleHtmlEditor v-model="form.description"/>
        </n-form-item>
        <n-form-item :label="t('common.logoUrl')">
          <n-input v-model:value="form.logoUrl" :placeholder="t('common.logoUrlPlaceholder')"/>
        </n-form-item>
        <n-form-item :label="t('common.backgroundUrl')">
          <n-input v-model:value="form.backgroundUrl" :placeholder="t('common.backgroundUrlPlaceholder')"/>
        </n-form-item>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('vote.voteType')">
              <n-select v-model:value="form.voteType" :options="voteTypeOptions"/>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('vote.frequency')">
              <n-select v-model:value="form.frequency" :options="frequencyOptions"/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('survey.accessLevel')">
              <n-select v-model:value="form.accessLevel" :options="accessOptions"/>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.anonymous')">
              <n-switch v-model:value="form.anonymous"/>
            </n-form-item>
          </n-gi>
          <n-gi v-if="!form.anonymous && form.voteType !== 'SCORED'">
            <n-form-item :label="t('vote.showVoters')">
              <n-switch v-model:value="form.showVoters"/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('common.startTime')" required>
              <n-date-picker v-model:value="startTimeTs" type="datetime" style="width: 100%"/>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('vote.endTime')" required>
              <n-date-picker v-model:value="endTimeTs" type="datetime" style="width: 100%"/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi v-if="form.voteType === 'MULTIPLE'">
            <n-form-item :label="t('vote.maxOptions')">
              <n-input-number v-model:value="form.maxOptions" :min="2" clearable style="width: 100%"/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid v-if="form.voteType === 'SCORED'" :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('vote.maxTotalVotes')" required>
              <n-input-number v-model:value="form.maxTotalVotes" :min="1" style="width: 100%"/>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('vote.maxVotesPerOption')" required>
              <n-input-number v-model:value="form.maxVotesPerOption" :min="1" style="width: 100%"/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>{{ t('survey.options') }}</n-divider>

        <draggable v-model="form.options" item-key="_key" handle=".drag-handle" animation="200" style="width: 100%">
          <template #item="{ element: opt, index: oi }">
            <n-card size="small" closable style="margin-bottom: 8px" @close="removeOption(oi)">
              <template #header>
                <n-space align="center" size="small">
                  <span class="drag-handle" style="cursor: grab; font-size: 18px; color: #999; user-select: none">&#x2630;</span>
                  <span>{{ t('survey.options') }} {{ oi + 1 }}</span>
                </n-space>
              </template>
              <n-form-item :label="t('vote.optionTitle')" required>
                <n-input v-model:value="opt.title" :placeholder="t('vote.optionTitle')"/>
              </n-form-item>
              <n-form-item :label="t('vote.optionContent')">
                <n-input v-model:value="opt.content" type="textarea" :rows="2" :placeholder="t('vote.optionContent')"/>
              </n-form-item>
              <n-form-item :label="t('vote.imageUrl')">
                <n-input v-model:value="opt.imageUrl" placeholder="https://..."/>
              </n-form-item>
              <img v-if="opt.imageUrl" :src="opt.imageUrl" class="option-image-preview"
                   @click="openPreview(opt.imageUrl)"/>
            </n-card>
          </template>
        </draggable>
        <n-space style="margin-top: 8px">
          <n-button dashed @click="addOption">+ {{ t('survey.addOption') }}</n-button>
          <n-button dashed @click="showBatchAdd = true">{{ t('vote.batchAdd') }}</n-button>
        </n-space>

        <n-modal v-model:show="showBatchAdd" preset="dialog" :title="t('vote.batchAdd')"
                 :positive-text="t('common.confirm')" :negative-text="t('common.cancel')"
                 @positive-click="handleBatchAdd">
          <n-input v-model:value="batchText" type="textarea" :rows="8" :placeholder="t('vote.batchAddPlaceholder')"/>
        </n-modal>

        <n-space justify="end" style="margin-top: 24px">
          <n-button @click="router.back()">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</n-button>
        </n-space>
      </n-form>
    </n-card>
  </div>

  <!-- Fullscreen image preview -->
  <div v-if="previewImage" class="image-preview-overlay" @click="closePreview">
    <img :src="previewImage" class="image-preview-full" @click.stop/>
    <span class="image-preview-close" @click="closePreview">&times;</span>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import draggable from 'vuedraggable'
import {voteApi} from '@/api/vote'
import type {VotePollCreateRequest} from '@/types'
import SimpleHtmlEditor from '@/components/SimpleHtmlEditor.vue'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()
const message = useMessage()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const previewImage = ref<string | null>(null)
const showBatchAdd = ref(false)
const batchText = ref('')

function openPreview(url: string) {
  previewImage.value = url
}

function closePreview() {
  previewImage.value = null
}

const defaultEndTime = Date.now() + 7 * 24 * 60 * 60 * 1000
const startTimeTs = ref<number | null>(Date.now())
const endTimeTs = ref<number | null>(defaultEndTime)
let keySeq = 0

function nextKey() {
  return `opt_${++keySeq}`
}

const form = ref<VotePollCreateRequest>({
  title: '',
  description: '',
  logoUrl: null,
  backgroundUrl: null,
  voteType: 'SINGLE',
  frequency: 'ONCE',
  accessLevel: 'PUBLIC',
  anonymous: false,
  showVoters: true,
  maxTotalVotes: null,
  maxOptions: null,
  maxVotesPerOption: null,
  startTime: null,
  endTime: null,
  options: [
    {title: '', sortOrder: 0, _key: nextKey()},
    {title: '', sortOrder: 1, _key: nextKey()},
  ],
})

watch(() => form.value.voteType, (newType) => {
  if (newType === 'SCORED') {
    if (!form.value.maxTotalVotes) form.value.maxTotalVotes = 10
    if (!form.value.maxVotesPerOption) form.value.maxVotesPerOption = 3
  }
})

const voteTypeOptions = [
  {label: t('vote.single'), value: 'SINGLE'},
  {label: t('vote.multiple'), value: 'MULTIPLE'},
  {label: t('vote.scored'), value: 'SCORED'},
]

const frequencyOptions = [
  {label: t('vote.once'), value: 'ONCE'},
  {label: t('vote.daily'), value: 'DAILY'},
]

const accessOptions = [
  {label: t('common.public'), value: 'PUBLIC'},
  {label: t('common.private'), value: 'PRIVATE'},
]

function addOption() {
  form.value.options.push({title: '', sortOrder: form.value.options.length, _key: nextKey()})
}

function handleBatchAdd() {
  const lines = batchText.value.split('\n').map(l => l.trim()).filter(l => l.length > 0)
  if (lines.length === 0) {
    message.warning(t('vote.batchAddEmpty'))
    return
  }
  const startIndex = form.value.options.length
  lines.forEach((line, i) => {
    const parts = line.split(/\s+/)
    const last = parts[parts.length - 1]
    let title = line
    let imageUrl = ''
    if (parts.length > 1 && /^https?:\/\//i.test(last)) {
      imageUrl = last
      title = parts.slice(0, -1).join(' ')
    }
    form.value.options.push({title, imageUrl, sortOrder: startIndex + i, _key: nextKey()})
  })
  batchText.value = ''
  message.success(t('vote.batchAddSuccess', {count: lines.length}))
}

function removeOption(index: number) {
  if (form.value.options.length <= 2) {
    message.warning('At least 2 options required')
    return
  }
  form.value.options.splice(index, 1)
}

async function loadPoll() {
  if (!isEdit.value) return
  try {
    const res = await voteApi.getById(Number(route.params.id))
    const poll = res.data.data
    form.value = {
      title: poll.title,
      description: poll.description || '',
      logoUrl: poll.logoUrl || null,
      backgroundUrl: poll.backgroundUrl || null,
      voteType: poll.voteType,
      frequency: poll.frequency,
      accessLevel: poll.accessLevel,
      anonymous: poll.anonymous,
      showVoters: poll.showVoters,
      maxTotalVotes: poll.maxTotalVotes,
      maxOptions: poll.maxOptions,
      maxVotesPerOption: poll.maxVotesPerOption,
      startTime: poll.startTime,
      endTime: poll.endTime,
      options: poll.options.map((o) => ({
        id: o.id,
        title: o.title,
        content: o.content || '',
        imageUrl: o.imageUrl || '',
        sortOrder: o.sortOrder,
        _key: nextKey(),
      })),
    }
    if (poll.startTime) {
      const ts = new Date(poll.startTime).getTime()
      startTimeTs.value = Number.isNaN(ts) ? null : ts
    }
    if (poll.endTime) {
      const ts = new Date(poll.endTime).getTime()
      endTimeTs.value = Number.isNaN(ts) ? null : ts
    }
  } catch (e) {
    console.error(e)
  }
}

async function handleSave() {
  if (!form.value.title.trim()) {
    message.warning(t('vote.voteTitle'))
    return
  }
  if (form.value.options.some((o) => !o.title.trim())) {
    message.warning(t('vote.optionTitle'))
    return
  }
  if (!startTimeTs.value) {
    message.warning(t('common.startTimeRequired'))
    return
  }
  if (!endTimeTs.value) {
    message.warning(t('vote.endTimeRequired'))
    return
  }
  if (endTimeTs.value <= startTimeTs.value) {
    message.warning(t('common.endTimeBeforeStartTime'))
    return
  }
  if (form.value.voteType === 'SCORED') {
    if (!form.value.maxTotalVotes || form.value.maxTotalVotes < 1) {
      message.warning(t('vote.maxTotalVotesRequired'))
      return
    }
    if (!form.value.maxVotesPerOption || form.value.maxVotesPerOption < 1) {
      message.warning(t('vote.maxVotesPerOptionRequired'))
      return
    }
  }

  saving.value = true
  try {
    form.value.startTime = startTimeTs.value ? new Date(startTimeTs.value).toISOString() : null
    form.value.endTime = endTimeTs.value ? new Date(endTimeTs.value).toISOString() : null
    form.value.options.forEach((o, i) => {
      o.sortOrder = i
    })
    if (isEdit.value) {
      await voteApi.update(Number(route.params.id), form.value)
    } else {
      await voteApi.create(form.value)
    }
    message.success(t('common.save'))
    router.push('/votes')
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    saving.value = false
  }
}

onMounted(loadPoll)
</script>

<style scoped>
.option-image-preview {
  display: block;
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  margin-top: 4px;
  object-fit: contain;
  cursor: pointer;
  transition: opacity 0.2s;
}

.option-image-preview:hover {
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
