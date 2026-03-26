<template>
  <div class="chat-page" :class="['mode-' + mode, mode === 'travel' ? 'travel-theme' : '']">
    <!-- Travel Header with Virtual Location -->
    <div v-if="mode === 'travel'" class="travel-location-bar">
      <div class="location-icon">📍</div>
      <div class="location-text">{{ currentTravelLocation }}</div>
      <div class="travel-clock">{{ currentTime }}</div>
    </div>

    <div class="chat-header">
      <div class="chat-title">
        <div class="mode-badge" :class="modeClass" />
        <div class="title-text">
          {{ mode === "travel" ? "AI 旅游助手" : "AI 超级智能体" }}
        </div>
      </div>

      <div class="chat-meta">
        <div class="meta-item">
          <span class="meta-label">Chat ID:</span>
          <span class="meta-value">{{ chatId }}</span>
        </div>
        <button class="btn-ghost" type="button" @click="copyChatId">
          复制
        </button>
      </div>
    </div>

    <div class="chat-body">
      <div ref="scrollEl" class="messages" role="log" aria-live="polite">
        <div
          v-for="m in messages"
          :key="m.id"
          class="message-row"
          :class="m.role === 'user' ? 'is-user' : 'is-ai'"
        >
          <div class="avatar" :class="m.role === 'user' ? 'avatar-user' : 'avatar-ai'">
             <span v-if="mode === 'travel'">{{ m.role === 'user' ? '📷' : '🧳' }}</span>
          </div>
          <div class="bubble-container">
            <div class="bubble" :class="[m.role === 'user' ? 'bubble-user' : 'bubble-ai', mode === 'travel' ? 'travel-bubble' : '']">
              <div class="bubble-content">
                <template v-if="m.content">
                  {{ m.content }}
                </template>
                <template v-else>
                  <div v-if="m.role === 'assistant' && m.streaming" class="loading-animation">
                    <span v-if="mode === 'travel'" class="plane-loader">✈️</span>
                    <span v-else class="placeholder">思考中...</span>
                  </div>
                </template>
              </div>
            </div>
            <div v-if="mode === 'travel'" class="message-time">
               <span class="clock-icon">🕒</span> {{ m.timestamp || '12:00' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-footer" :class="mode === 'travel' ? 'diary-style' : ''">
      <div v-if="mode === 'travel'" class="diary-binding"></div>
      <textarea
        v-model="inputText"
        class="input"
        :placeholder="mode === 'travel' ? '记下你的旅行计划...' : '请输入你的消息，然后回车发送（Shift+Enter 换行）'"
        rows="3"
        :disabled="isStreaming"
        @keydown.enter.exact.prevent="onSend"
      />
      <div class="footer-actions">
        <button class="btn-primary" type="button" :disabled="!canSend" @click="onSend">
          <template v-if="mode === 'travel'">
            <span class="send-icon">🚀</span> 出发
          </template>
          <template v-else>
            {{ isStreaming ? "发送中..." : "发送" }}
          </template>
        </button>
        <button
          class="btn-ghost"
          type="button"
          :disabled="!isStreaming"
          @click="cancelStream"
        >
          停止
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch, onMounted } from "vue";
import axios from "axios";

const props = defineProps({
  mode: {
    type: String,
    required: true,
    validator: (v) => v === "travel" || v === "manus"
  }
});

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8102/api";

// Travel related refs
const currentTravelLocation = ref("巴黎 · 咖啡馆模式");
const currentTime = ref("");

function updateTime() {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

onMounted(() => {
  updateTime();
  const timer = setInterval(updateTime, 60000);
  onBeforeUnmount(() => clearInterval(timer));
});

function createChatId() {
  if (typeof crypto !== "undefined" && crypto && crypto.randomUUID) return crypto.randomUUID();
  return `chat_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`;
}

function createMessageId() {
  return `m_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`;
}

const chatId = ref(createChatId());
const inputText = ref("");
const isStreaming = ref(false);
const scrollEl = ref(null);

const messages = ref([]);

const modeClass = computed(() => (props.mode === "travel" ? "mode-travel" : "mode-manus"));

const canSend = computed(() => !isStreaming.value && inputText.value.trim().length > 0);

function scrollToBottom() {
  const el = scrollEl.value;
  if (!el) return;
  el.scrollTop = el.scrollHeight;
}

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    scrollToBottom();
  }
);

let abortController = null;

function buildSseUrl(message, chatIdValue) {
  const encodedMessage = encodeURIComponent(message);
  const encodedChatId = encodeURIComponent(chatIdValue);

  if (props.mode === "travel") {
    return `${API_BASE_URL}/ai/travel/chat/sse?message=${encodedMessage}&chatId=${encodedChatId}`;
  }
  return `${API_BASE_URL}/ai/manus/chat?message=${encodedMessage}&chatId=${encodedChatId}`;
}

async function streamChatWithFetch(url, onData, signal) {
  const res = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "text/event-stream"
    },
    signal
  });

  if (!res.ok) {
    throw new Error(`SSE request failed: ${res.status} ${res.statusText}`);
  }

  const body = res.body;
  if (!body) {
    throw new Error("SSE response has no body");
  }

  const reader = body.getReader();
  const decoder = new TextDecoder("utf-8");

  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    buffer = buffer.replace(/\r/g, "");

    let sepIndex = buffer.indexOf("\n\n");
    while (sepIndex !== -1) {
      const rawEvent = buffer.slice(0, sepIndex);
      buffer = buffer.slice(sepIndex + 2);
      sepIndex = buffer.indexOf("\n\n");

      const lines = rawEvent.split("\n");
      const dataLines = [];
      for (const line of lines) {
        if (line.startsWith("data:")) dataLines.push(line.slice(5).trimStart());
      }

      const data = dataLines.length ? dataLines.join("\n") : "";
      if (!data) {
        const trimmed = rawEvent.trim();
        if (trimmed) {
          if (trimmed === "[DONE]" || trimmed === "DONE") return;
          onData(trimmed);
        }
        continue;
      }

      if (data === "[DONE]" || data === "DONE") return;
      onData(data);
    }
  }
}

async function startStream(userMessage) {
  const url = buildSseUrl(userMessage, chatId.value);
  const now = new Date();
  const timestamp = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  const userMsg = {
    id: createMessageId(),
    role: "user",
    content: userMessage,
    timestamp
  };

  const assistantMsg = {
    id: createMessageId(),
    role: "assistant",
    content: "",
    streaming: true,
    done: false,
    timestamp
  };

  messages.value.push(userMsg);
  messages.value.push(assistantMsg);

  isStreaming.value = true;

  abortController = new AbortController();

  try {
    await streamChatWithFetch(
      url,
      (token) => {
        assistantMsg.content += token;
      },
      abortController.signal
    );
    assistantMsg.done = true;
  } catch (err) {
    if (String(err?.name || err) === "AbortError") {
      assistantMsg.content += "\n(已停止)";
    } else {
      assistantMsg.content += `\n[错误] ${err?.message || "stream failed"}`;
    }
  } finally {
    assistantMsg.streaming = false;
    isStreaming.value = false;
    abortController = null;
  }
}

function onSend() {
  const text = inputText.value.trim();
  if (!text || isStreaming.value) return;
  inputText.value = "";
  startStream(text);
}

function cancelStream() {
  if (!isStreaming.value || !abortController) return;
  abortController.abort();
}

async function copyChatId() {
  try {
    await navigator.clipboard.writeText(chatId.value);
  } catch {
    // Fallback: do nothing silently
  }
}

onBeforeUnmount(() => {
  if (abortController) abortController.abort();
});
</script>

<style scoped>
/* Base Layout */
.chat-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 16px 20px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 68px);
  transition: all 0.5s ease;
}

/* Travel Theme - Color Palette */
.travel-theme {
  --travel-main: #E6D2B5; /* 沙色 */
  --travel-forest: #2E5A3D; /* 森林绿 */
  --travel-sky: #87CEEB; /* 天空蓝 */
  --travel-orange: #FF8C42; /* 行李箱橙 */
  --travel-paper: #F5F1E6; /* 地图纸黄 */
  
  background-color: var(--travel-paper);
  background-image: 
    radial-gradient(circle at 2px 2px, rgba(46, 90, 61, 0.05) 1px, transparent 0);
  background-size: 40px 40px;
}

/* Travel Virtual Location Bar */
.travel-location-bar {
  background: white;
  margin: 10px 0;
  padding: 8px 20px;
  border-radius: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  font-weight: 800;
  color: var(--travel-forest);
  border: 2px solid var(--travel-main);
}

.travel-clock {
  margin-left: auto;
  font-family: 'Courier New', Courier, monospace;
  background: var(--travel-forest);
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
}

.chat-header {
  margin: 6px 0 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 18px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(0,0,0,0.05);
}

.travel-theme .chat-header {
  border: 2px solid var(--travel-main);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mode-badge {
  width: 14px;
  height: 28px;
  border-radius: 10px;
}

.mode-badge.mode-travel {
  background: linear-gradient(180deg, #FF8C42, #E6D2B5);
}

.mode-badge.mode-manus {
  background: linear-gradient(180deg, #48C9B0, #5C3BFF);
}

.title-text {
  font-weight: 900;
  color: #191919;
  font-size: 18px;
}

.chat-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.meta-item {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-label {
  font-weight: 700;
  color: rgba(0, 0, 0, 0.4);
  font-size: 12px;
}

.meta-value {
  font-weight: 800;
  color: rgba(0, 0, 0, 0.6);
  font-family: monospace;
  font-size: 11px;
}

/* Chat Body & Messages */
.chat-body {
  flex: 1;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  padding: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.travel-theme .chat-body {
  background: rgba(245, 241, 230, 0.4);
  border: 2px solid var(--travel-main);
  /* Map Line Pattern Background */
  background-image: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M10 10 Q 50 10 50 50 T 90 90' stroke='%23E6D2B5' fill='transparent' stroke-width='0.5'/%3E%3Cpath d='M90 10 Q 50 10 50 50 T 10 90' stroke='%23E6D2B5' fill='transparent' stroke-width='0.5'/%3E%3C/svg%3E");
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.message-row {
  display: flex;
  gap: 14px;
  margin: 20px 0;
  align-items: flex-start;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.avatar-user {
  background: white;
  border: 2px solid #5C3BFF;
}

.avatar-ai {
  background: white;
  border: 2px solid #48C9B0;
}

.travel-theme .avatar-user {
  border-color: var(--travel-orange);
}

.travel-theme .avatar-ai {
  border-color: var(--travel-forest);
}

.bubble-container {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message-row.is-user .bubble-container {
  align-items: flex-end;
}

.bubble {
  border-radius: 20px;
  padding: 14px 18px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.04);
  position: relative;
}

.bubble-user {
  background: #5C3BFF;
  color: white;
}

.bubble-ai {
  background: white;
  color: #1a1a1a;
}

/* Travel Bubble Styles */
.travel-bubble.bubble-ai {
  background: var(--travel-forest);
  color: white;
  border-radius: 12px 24px 24px 12px;
  border: 4px solid #3d7a52; /* Suitcase look */
}

.travel-bubble.bubble-user {
  background: var(--travel-orange);
  color: white;
  border-radius: 24px 12px 12px 24px;
  border: 4px solid #ff7a21; /* Camera look */
}

.bubble-content {
  font-weight: 600;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.4);
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 700;
}

/* Plane Loader Animation */
.plane-loader {
  display: inline-block;
  animation: plane-fly 2s infinite linear;
}

@keyframes plane-fly {
  0% { transform: translateX(-20px); opacity: 0; }
  50% { transform: translateX(0); opacity: 1; }
  100% { transform: translateX(20px); opacity: 0; }
}

/* Chat Footer & Input */
.chat-footer {
  margin-top: 16px;
  background: white;
  border-radius: 24px;
  padding: 16px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.03);
  position: relative;
}

.diary-style {
  background: var(--travel-paper);
  border: 2px solid var(--travel-main);
  border-radius: 8px;
}

.diary-binding {
  position: absolute;
  top: 10px;
  left: -8px;
  bottom: 10px;
  width: 16px;
  background-image: radial-gradient(circle, #888 2px, transparent 2px);
  background-size: 100% 20px;
  z-index: 2;
}

.input {
  width: 100%;
  border: none;
  background: rgba(0,0,0,0.03);
  border-radius: 12px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  outline: none;
  transition: all 0.3s ease;
}

.travel-theme .input {
  background: white;
  border-bottom: 2px dashed var(--travel-main);
  border-radius: 0;
  font-family: 'Comic Sans MS', cursive;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.btn-primary {
  background: #5C3BFF;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.3s ease;
}

.travel-theme .btn-primary {
  background: var(--travel-forest);
  border-radius: 30px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(92, 59, 255, 0.3);
}

.travel-theme .btn-primary:hover {
  background: var(--travel-orange);
  box-shadow: 0 4px 12px rgba(255, 140, 66, 0.3);
}

.btn-ghost {
  background: transparent;
  border: 1px solid rgba(0,0,0,0.1);
  padding: 10px 16px;
  border-radius: 14px;
  font-weight: 700;
  cursor: pointer;
}

.travel-theme .btn-ghost {
  color: var(--travel-forest);
  border-color: var(--travel-main);
}
</style>
