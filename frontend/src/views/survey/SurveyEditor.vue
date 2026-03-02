<template>
  <div class="card-container">
    <n-card :title="isEdit ? t('survey.editSurvey') : t('survey.createSurvey')">
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item :label="t('survey.surveyTitle')" path="title" required>
          <n-input v-model:value="form.title" :placeholder="t('survey.surveyTitle')"/>
        </n-form-item>
        <n-form-item :label="t('survey.description')">
          <SimpleHtmlEditor v-model="form.description"/>
        </n-form-item>
        <n-form-item :label="t('common.logoUrl')">
          <n-input v-model:value="form.logoUrl" :placeholder="t('common.logoUrlPlaceholder')"/>
        </n-form-item>
        <n-form-item :label="t('common.backgroundUrl')">
          <n-input v-model:value="form.backgroundUrl" :placeholder="t('common.backgroundUrlPlaceholder')"/>
        </n-form-item>

        <n-grid :cols="3" :x-gap="16">
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
          <n-gi>
            <n-form-item :label="t('survey.allowUpdate')">
              <n-switch v-model:value="form.allowUpdate"/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="3" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('common.startTime')" required>
              <n-date-picker v-model:value="startTimeTs" type="datetime" style="width: 100%"/>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.endTime')" required>
              <n-date-picker v-model:value="endTimeTs" type="datetime" style="width: 100%"/>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.maxResponses')">
              <n-input-number v-model:value="form.maxResponses" :min="1"
                              :placeholder="t('survey.maxResponsesPlaceholder')" style="width: 100%" clearable/>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>{{ t('survey.questions') }}</n-divider>

        <draggable v-model="form.sections" item-key="_key" handle=".section-drag-handle" animation="200">
          <template #item="{ element: sec, index: si }">
            <n-card size="small" style="margin-bottom: 16px; border: 1.5px solid #e0e0e6">
              <template #header>
                <n-space align="center" size="small">
                  <span class="section-drag-handle"
                        style="cursor: grab; font-size: 18px; color: #999; user-select: none">&#x2630;</span>
                  <n-input
                      v-model:value="sec.title"
                      :placeholder="`${t('survey.section')} ${si + 1}`"
                      style="width: 200px"
                      size="small"
                  />
                  <n-tag size="small" type="info">{{ t('survey.section') }} {{ si + 1 }}</n-tag>
                </n-space>
              </template>
              <template #header-extra>
                <n-button
                    v-if="form.sections.length > 1"
                    size="tiny" type="error" quaternary
                    @click.stop="removeSection(si)"
                >{{ t('common.delete') }}
                </n-button>
              </template>

              <draggable v-model="sec.questions" item-key="_key" handle=".drag-handle" animation="200">
                <template #item="{ element: q, index: qi }">
                  <n-card size="small" closable style="margin-bottom: 12px" @close="removeQuestion(si, qi)">
                    <template #header>
                      <n-space align="center" size="small">
                        <span class="drag-handle" style="cursor: grab; font-size: 18px; color: #999; user-select: none">&#x2630;</span>
                        <span>{{ t('survey.questions') }} {{ qi + 1 }}</span>
                        <n-tag size="small">{{ t(`survey.types.${q.type}`) }}</n-tag>
                      </n-space>
                    </template>
                    <template #header-extra>
                      <n-button size="tiny" quaternary @click.stop="copyQuestion(si, qi)">{{
                          t('survey.copyQuestion')
                        }}
                      </n-button>
                    </template>

                    <n-form-item :label="t('survey.questionType')">
                      <n-select v-model:value="q.type" :options="questionTypeOptions"/>
                    </n-form-item>

                    <n-form-item :label="t('survey.questionTitle')">
                      <n-input v-model:value="q.title" :placeholder="t('survey.questionTitle')"/>
                    </n-form-item>

                    <n-form-item :label="t('common.required')">
                      <n-switch v-model:value="q.required"/>
                    </n-form-item>

                    <template v-if="q.type === 'SINGLE_CHOICE' || q.type === 'MULTIPLE_CHOICE'">
                      <n-form-item :label="t('survey.options')">
                        <n-space vertical style="width: 100%">
                          <n-input-group v-for="(opt, oi) in q.options" :key="oi">
                            <n-input v-model:value="opt.content" :placeholder="`${t('survey.options')} ${oi + 1}`"/>
                            <n-button @click="removeOption(si, qi, oi)" type="error" ghost>✕</n-button>
                          </n-input-group>
                          <n-button dashed block @click="addOption(si, qi)">+ {{ t('survey.addOption') }}</n-button>
                        </n-space>
                      </n-form-item>
                    </template>

                    <n-form-item :label="t('survey.conditionLogic')">
                      <n-space align="center" wrap>
                        <n-select
                            v-model:value="q.conditionQuestionId"
                            :options="conditionQuestionOptions(si, qi)"
                            :placeholder="t('survey.conditionNone')"
                            clearable
                            style="min-width: 220px"
                            @update:value="q.conditionOptionId = null"
                        />
                        <template v-if="q.conditionQuestionId">
                          <n-text>{{ t('survey.conditionOption') }}</n-text>
                          <n-select
                              v-model:value="q.conditionOptionId"
                              :options="conditionOptionOptions(si, q.conditionQuestionId)"
                              clearable
                              style="min-width: 180px"
                          />
                          <n-text depth="3">→ {{ t('survey.conditionThen') }}</n-text>
                        </template>
                      </n-space>
                    </n-form-item>
                  </n-card>
                </template>
              </draggable>

              <n-button dashed block @click="addQuestion(si)" style="margin-top: 4px">+ {{
                  t('survey.addQuestion')
                }}
              </n-button>
            </n-card>
          </template>
        </draggable>

        <n-button dashed block size="large" @click="addSection" style="margin-top: 8px">+ {{
            t('survey.addSection')
          }}
        </n-button>

        <n-space justify="end" style="margin-top: 24px">
          <n-button @click="router.back()">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</n-button>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import draggable from 'vuedraggable'
import {surveyApi} from '@/api/survey'
import type {QuestionRequest, SurveyCreateRequest} from '@/types'
import SimpleHtmlEditor from '@/components/SimpleHtmlEditor.vue'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()
const message = useMessage()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const startTimeTs = ref<number | null>(Date.now())
const defaultEndTime = Date.now() + 30 * 24 * 60 * 60 * 1000
const endTimeTs = ref<number | null>(defaultEndTime)
let keySeq = 0

function nextKey() {
  return `q_${++keySeq}`
}

function newQuestion(sortOrder = 0): QuestionRequest {
  return {
    type: 'SINGLE_CHOICE',
    title: '',
    description: '',
    required: false,
    sortOrder,
    conditionQuestionId: null,
    conditionOptionId: null,
    options: [{content: '', sortOrder: 0}, {content: '', sortOrder: 1}],
    _key: nextKey(),
  }
}

const form = ref<SurveyCreateRequest>({
  title: '',
  description: '',
  logoUrl: null,
  backgroundUrl: null,
  accessLevel: 'PUBLIC',
  anonymous: true,
  template: false,
  allowUpdate: false,
  startTime: null,
  endTime: null,
  maxResponses: null,
  questions: [],
  sections: [{title: '', sortOrder: 0, questions: [newQuestion()], _key: nextKey()}],
})

const accessOptions = [
  {label: t('common.public'), value: 'PUBLIC'},
  {label: t('common.private'), value: 'PRIVATE'},
]

const questionTypeOptions = [
  {label: t('survey.types.SINGLE_CHOICE'), value: 'SINGLE_CHOICE'},
  {label: t('survey.types.MULTIPLE_CHOICE'), value: 'MULTIPLE_CHOICE'},
  {label: t('survey.types.TEXT'), value: 'TEXT'},
  {label: t('survey.types.TEXTAREA'), value: 'TEXTAREA'},
  {label: t('survey.types.NUMBER'), value: 'NUMBER'},
  {label: t('survey.types.RATING'), value: 'RATING'},
  {label: t('survey.types.DATE'), value: 'DATE'},
  {label: t('survey.types.EMAIL'), value: 'EMAIL'},
  {label: t('survey.types.URL'), value: 'URL'},
  {label: t('survey.types.PHONE'), value: 'PHONE'},
  {label: t('survey.types.ID_CARD'), value: 'ID_CARD'},
  {label: t('survey.types.FILE'), value: 'FILE'},
]

function addSection() {
  form.value.sections.push({
    title: '',
    sortOrder: form.value.sections.length,
    questions: [newQuestion()],
    _key: nextKey(),
  })
}

function removeSection(si: number) {
  form.value.sections.splice(si, 1)
}

function addQuestion(si: number) {
  const sec = form.value.sections[si]
  sec.questions.push(newQuestion(sec.questions.length))
}

function removeQuestion(si: number, qi: number) {
  form.value.sections[si].questions.splice(qi, 1)
}

function copyQuestion(si: number, qi: number) {
  const sec = form.value.sections[si]
  const source = sec.questions[qi]
  const copy: QuestionRequest = {
    type: source.type,
    title: source.title,
    description: source.description,
    required: source.required,
    sortOrder: sec.questions.length,
    conditionQuestionId: source.conditionQuestionId,
    conditionOptionId: source.conditionOptionId,
    options: source.options.map((o) => ({content: o.content, sortOrder: o.sortOrder})),
    _key: nextKey(),
  }
  sec.questions.splice(qi + 1, 0, copy)
}

function addOption(si: number, qi: number) {
  const opts = form.value.sections[si].questions[qi].options
  opts.push({content: '', sortOrder: opts.length})
}

function removeOption(si: number, qi: number, oi: number) {
  form.value.sections[si].questions[qi].options.splice(oi, 1)
}

function conditionQuestionOptions(si: number, qi: number) {
  const result: { label: string, value: number }[] = []
  for (let s = 0; s < form.value.sections.length; s++) {
    const sec = form.value.sections[s]
    for (let q = 0; q < sec.questions.length; q++) {
      if (s === si && q >= qi) break
      const question = sec.questions[q]
      if (question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE') {
        if (question.id) {
          result.push({
            label: `Q${q + 1}: ${question.title || t('survey.questionTitle')}`,
            value: question.id,
          })
        }
      }
    }
  }
  return result
}

function conditionOptionOptions(si: number, conditionQuestionId: number | null) {
  if (!conditionQuestionId) return []
  for (const sec of form.value.sections) {
    for (const q of sec.questions) {
      if (q.id === conditionQuestionId) {
        return q.options
            .filter(o => o.content)
            .map(o => ({label: o.content, value: o.id as number}))
      }
    }
  }
  return []
}

async function loadSurvey() {
  if (!isEdit.value) return
  try {
    const res = await surveyApi.getById(Number(route.params.id))
    const survey = res.data.data

    const mapQuestion = (q: any) => ({
      id: q.id,
      type: q.type,
      title: q.title,
      description: q.description || '',
      required: q.required,
      sortOrder: q.sortOrder,
      conditionQuestionId: q.conditionQuestionId ?? null,
      conditionOptionId: q.conditionOptionId ?? null,
      options: q.options.map((o: any) => ({id: o.id, content: o.content, sortOrder: o.sortOrder})),
      _key: nextKey(),
    })

    let sections
    if (survey.sections && survey.sections.length > 0) {
      sections = survey.sections.map((s: any) => ({
        id: s.id,
        title: s.title || '',
        sortOrder: s.sortOrder,
        questions: s.questions.map(mapQuestion),
        _key: nextKey(),
      }))
    } else {
      sections = [{
        title: '',
        sortOrder: 0,
        questions: survey.questions.map(mapQuestion),
        _key: nextKey(),
      }]
    }

    form.value = {
      title: survey.title,
      description: survey.description || '',
      logoUrl: survey.logoUrl || null,
      backgroundUrl: survey.backgroundUrl || null,
      accessLevel: survey.accessLevel,
      anonymous: survey.anonymous,
      template: survey.template,
      allowUpdate: survey.allowUpdate,
      startTime: survey.startTime,
      endTime: survey.endTime,
      maxResponses: survey.maxResponses ?? null,
      questions: [],
      sections,
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
  const totalQuestions = form.value.sections.reduce((sum, s) => sum + s.questions.length, 0)
  if (totalQuestions === 0) {
    message.warning(t('survey.atLeastOneQuestion'))
    return
  }
  saving.value = true
  try {
    form.value.startTime = startTimeTs.value ? new Date(startTimeTs.value).toISOString() : null
    form.value.endTime = endTimeTs.value ? new Date(endTimeTs.value).toISOString() : null
    form.value.sections.forEach((s, si) => {
      s.sortOrder = si
      s.questions.forEach((q, qi) => {
        q.sortOrder = qi
      })
    })
    form.value.questions = []
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
