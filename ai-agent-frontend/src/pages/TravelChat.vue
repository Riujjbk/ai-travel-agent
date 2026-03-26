<template>
  <div class="chat-page travel-theme">
    <!-- Consolidated Header -->
    <header class="chat-header-main">
      <div class="header-left">
        <div class="mode-info">
          <div class="mode-badge mode-travel" />
          <h1 class="title-text">AI 旅游助手</h1>
        </div>
      </div>

      <div class="header-right">
        <div v-if="!isMobile" class="chat-meta">
          <div class="meta-item">
            <span class="meta-label">ID:</span>
            <span class="meta-value">{{ chatId }}</span>
          </div>
          <button class="btn-copy" type="button" @click="copyChatId" title="复制 ID">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z" fill="currentColor"/>
            </svg>
          </button>
        </div>
        <div v-if="!isMobile" class="travel-clock-wrap">
          <span class="clock-icon">🕒</span>
          <span class="clock-text">{{ currentTime }}</span>
        </div>
      </div>
    </header>

    <!-- Main Chat Area -->
    <main class="chat-container-inner">
      <div class="chat-body">
        <div ref="scrollEl" class="messages" role="log" aria-live="polite">
          <div
            v-for="m in messages"
            :key="m.id"
            class="message-row"
            :class="m.role === 'user' ? 'is-user' : 'is-ai'"
          >
            <div class="avatar-wrap">
              <div class="avatar" :class="m.role === 'user' ? 'avatar-user' : 'avatar-ai'">
                <span>{{ m.role === 'user' ? '📷' : '🧳' }}</span>
              </div>
            </div>
            
            <div class="bubble-group">
              <div class="bubble travel-bubble" :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">
                <div class="bubble-content markdown-body">
                  <template v-if="m.content">
                    <div v-html="renderMarkdown(m.content)"></div>
                  </template>
                  <template v-else>
                    <div v-if="m.role === 'assistant' && m.streaming" class="loading-animation">
                      <span class="plane-loader">✈️</span>
                    </div>
                  </template>
                </div>
              </div>
              <div class="message-info">
                <span class="timestamp">{{ m.timestamp }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Chat Footer -->
    <footer class="chat-footer-main diary-style">
      <div class="diary-binding"></div>
      <div class="input-area">
        <textarea
          v-model="inputText"
          class="input"
          placeholder="在这里记下你的旅行灵感..."
          rows="1"
          ref="textareaEl"
          :disabled="isStreaming"
          @input="adjustTextareaHeight"
          @keydown.enter.exact.prevent="onSend"
        />
        <div class="footer-actions">
          <button class="btn-send is-travel" type="button" :disabled="!canSend" @click="onSend">
            <span class="send-icon">🚀</span>
            <span v-if="!isMobile" class="send-text">出发</span>
          </button>
          <button
            v-if="isStreaming"
            class="btn-stop"
            type="button"
            @click="cancelStream"
            title="停止生成"
          >
            <span class="stop-icon">■</span>
          </button>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch, onMounted } from "vue";
import MarkdownIt from "markdown-it";

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
});

const renderMarkdown = (content) => {
  return md.render(content);
};

const API_BASE_URL = "/api";

const currentTime = ref("");
const textareaEl = ref(null);
const isMobile = ref(window.innerWidth <= 768);

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
};

function updateTime() {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function adjustTextareaHeight() {
  const el = textareaEl.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = (el.scrollHeight) + 'px';
}

onMounted(() => {
  window.addEventListener('resize', handleResize);
  updateTime();
  const timer = setInterval(updateTime, 60000);
  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
    clearInterval(timer);
  });
  adjustTextareaHeight();
});

function createChatId() {
  if (typeof crypto !== "undefined" && crypto && crypto.randomUUID) return crypto.randomUUID();
  return `travel_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`;
}

function createMessageId() {
  return `m_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`;
}

const chatId = ref(createChatId());
const inputText = ref("");
const isStreaming = ref(false);
const scrollEl = ref(null);
const messages = ref([]);

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
  return `${API_BASE_URL}/travel/chat/sse?message=${encodedMessage}&chatId=${encodedChatId}`;
}

async function streamChatWithFetch(url, onData, signal) {
  const res = await fetch(url, {
    method: "GET",
    headers: { Accept: "text/event-stream" },
    signal
  });

  if (!res.ok) {
    throw new Error(`SSE request failed: ${res.status} ${res.statusText}`);
  }

  const body = res.body;
  if (!body) throw new Error("SSE response has no body");

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
  nextTick(() => adjustTextareaHeight());
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
</script>

<style scoped>
/* Base Layout */
.chat-page {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 68px);
  position: relative;
  overflow: hidden;
}

/* Travel Theme */
.travel-theme {
  --travel-main: #E6D2B5;
  --travel-forest: #2E5A3D;
  --travel-orange: #FF8C42;
  --travel-paper: #F5F1E6;
  background-color: var(--travel-paper);
}

/* Header */
.chat-header-main {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 2px solid var(--travel-main);
  backdrop-filter: blur(10px);
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.mode-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mode-badge {
  width: 8px;
  height: 24px;
  border-radius: 4px;
}

.mode-badge.mode-travel { background: var(--travel-orange); }

.title-text {
  font-size: 18px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f1f3f5;
  padding: 4px 10px;
  border-radius: 8px;
}

.meta-label, .meta-value {
  font-size: 12px;
  font-family: ui-monospace, monospace;
}

.btn-copy {
  border: none; background: transparent; color: #adb5bd; cursor: pointer; padding: 4px; display: flex;
}

.travel-clock-wrap {
  display: flex; align-items: center; gap: 6px; font-family: monospace; font-weight: 700; color: var(--travel-forest);
}

/* Main Chat Area */
.chat-container-inner {
  flex: 1;
  overflow: hidden;
  display: flex;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message-row {
  display: flex;
  gap: 16px;
  max-width: 85%;
}

.message-row.is-user { align-self: flex-end; flex-direction: row-reverse; }
.message-row.is-ai { align-self: flex-start; }

.avatar-wrap { flex-shrink: 0; margin-top: 4px; }

.avatar {
  width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
  font-size: 20px; background: white; box-shadow: 0 4px 10px rgba(0,0,0,0.05);
}

.avatar-user { border: 2px solid var(--travel-orange); }
.avatar-ai { border: 2px solid var(--travel-forest); }

.bubble-group { display: flex; flex-direction: column; gap: 8px; }
.message-row.is-user .bubble-group { align-items: flex-end; }

.bubble {
  padding: 14px 18px; border-radius: 18px; font-size: 15px; line-height: 1.6; font-weight: 500;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03); word-break: break-word;
}

.bubble-user { background: #5C3BFF; color: white; border-bottom-right-radius: 4px; }
.bubble-ai { background: white; color: #1a1a1a; border-bottom-left-radius: 4px; border: 1px solid rgba(0,0,0,0.05); }

.travel-bubble.bubble-ai { background: var(--travel-forest); color: white; border: 3px solid #3d7a52; border-radius: 4px 24px 24px 24px; }
.travel-bubble.bubble-user { background: var(--travel-orange); color: white; border: 3px solid #ff7a21; border-radius: 24px 4px 24px 24px; }

/* Markdown Styles */
.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 12px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 800;
  line-height: 1.25;
}

.markdown-body :deep(h1) { font-size: 1.5em; }
.markdown-body :deep(h2) { font-size: 1.3em; }
.markdown-body :deep(h3) { font-size: 1.1em; }

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  margin-top: 0;
  margin-bottom: 12px;
  padding-left: 20px;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 16px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
}

.bubble-ai.markdown-body :deep(table) {
  background: rgba(0, 0, 0, 0.02);
}

.bubble-user.markdown-body :deep(table) {
  background: rgba(255, 255, 255, 0.1);
}

.markdown-body :deep(th), .markdown-body :deep(td) {
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.travel-bubble.bubble-ai :deep(th), .travel-bubble.bubble-ai :deep(td) {
  border-color: rgba(255, 255, 255, 0.2);
}

.markdown-body :deep(th) {
  font-weight: 800;
  background: rgba(0, 0, 0, 0.05);
}

.travel-bubble.bubble-ai :deep(th) {
  background: rgba(255, 255, 255, 0.1);
}

.markdown-body :deep(hr) {
  height: 1px;
  background: rgba(0, 0, 0, 0.1);
  border: none;
  margin: 16px 0;
}

.travel-bubble.bubble-ai :deep(hr) {
  background: rgba(255, 255, 255, 0.2);
}

.markdown-body :deep(strong) {
  font-weight: 800;
}

.message-info { font-size: 12px; color: rgba(46, 90, 61, 0.6); font-weight: 600; }

/* Footer */
.chat-footer-main {
  background: var(--travel-paper); border-top: 2px solid var(--travel-main);
  padding: 16px 24px; z-index: 10;
}

.input-area {
  max-width: 900px; margin: 0 auto; background: white; border: 2px dashed var(--travel-main);
  border-radius: 16px; padding: 8px;
  display: flex; align-items: flex-end; gap: 8px;
}

.input-area:focus-within { border-color: var(--travel-orange); box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.1); }

.input {
  flex: 1; border: none; background: transparent; padding: 10px; font-size: 15px; font-weight: 500;
  color: #1a1a1a; outline: none; resize: none; max-height: 150px; min-height: 24px;
}

.footer-actions { display: flex; align-items: center; gap: 8px; }

.btn-send {
  background: var(--travel-forest); color: white; border: none; padding: 8px 16px; border-radius: 12px;
  font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 6px;
}

.btn-send:disabled { background: #dee2e6; cursor: not-allowed; }
.btn-send:not(:disabled):hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(255, 140, 66, 0.2); background: var(--travel-orange); }

.btn-stop {
  width: 36px; height: 36px; border-radius: 12px; border: 1px solid #ff6b6b; background: white;
  color: #ff6b6b; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 16px;
}

/* Animations */
.plane-loader { display: inline-block; animation: plane-fly 1.5s infinite ease-in-out; }
@keyframes plane-fly { 0%, 100% { transform: translateX(-15px); opacity: 0; } 50% { transform: translateX(15px); opacity: 1; } }

/* Responsive Media Queries */
@media (max-width: 768px) {
  .chat-header-main { height: 60px; padding: 0 16px; }
  .title-text { font-size: 16px; }
  .chat-body { padding: 16px; }
  .message-row { max-width: 95%; }
  .avatar { width: 36px; height: 36px; }
  .bubble { padding: 12px 16px; font-size: 14px; }
  .chat-footer-main { padding: 12px 16px; }
  .input-area { padding: 6px; }
  .input { padding: 8px; }
  .btn-send { padding: 8px 12px; }
  .send-text { display: none; }
}
</style>
