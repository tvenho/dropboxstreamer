<template>
  <div class="player" :class="{ active: track }">
    <audio ref="audioEl" @timeupdate="onTimeUpdate" @ended="onEnded" @loadedmetadata="onLoaded" @error="onError" />

    <div class="player-track">
      <div class="track-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1" fill="currentColor"/>
          <line x1="12" y1="2" x2="12" y2="4"/><line x1="12" y1="20" x2="12" y2="22"/>
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
          <line x1="2" y1="12" x2="4" y2="12"/><line x1="20" y1="12" x2="22" y2="12"/>
        </svg>
      </div>
      <div class="track-info">
        <span class="track-name">{{ track ? track.name.replace(/\.mp3$/i, '') : 'Ei kappaletta valittu' }}</span>
        <span class="track-status" v-if="errorMsg">{{ errorMsg }}</span>
        <span class="track-status" v-else-if="loading">Ladataan linkkiä...</span>
      </div>
    </div>

    <div class="player-controls">
      <button class="btn-play" @click="togglePlay" :disabled="!track || loading">
        <svg v-if="!playing" xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
          <polygon points="5 3 19 12 5 21 5 3"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>
        </svg>
      </button>
      <button class="btn-next" @click="emit('ended')" :disabled="!track || loading" title="Seuraava kappale">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <polygon points="5 4 15 12 5 20 5 4"/><rect x="17" y="4" width="2" height="16"/>
        </svg>
      </button>
    </div>

    <div class="player-progress">
      <span class="time">{{ formatTime(currentTime) }}</span>
      <input
        type="range"
        class="progress-bar"
        :value="currentTime"
        :max="duration || 100"
        @input="onSeek"
        :style="{ '--progress': `${duration ? (currentTime / duration) * 100 : 0}%` }"
      />
      <span class="time">{{ formatTime(duration) }}</span>
    </div>

    <div class="player-volume">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
        <path v-if="volume > 0.5" d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/>
        <path v-else-if="volume > 0" d="M15.54 8.46a5 5 0 0 1 0 7.07"/>
      </svg>
      <input
        type="range"
        class="volume-bar"
        min="0"
        max="1"
        step="0.01"
        :value="volume"
        @input="onVolumeChange"
        :style="{ '--progress': `${volume * 100}%` }"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ track: Object })
const emit = defineEmits(['ended'])

const audioEl = ref(null)
const playing = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(0.8)
const loading = ref(false)
const errorMsg = ref(null)

watch(() => props.track, async (newTrack) => {
  if (!newTrack) return
  errorMsg.value = null
  loading.value = true
  playing.value = false

  try {
    const res = await fetch(`/api/stream?path=${encodeURIComponent(newTrack.path)}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()

    const audio = audioEl.value
    audio.src = data.link
    audio.volume = volume.value
    await audio.play()
    playing.value = true
    updateMediaSession(newTrack)
  } catch (e) {
    errorMsg.value = 'Virhe: ' + e.message
  } finally {
    loading.value = false
  }
})

function updateMediaSession(track) {
  if (!('mediaSession' in navigator)) return
  navigator.mediaSession.metadata = new MediaMetadata({
    title: track.name.replace(/\.mp3$/i, '')
  })
  navigator.mediaSession.setActionHandler('play', () => {
    audioEl.value.play()
    playing.value = true
    navigator.mediaSession.playbackState = 'playing'
  })
  navigator.mediaSession.setActionHandler('pause', () => {
    audioEl.value.pause()
    playing.value = false
    navigator.mediaSession.playbackState = 'paused'
  })
  navigator.mediaSession.setActionHandler('nexttrack', () => emit('ended'))
  navigator.mediaSession.playbackState = 'playing'
}

async function togglePlay() {
  const audio = audioEl.value
  if (!audio.src) return

  if (playing.value) {
    audio.pause()
    playing.value = false
    if ('mediaSession' in navigator) navigator.mediaSession.playbackState = 'paused'
  } else {
    await audio.play()
    playing.value = true
    if ('mediaSession' in navigator) navigator.mediaSession.playbackState = 'playing'
  }
}

function onTimeUpdate() {
  currentTime.value = audioEl.value.currentTime
}

function onLoaded() {
  duration.value = audioEl.value.duration
}

function onEnded() {
  playing.value = false
  currentTime.value = 0
  emit('ended')
}

function onError() {
  errorMsg.value = 'Streamaus epäonnistui'
  playing.value = false
}

function onSeek(e) {
  const time = parseFloat(e.target.value)
  audioEl.value.currentTime = time
  currentTime.value = time
}

function onVolumeChange(e) {
  const val = parseFloat(e.target.value)
  volume.value = val
  audioEl.value.volume = val
}

function formatTime(seconds) {
  if (!seconds || isNaN(seconds)) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}
</script>

<style scoped>
.player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--player-height);
  background: var(--bg-secondary);
  border-top: 1px solid var(--border);
  display: grid;
  grid-template-columns: 1fr auto 2fr 1fr;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.player.active {
  opacity: 1;
}

.player-track {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.track-icon {
  color: var(--accent);
  flex-shrink: 0;
}

.track-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.track-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.track-status {
  font-size: 12px;
  color: var(--text-secondary);
}

.player-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn-next {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.btn-next:hover:not(:disabled) {
  color: var(--text-primary);
}

.btn-next:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.btn-play {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, transform 0.1s;
}

.btn-play:hover:not(:disabled) {
  background: var(--accent-light);
}

.btn-play:active:not(:disabled) {
  transform: scale(0.95);
}

.btn-play:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.player-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time {
  font-size: 12px;
  color: var(--text-secondary);
  min-width: 36px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.progress-bar {
  flex: 1;
  background: linear-gradient(
    to right,
    var(--accent) var(--progress),
    var(--border) var(--progress)
  );
}

.player-volume {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.volume-bar {
  width: 80px;
  background: linear-gradient(
    to right,
    var(--accent) var(--progress),
    var(--border) var(--progress)
  );
}
</style>
