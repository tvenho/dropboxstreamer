<template>
  <div class="app">
    <header class="app-header">
      <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="var(--accent)">
        <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
      </svg>
      <span>Dropbox Streamer</span>
    </header>

    <div class="app-body">
      <FileList @play="onPlay" />
    </div>

    <AudioPlayer :track="currentTrack" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FileList from './components/FileList.vue'
import AudioPlayer from './components/AudioPlayer.vue'

const currentTrack = ref(null)

function onPlay(track) {
  currentTrack.value = track
}
</script>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.3px;
  flex-shrink: 0;
}

.app-body {
  flex: 1;
  overflow: hidden;
  padding-bottom: var(--player-height);
}
</style>
