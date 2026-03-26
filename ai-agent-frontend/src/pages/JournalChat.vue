<template>
  <div class="chat-page journal-theme">
    <!-- Consolidated Header -->
    <header class="chat-header-main">
      <div class="header-left">
        <div class="mode-info">
          <div class="mode-badge mode-journal" />
          <h1 class="title-text">AI 旅游日记</h1>
        </div>
      </div>

      <div class="header-right">
        <div v-if="!isMobile" class="chat-meta">
          <div class="meta-item">
            <span class="meta-label">会话 ID:</span>
            <span class="meta-value">{{ chatId }}</span>
          </div>
          <button class="btn-copy" type="button" @click="copyChatId" title="复制 ID">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z" fill="currentColor"/>
            </svg>
          </button>
        </div>
        <div v-if="!isMobile" class="journal-date-wrap">
          <span class="date-icon">📅</span>
          <span class="date-text">{{ currentDate }}</span>
        </div>
      </div>
    </header>

    <!-- Main Chat Area -->
    <main class="chat-container-inner">
      <div class="chat-body">
        <div ref="scrollEl" class="messages" role="log" aria-live="polite">
          <!-- Empty State -->
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">📖</div>
            <h3>开始你的旅行日记</h3>
            <p>告诉 AI 你的旅行见闻，或者让它帮你规划一次难忘的旅程。</p>
          </div>

          <div
            v-for="m in messages"
            :key="m.id"
            class="message-row"
            :class="m.role === 'user' ? 'is-user' : 'is-ai'"
          >
            <div class="avatar-wrap">
              <div class="avatar" :class="m.role === 'user' ? 'avatar-user' : 'avatar-ai'">
                <span>{{ m.role === 'user' ? '🖋️' : '🗺️' }}</span>
              </div>
            </div>
            
            <div class="bubble-group">
              <div 
                class="bubble journal-bubble" 
                :class="[
                  m.role === 'user' ? 'bubble-user' : 'bubble-ai',
                  m.isThought ? 'bubble-thought' : ''
                ]"
              >
                <div v-if="m.isThought" class="thought-header">
                  <span class="thought-icon">⚙️</span>
                  <span class="thought-label">记录步骤</span>
                </div>
                <div class="bubble-content markdown-body">
                  <template v-if="m.content">
                    <div v-html="renderMarkdown(m.content)"></div>
                  </template>
                  <template v-else>
                    <div v-if="m.role === 'assistant' && m.streaming" class="loading-animation">
                      <span class="writing-loader">✍️</span>
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
          placeholder="记录这一刻的旅行心情..."
          rows="1"
          ref="textareaEl"
          :disabled="isStreaming"
          @input="adjustTextareaHeight"
          @keydown.enter.exact.prevent="onSend"
        />
        <div class="footer-actions">
          <button class="btn-send is-journal" type="button" :disabled="!canSend" @click="onSend">
            <span class="send-icon">📜</span>
            <span v-if="!isMobile" class="send-text">记录</span>
          </button>
          <button
            v-if="isStreaming"
            class="btn-stop"
            type="button"
            @click="cancelStream"
            title="停止记录"
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

// 辅助函数：判断是否为“思考/步骤”内容
const isMessageThought = (content) => {
  if (!content) return false;
  // 匹配关键词：Step, 工具, 结果, 或者看起来像 JSON 的原始数据
  const keywords = ["Step", "工具", "结果", "Thought:", "Observation:"];
  const isKeywordMatch = keywords.some(k => content.includes(k));
  const isJsonLike = content.trim().startsWith("{") && content.trim().endsWith("}");
  return isKeywordMatch || isJsonLike;
};

const API_BASE_URL = "/api";

const currentDate = ref("");
const textareaEl = ref(null);
const isMobile = ref(window.innerWidth <= 768);

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
};

function updateDate() {
  const now = new Date();
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
}

function adjustTextareaHeight() {
  const el = textareaEl.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = (el.scrollHeight) + 'px';
}

onMounted(() => {
  window.addEventListener('resize', handleResize);
  updateDate();
  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
  });
  adjustTextareaHeight();
});

function createChatId() {
  if (typeof crypto !== "undefined" && crypto && crypto.randomUUID) return crypto.randomUUID();
  return `journal_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`;
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
  // 使用后端指定的接口地址
  return `${API_BASE_URL}/journal/chat?message=${encodedMessage}&chatId=${encodedChatId}`;
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

  let isFirstStep = true;

  try {
    await streamChatWithFetch(
      url,
      (token) => {
        if (isFirstStep) {
          assistantMsg.content = token;
          assistantMsg.isThought = isMessageThought(token);
          assistantMsg.streaming = false;
          isFirstStep = false;
        } else {
          const stepMsg = {
            id: createMessageId(),
            role: "assistant",
            content: token,
            isThought: isMessageThought(token),
            streaming: false,
            done: false,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          };
          messages.value.push(stepMsg);
        }
      },
      abortController.signal
    );
    assistantMsg.done = true;
  } catch (err) {
    if (String(err?.name || err) === "AbortError") {
      const stopMsg = {
        id: createMessageId(),
        role: "assistant",
        content: "(已停止记录)",
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      messages.value.push(stopMsg);
    } else {
      const errorMsg = {
        id: createMessageId(),
        role: "assistant",
        content: `[错误] ${err?.message || "stream failed"}`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      messages.value.push(errorMsg);
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

/* Journal Theme */
.journal-theme {
  --journal-main: #8B5E3C;
  --journal-paper: #FFF9F0;
  --journal-ink: #2C1810;
  --journal-accent: #D4A373;
  --journal-light: #FDF5E6;
  background-color: var(--journal-paper);
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
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 3px double var(--journal-accent);
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

.mode-badge.mode-journal { background: var(--journal-main); }

.title-text {
  font-size: 18px;
  font-weight: 800;
  color: var(--journal-ink);
  margin: 0;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--journal-light);
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid var(--journal-accent);
}

.meta-label, .meta-value {
  font-size: 12px;
  font-family: ui-monospace, monospace;
  color: var(--journal-main);
}

.btn-copy {
  border: none; background: transparent; color: var(--journal-accent); cursor: pointer; padding: 4px; display: flex;
}

.journal-date-wrap {
  display: flex; align-items: center; gap: 6px; font-weight: 700; color: var(--journal-main);
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--journal-main);
  text-align: center;
  opacity: 0.6;
}

.empty-icon { font-size: 64px; margin-bottom: 16px; }

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
  border: 2px solid var(--journal-accent);
}

.avatar-user { border-color: var(--journal-main); background: var(--journal-light); }
.avatar-ai { border-color: var(--journal-accent); background: white; }

.bubble-group { display: flex; flex-direction: column; gap: 8px; }
.message-row.is-user .bubble-group { align-items: flex-end; }

.bubble {
  padding: 14px 18px; border-radius: 18px; font-size: 15px; line-height: 1.6; font-weight: 500;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03); word-break: break-word;
}

.journal-bubble {
  border: 1px solid rgba(0,0,0,0.05);
}

.bubble-user { 
  background: var(--journal-main); 
  color: white; 
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 15px rgba(139, 94, 60, 0.2);
}

.bubble-ai { 
  background: white; 
  color: var(--journal-ink); 
  border-bottom-left-radius: 4px; 
  border-left: 4px solid var(--journal-accent);
}

/* Thinking/Step Style */
.bubble-thought {
  background: var(--journal-light) !important;
  border: 1px dashed var(--journal-accent) !important;
  color: var(--journal-main) !important;
  font-size: 13px !important;
  padding: 10px 14px !important;
  box-shadow: none !important;
}

.thought-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-weight: 800;
  color: var(--journal-accent);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.thought-icon {
  font-size: 14px;
}

.bubble-thought :deep(pre), .bubble-thought :deep(code) {
  background: rgba(0,0,0,0.03);
  font-size: 12px;
}

/* Markdown Styles */
.markdown-body :deep(p) { margin-top: 0; margin-bottom: 12px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(strong) { font-weight: 800; }

.markdown-body :deep(table) {
  width: 100%; border-collapse: collapse; margin-bottom: 16px; font-size: 14px;
}

.markdown-body :deep(th), .markdown-body :deep(td) {
  padding: 8px 12px; border: 1px solid rgba(0, 0, 0, 0.1);
}

.bubble-ai.markdown-body :deep(th) { background: var(--journal-light); }

.message-info { font-size: 12px; color: var(--journal-accent); font-weight: 600; }

/* Footer */
.chat-footer-main {
  background: var(--journal-light); border-top: 2px solid var(--journal-accent);
  padding: 16px 24px; z-index: 10;
}

.input-area {
  max-width: 900px; margin: 0 auto; background: white; border: 1px solid var(--journal-accent);
  border-radius: 16px; padding: 8px;
  display: flex; align-items: flex-end; gap: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.input-area:focus-within { border-color: var(--journal-main); box-shadow: 0 0 0 3px rgba(139, 94, 60, 0.1); }

.input {
  flex: 1; border: none; background: transparent; padding: 10px; font-size: 15px; font-weight: 500;
  color: var(--journal-ink); outline: none; resize: none; max-height: 150px; min-height: 24px;
}

.footer-actions { display: flex; align-items: center; gap: 8px; }

.btn-send {
  background: var(--journal-main); color: white; border: none; padding: 8px 16px; border-radius: 12px;
  font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 6px;
  transition: all 0.2s;
}

.btn-send:disabled { background: #dee2e6; cursor: not-allowed; }
.btn-send:not(:disabled):hover { background: var(--journal-accent); transform: translateY(-1px); }

.btn-stop {
  width: 36px; height: 36px; border-radius: 12px; border: 1px solid #ff6b6b; background: white;
  color: #ff6b6b; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 16px;
}

/* Animations */
.writing-loader { display: inline-block; animation: writing 1.5s infinite; }
@keyframes writing { 0%, 100% { transform: translateY(0) rotate(0); } 50% { transform: translateY(-5px) rotate(10deg); } }

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
}
</style>
