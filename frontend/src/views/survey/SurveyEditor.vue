<template>
  <div class="card-container">
    <n-card :title="isEdit ? t('survey.editSurvey') : t('survey.createSurvey')">
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item :label="t('survey.surveyTitle')" path="title" required>
          <n-input v-model:value="form.title" :placeholder="t('survey.surveyTitle')" />
        </n-form-item>
        <n-form-item :label="t('survey.description')">
          <n-input v-model:value="form.description" type="textarea" :rows="3" :placeholder="t('survey.description')" />
        </n-form-item>

        <n-grid :cols="3" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('survey.accessLevel')">
              <n-select v-model:value="form.accessLevel" :options="accessOptions" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.anonymous')">
              <n-switch v-model:value="form.anonymous" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.allowUpdate')">
              <n-switch v-model:value="form.allowUpdate" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('common.startTime')" required>
              <n-date-picker v-model:value="startTimeTs" type="datetime" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.endTime')" required>
              <n-date-picker v-model:value="endTimeTs" type="datetime" style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>{{ t('survey.questions') }}</n-divider>

        <draggable v-model="form.questions" item-key="_key" handle=".drag-handle" animation="200">
          <template #item="{ element: q, index: qi }">
            <n-card size="small" closable style="margin-bottom: 12px" @close="removeQuestion(qi)">
              <template #header>
                <n-space align="center" size="small">
                  <span class="drag-handle" style="cursor: grab; font-size: 18px; color: #999; user-select: none">&#x2630;</span>
                  <span>{{ t('survey.questions') }} {{ qi + 1 }}</span>
                  <n-tag size="small">{{ t(`survey.types.${q.type}`) }}</n-tag>
                </n-space>
              </template>
              <template #header-extra>
                <n-button size="tiny" quaternary @click.stop="copyQuestion(qi)">{{ t('survey.copyQuestion') }}</n-button>
              </template>

              <n-form-item :label="t('survey.questionType')">
                <n-select v-model:value="q.type" :options="questionTypeOptions" />
              </n-form-item>

              <n-form-item :label="t('survey.questionTitle')">
                <n-input v-model:value="q.title" :placeholder="t('survey.questionTitle')" />
              </n-form-item>

              <n-form-item :label="t('common.required')">
                <n-switch v-model:value="q.required" />
              </n-form-item>

              <template v-if="q.type === 'SINGLE_CHOICE' || q.type === 'MULTIPLE_CHOICE'">
                <n-form-item :label="t('survey.options')">
                  <n-space vertical style="width: 100%">
                    <n-input-group v-for="(opt, oi) in q.options" :key="oi">
                      <n-input v-model:value="opt.content" :placeholder="`${t('survey.options')} ${oi + 1}`" />
                      <n-button @click="removeOption(qi, oi)" type="error" ghost>✕</n-button>
                    </n-input-group>
                    <n-button dashed block @click="addOption(qi)">+ {{ t('survey.addOption') }}</n-button>
                  </n-space>
                </n-form-item>
              </template>
            </n-card>
          </template>
        </draggable>
        <n-button dashed block size="large" @click="addQuestion" style="margin-top: 8px">+ {{ t('survey.addQuestion') }}</n-button>

        <n-space justify="end" style="margin-top: 24px">
          <n-button @click="router.back()">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</n-button>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import draggable from 'vuedraggable'
import { surveyApi } from '@/api/survey'
import type { SurveyCreateRequest, QuestionRequest, OptionRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const startTimeTs = ref<number | null>(Date.now())
const defaultEndTime = Date.now() + 30 * 24 * 60 * 60 * 1000
const endTimeTs = ref<number | null>(defaultEndTime)
let keySeq = 0
function nextKey() { return `q_${++keySeq}` }

const form = ref<SurveyCreateRequest>({
  title: '',
  description: '',
  accessLevel: 'PUBLIC',
  anonymous: true,
  template: false,
  allowUpdate: false,
  startTime: null,
  endTime: null,
  questions: [],
})

const accessOptions = [
  { label: t('common.public'), value: 'PUBLIC' },
  { label: t('common.private'), value: 'PRIVATE' },
]

const questionTypeOptions = [
  { label: t('survey.types.SINGLE_CHOICE'), value: 'SINGLE_CHOICE' },
  { label: t('survey.types.MULTIPLE_CHOICE'), value: 'MULTIPLE_CHOICE' },
  { label: t('survey.types.TEXT'), value: 'TEXT' },
  { label: t('survey.types.TEXTAREA'), value: 'TEXTAREA' },
  { label: t('survey.types.NUMBER'), value: 'NUMBER' },
  { label: t('survey.types.RATING'), value: 'RATING' },
  { label: t('survey.types.DATE'), value: 'DATE' },
  { label: t('survey.types.EMAIL'), value: 'EMAIL' },
  { label: t('survey.types.URL'), value: 'URL' },
  { label: t('survey.types.PHONE'), value: 'PHONE' },
  { label: t('survey.types.ID_CARD'), value: 'ID_CARD' },
  { label: t('survey.types.FILE'), value: 'FILE' },
]

function addQuestion() {
  form.value.questions.push({
    type: 'SINGLE_CHOICE',
    title: '',
    description: '',
    required: false,
    sortOrder: form.value.questions.length,
    options: [{ content: '', sortOrder: 0 }, { content: '', sortOrder: 1 }],
    _key: nextKey(),
  })
}

function removeQuestion(index: number) {
  form.value.questions.splice(index, 1)
}

function copyQuestion(index: number) {
  const source = form.value.questions[index]
  const copy = {
    type: source.type,
    title: source.title,
    description: source.description,
    required: source.required,
    sortOrder: form.value.questions.length,
    options: source.options.map((o) => ({ content: o.content, sortOrder: o.sortOrder })),
    _key: nextKey(),
  }
  form.value.questions.splice(index + 1, 0, copy)
}

function addOption(qi: number) {
  const opts = form.value.questions[qi].options
  opts.push({ content: '', sortOrder: opts.length })
}

function removeOption(qi: number, oi: number) {
  form.value.questions[qi].options.splice(oi, 1)
}

async function loadSurvey() {
  if (!isEdit.value) return
  try {
    const res = await surveyApi.getById(Number(route.params.id))
    const survey = res.data.data
    form.value = {
      title: survey.title,
      description: survey.description || '',
      accessLevel: survey.accessLevel,
      anonymous: survey.anonymous,
      template: survey.template,
      allowUpdate: survey.allowUpdate,
      startTime: survey.startTime,
      endTime: survey.endTime,
      questions: survey.questions.map((q) => ({
        id: q.id,
        type: q.type,
        title: q.title,
        description: q.description || '',
        required: q.required,
        sortOrder: q.sortOrder,
        options: q.options.map((o) => ({ id: o.id, content: o.content, sortOrder: o.sortOrder })),
        _key: nextKey(),
      })),
    }
    if (survey.startTime) {
      const ts = new Date(survey.startTime).getTime()
      startTimeTs.value = Number.isNaN(ts) ? null : ts
    }
    if (survey.endTime) {
      const ts = new Date(survey.endTime).getTime()
      endTimeTs.value = Number.isNaN(ts) ? null : ts
    }
  } catch (e) {
    console.error(e)
  }
}

async function handleSave() {
  if (!form.value.title.trim()) {
    message.warning(t('survey.surveyTitle'))
    return
  }
  if (!startTimeTs.value) {
    message.warning(t('common.startTimeRequired'))
    return
  }
  if (!endTimeTs.value) {
    message.warning(t('survey.endTimeRequired'))
    return
  }
  if (endTimeTs.value <= startTimeTs.value) {
    message.warning(t('common.endTimeBeforeStartTime'))
    return
  }
  saving.value = true
  try {
    form.value.startTime = startTimeTs.value ? new Date(startTimeTs.value).toISOString() : null
    form.value.endTime = endTimeTs.value ? new Date(endTimeTs.value).toISOString() : null
    form.value.questions.forEach((q, i) => { q.sortOrder = i })
    if (isEdit.value) {
      await surveyApi.update(Number(route.params.id), form.value)
    } else {
      await surveyApi.create(form.value)
    }
    message.success(t('common.save'))
    router.push('/surveys')
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    saving.value = false
  }
}

onMounted(loadSurvey)
</script>
