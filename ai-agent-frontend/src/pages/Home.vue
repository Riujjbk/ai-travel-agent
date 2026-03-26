<template>
  <div class="home-container" @scroll="handleScroll" ref="container">
    <!-- Page 1: Big Title -->
    <section class="section first-page">
      <div 
        class="big-title" 
        :style="{ 
          transform: `scale(${1 + scrollProgress * 3})`, 
          opacity: 1 - scrollProgress * 1.5,
          filter: `blur(${scrollProgress * 10}px)`
        }"
        aria-label="viva la vida"
      >
        viva la vida
      </div>
      
      <!-- Scroll Hint -->
      <div class="scroll-hint" :style="{ opacity: 1 - scrollProgress * 2 }">
        <div class="mouse">
          <div class="wheel"></div>
        </div>
        <div class="arrow"></div>
      </div>
    </section>

    <!-- Page 2: Entry Buttons -->
    <section class="section second-page">
      <div class="cards-wrap" :class="{ 'is-visible': scrollProgress > 0.6 }">
        <h2 class="section-title">选择你的 AI 伙伴</h2>
        <div class="cards">
          <button class="card" type="button" @click="goTravel">
            <div class="card-content">
              <div class="card-icon travel">
                <span class="icon-emoji">✈️</span>
              </div>
              <div class="card-text">
                <div class="card-title">AI 旅游助手</div>
                <div class="card-desc">深度定制的行程规划与建议，让你的旅行更轻松。</div>
              </div>
            </div>
            <div class="card-cta">
              <span>立即体验</span>
              <svg viewBox="0 0 24 24" class="cta-arrow">
                <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2.5" fill="none" />
              </svg>
            </div>
          </button>

          <button class="card" type="button" @click="goManus">
            <div class="card-content">
              <div class="card-icon manus">
                <span class="icon-emoji">🤖</span>
              </div>
              <div class="card-text">
                <div class="card-title">AI 超级智能体</div>
                <div class="card-desc">基于 Manus 引擎的流式交互，极致响应速度。</div>
              </div>
            </div>
            <div class="card-cta">
              <span>立即体验</span>
              <svg viewBox="0 0 24 24" class="cta-arrow">
                <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2.5" fill="none" />
              </svg>
            </div>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const goTravel = () => router.push("/travel");
const goManus = () => router.push("/manus");

const container = ref(null);
const scrollProgress = ref(0);

const handleScroll = (e) => {
  const scrollTop = e.target.scrollTop;
  const height = e.target.clientHeight;
  // Calculate progress of first page scrolling out (0 to 1)
  scrollProgress.value = Math.min(Math.max(scrollTop / height, 0), 1);
};

onMounted(() => {
  if (container.value) {
    handleScroll({ target: container.value });
  }
});
</script>

<style scoped>
.home-container {
  height: calc(100vh - 68px);
  overflow-y: auto;
  scroll-snap-type: y mandatory;
  scroll-behavior: smooth;
  background: linear-gradient(180deg, #f4e5d1 0%, #f9f0e6 55%, #fff7f0 100%);
  scrollbar-width: none; /* Hide scrollbar Firefox */
}

.home-container::-webkit-scrollbar {
  display: none; /* Hide scrollbar Chrome/Safari */
}

.section {
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  scroll-snap-align: start;
  scroll-snap-stop: always;
  overflow: hidden;
  position: relative;
}

.big-title {
  font-size: clamp(86px, 15vw, 220px);
  font-weight: 900;
  letter-spacing: -2px;
  line-height: 1;
  color: transparent;
  background-image: linear-gradient(135deg, #5a3bff 0%, #7a63ff 35%, #4b2dff 70%, #6d56ff 100%);
  background-size: 100% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  font-family: "Comic Sans MS", "Segoe Script", "Brush Script MT", "Apple Chancery", cursive;
  text-shadow:
    0 10px 30px rgba(74, 52, 255, 0.15);
  filter: saturate(1.1);
  transition: transform 0.05s linear, opacity 0.05s linear, filter 0.05s linear;
  will-change: transform, opacity, filter;
  user-select: none;
}

/* Scroll Hint Styles */
.scroll-hint {
  position: absolute;
  bottom: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: opacity 0.3s ease;
}

.mouse {
  width: 24px;
  height: 40px;
  border: 2px solid rgba(90, 59, 255, 0.4);
  border-radius: 12px;
  display: flex;
  justify-content: center;
  padding-top: 6px;
}

.wheel {
  width: 4px;
  height: 8px;
  background: #5a3bff;
  border-radius: 2px;
  animation: scroll-wheel 1.5s infinite;
}

.arrow {
  width: 10px;
  height: 10px;
  border-right: 2px solid rgba(90, 59, 255, 0.4);
  border-bottom: 2px solid rgba(90, 59, 255, 0.4);
  transform: rotate(45deg);
  animation: scroll-arrow 1.5s infinite;
}

@keyframes scroll-wheel {
  0% { transform: translateY(0); opacity: 1; }
  100% { transform: translateY(12px); opacity: 0; }
}

@keyframes scroll-arrow {
  0% { transform: rotate(45deg) translate(-2px, -2px); opacity: 0; }
  50% { opacity: 1; }
  100% { transform: rotate(45deg) translate(2px, 2px); opacity: 0; }
}

/* Second Page Styles */
.section-title {
  font-size: 32px;
  font-weight: 800;
  color: #191919;
  margin-bottom: 48px;
  text-align: center;
}

.cards-wrap {
  width: min(1000px, 92%);
  opacity: 0;
  transform: translateY(60px);
  transition: all 1s cubic-bezier(0.2, 0.9, 0.2, 1);
}

.cards-wrap.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;
}

@media (max-width: 768px) {
  .cards {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .section-title {
    font-size: 24px;
    margin-bottom: 32px;
  }
}

.card {
  background: white;
  border: 1px solid rgba(92, 59, 255, 0.08);
  border-radius: 24px;
  padding: 32px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 24px;
  transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(92, 59, 255, 0.12);
  border-color: rgba(92, 59, 255, 0.2);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.card-icon.travel {
  background: linear-gradient(135deg, #fff1eb 0%, #ace0f9 100%);
}

.card-icon.manus {
  background: linear-gradient(135deg, #fdfcfb 0%, #e2d1c3 100%);
}

.card-title {
  font-size: 22px;
  font-weight: 800;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.card-desc {
  font-size: 15px;
  line-height: 1.6;
  color: #666;
}

.card-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  color: #5c3bff;
  font-weight: 700;
  font-size: 16px;
}

.cta-arrow {
  width: 20px;
  height: 20px;
  transition: transform 0.3s ease;
}

.card:hover .cta-arrow {
  transform: translateX(6px);
}
</style>
