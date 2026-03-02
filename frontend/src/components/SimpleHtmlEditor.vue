<template>
  <div class="simple-html-editor"
       :style="{ border: `1px solid ${themeVars.borderColor}`, background: themeVars.cardColor }">
    <div v-if="editor" class="editor-toolbar"
         :style="{ borderBottom: `1px solid ${themeVars.borderColor}`, background: themeVars.actionColor }">
      <n-popselect v-model:value="currentHeading" :options="headingOptions" trigger="click"
                   @update:value="onHeadingChange">
        <n-button size="small" quaternary style="min-width: 80px">
          {{ currentHeadingLabel }}
        </n-button>
      </n-popselect>
      <n-button-group size="small" style="margin-left: 8px">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleBold().run()"
                      :type="editor!.isActive('bold') ? 'primary' : 'default'" quaternary>
              <b>B</b>
            </n-button>
          </template>
          {{ t('editor.bold') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleItalic().run()"
                      :type="editor!.isActive('italic') ? 'primary' : 'default'" quaternary>
              <i>I</i>
            </n-button>
          </template>
          {{ t('editor.italic') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleUnderline().run()"
                      :type="editor!.isActive('underline') ? 'primary' : 'default'" quaternary>
              <u>U</u>
            </n-button>
          </template>
          {{ t('editor.underline') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleStrike().run()"
                      :type="editor!.isActive('strike') ? 'primary' : 'default'" quaternary>
              <s>S</s>
            </n-button>
          </template>
          {{ t('editor.strikethrough') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleSuperscript().run()"
                      :type="editor!.isActive('superscript') ? 'primary' : 'default'" quaternary>
              X<sup>2</sup>
            </n-button>
          </template>
          {{ t('editor.superscript') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleSubscript().run()"
                      :type="editor!.isActive('subscript') ? 'primary' : 'default'" quaternary>
              X<sub>2</sub>
            </n-button>
          </template>
          {{ t('editor.subscript') }}
        </n-tooltip>
      </n-button-group>
      <n-button-group size="small" style="margin-left: 8px">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleBulletList().run()"
                      :type="editor!.isActive('bulletList') ? 'primary' : 'default'" quaternary>⁃
            </n-button>
          </template>
          {{ t('editor.bulletList') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().toggleOrderedList().run()"
                      :type="editor!.isActive('orderedList') ? 'primary' : 'default'" quaternary>1.
            </n-button>
          </template>
          {{ t('editor.orderedList') }}
        </n-tooltip>
      </n-button-group>
      <n-button-group size="small" style="margin-left: 8px">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="showLinkDialog" :type="editor!.isActive('link') ? 'primary' : 'default'" quaternary>🔗
            </n-button>
          </template>
          {{ t('editor.link') }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="showImageDialog" quaternary>🖼</n-button>
          </template>
          {{ t('editor.image') }}
        </n-tooltip>
      </n-button-group>
      <n-button-group size="small" style="margin-left: 8px">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button @click="editor!.chain().focus().clearNodes().unsetAllMarks().run()" quaternary>✕</n-button>
          </template>
          {{ t('editor.clearFormat') }}
        </n-tooltip>
      </n-button-group>
    </div>
    <EditorContent :editor="editor" class="editor-content"/>

    <n-modal v-model:show="linkDialogVisible" preset="dialog" :title="t('editor.link')"
             :positive-text="t('common.confirm')" :negative-text="t('common.cancel')" @positive-click="confirmLink"
             @negative-click="linkDialogVisible = false" style="width: 440px">
      <n-input v-model:value="linkUrl" :placeholder="t('editor.linkPlaceholder')" @keydown.enter="confirmLink"/>
    </n-modal>

    <n-modal v-model:show="imageDialogVisible" preset="dialog" :title="t('editor.image')"
             :positive-text="t('common.confirm')" :negative-text="t('common.cancel')" @positive-click="confirmImage"
             @negative-click="imageDialogVisible = false" style="width: 440px">
      <n-input v-model:value="imageUrl" :placeholder="t('editor.imagePlaceholder')" @keydown.enter="confirmImage"/>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, ref, watch} from 'vue'
import {useThemeVars} from 'naive-ui'
import {useI18n} from 'vue-i18n'
import {EditorContent, useEditor} from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Superscript from '@tiptap/extension-superscript'
import Subscript from '@tiptap/extension-subscript'

const themeVars = useThemeVars()
const {t} = useI18n()

const linkDialogVisible = ref(false)
const linkUrl = ref('')
const imageDialogVisible = ref(false)
const imageUrl = ref('')

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const headingOptions = computed(() => [
  {label: t('editor.paragraph'), value: 0},
  {label: t('editor.heading1'), value: 1},
  {label: t('editor.heading2'), value: 2},
  {label: t('editor.heading3'), value: 3},
])

const currentHeading = computed(() => {
  if (!editor.value) return 0
  for (const level of [1, 2, 3] as const) {
    if (editor.value.isActive('heading', {level})) return level
  }
  return 0
})

const currentHeadingLabel = computed(() => {
  const val = currentHeading.value
  if (val === 0) return t('editor.paragraph')
  return t(`editor.heading${val}`)
})

function onHeadingChange(value: number) {
  if (!editor.value) return
  if (value === 0) {
    editor.value.chain().focus().setParagraph().run()
  } else {
    editor.value.chain().focus().toggleHeading({level: value as 1 | 2 | 3}).run()
  }
}

const editor = useEditor({
  content: props.modelValue || '',
  extensions: [
    StarterKit,
    Underline,
    Link.configure({openOnClick: false}),
    Image,
    Superscript,
    Subscript,
  ],
  onUpdate({editor}) {
    const html = editor.getHTML()
    emit('update:modelValue', html === '<p></p>' ? '' : html)
  },
})

watch(() => props.modelValue, (val) => {
  if (!editor.value) return
  const current = editor.value.getHTML()
  if (current !== val) {
    editor.value.commands.setContent(val || '', false)
  }
})

function showLinkDialog() {
  if (!editor.value) return
  linkUrl.value = editor.value.getAttributes('link').href || ''
  linkDialogVisible.value = true
}

function confirmLink() {
  if (!editor.value) return
  linkDialogVisible.value = false
  if (linkUrl.value === '') {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
  } else {
    editor.value.chain().focus().extendMarkRange('link').setLink({href: linkUrl.value}).run()
  }
}

function showImageDialog() {
  imageUrl.value = ''
  imageDialogVisible.value = true
}

function confirmImage() {
  imageDialogVisible.value = false
  if (imageUrl.value) {
    editor.value?.chain().focus().setImage({src: imageUrl.value}).run()
  }
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.simple-html-editor {
  border-radius: 3px;
  overflow: hidden;
  width: 100%;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  flex-wrap: wrap;
}

.editor-content :deep(.tiptap) {
  min-height: 80px;
  padding: 8px 12px;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
}

.editor-content :deep(.tiptap:focus) {
  box-shadow: 0 0 0 2px rgba(24, 160, 88, 0.2);
}

.editor-content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.editor-content :deep(a) {
  color: #2080f0;
  text-decoration: underline;
}

.editor-content :deep(ul),
.editor-content :deep(ol) {
  padding-left: 20px;
}

.editor-content :deep(p) {
  margin: 0 0 4px;
}
</style>
