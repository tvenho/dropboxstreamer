<template>
  <div class="file-list-container">
    <div class="breadcrumb">
      <button class="crumb" @click="navigateTo('')">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
        </svg>
        Koti
      </button>
      <template v-for="(crumb, i) in breadcrumbs" :key="i">
        <span class="crumb-sep">/</span>
        <button class="crumb" @click="navigateTo(crumb.path)">{{ crumb.name }}</button>
      </template>
    </div>

    <div class="file-list" v-if="!loading && !error">
      <div
        v-for="file in files"
        :key="file.path"
        class="file-item"
        :class="{ active: props.activePath === file.path, folder: file.type === 'folder' }"
        @click="onItemClick(file)"
      >
        <span class="file-icon">
          <svg v-if="file.type === 'folder'" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor" opacity="0.7">
            <path d="M10 4H2v16h20V6H12l-2-2z"/>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
        </span>
        <span class="file-name">{{ file.name }}</span>
        <span class="file-size" v-if="file.size">{{ formatSize(file.size) }}</span>
      </div>

      <div v-if="files.length === 0" class="empty">
        Ei MP3-tiedostoja tai kansioita tässä hakemistossa.
      </div>
    </div>

    <div class="state-message" v-if="loading">
      <div class="spinner"></div>
      Ladataan...
    </div>

    <div class="state-message error" v-if="error">
      Virhe: {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({ activePath: String })
const emit = defineEmits(['play'])

const files = ref([])
const currentPath = ref('')
const loading = ref(false)
const error = ref(null)

const breadcrumbs = computed(() => {
  if (!currentPath.value) return []
  const parts = currentPath.value.split('/').filter(Boolean)
  return parts.map((name, i) => ({
    name,
    path: '/' + parts.slice(0, i + 1).join('/')
  }))
})

async function loadFiles(path) {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`/api/files?path=${encodeURIComponent(path)}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    files.value = await res.json()
    currentPath.value = path
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function navigateTo(path) {
  loadFiles(path)
}

function onItemClick(file) {
  if (file.type === 'folder') {
    loadFiles(file.path)
  } else {
    emit('play', { track: file, files: files.value.filter(f => f.type === 'file') })
  }
}

function formatSize(bytes) {
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => loadFiles(''))
</script>

<style scoped>
.file-list-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.crumb {
  color: var(--text-secondary);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}

.crumb:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.crumb-sep {
  color: var(--border);
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.file-list::-webkit-scrollbar {
  width: 6px;
}
.file-list::-webkit-scrollbar-track {
  background: transparent;
}
.file-list::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  cursor: pointer;
  transition: background 0.1s;
  border-bottom: 1px solid transparent;
}

.file-item:hover {
  background: var(--bg-hover);
}

.file-item.active {
  background: var(--bg-active);
  border-left: 3px solid var(--accent);
  padding-left: 17px;
}

.file-item.active .file-name {
  color: var(--accent-light);
}

.file-icon {
  display: flex;
  align-items: center;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.file-item.folder .file-icon {
  color: #f0c040;
}

.file-name {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.state-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex: 1;
  color: var(--text-secondary);
  font-size: 14px;
}

.state-message.error {
  color: #e05555;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
