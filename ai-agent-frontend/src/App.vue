<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <div class="logo-mark" />
        <div class="brand-text">AI LvYou</div>
      </RouterLink>

      <!-- Desktop Navigation -->
      <nav class="topnav-desktop">
        <RouterLink class="topnav-link" to="/travel">AI 旅游助手</RouterLink>
        <RouterLink class="topnav-link" to="/manus">AI 超级智能体</RouterLink>
        <RouterLink class="topnav-link" to="/journal">AI 旅游日记</RouterLink>
      </nav>

      <!-- Mobile Navigation -->
      <div class="topnav-mobile">
        <button class="hamburger-btn" @click="toggleMobileMenu" :class="{ 'is-active': isMobileMenuOpen }">
          <span class="line"></span>
          <span class="line"></span>
          <span class="line"></span>
        </button>
        <transition name="mobile-menu-fade">
          <div v-if="isMobileMenuOpen" class="mobile-menu-overlay" @click="closeMobileMenu">
            <nav class="mobile-menu-content">
              <RouterLink class="mobile-nav-link" to="/" @click="closeMobileMenu">首页</RouterLink>
              <RouterLink class="mobile-nav-link" to="/travel" @click="closeMobileMenu">AI 旅游助手</RouterLink>
              <RouterLink class="mobile-nav-link" to="/manus" @click="closeMobileMenu">AI 超级智能体</RouterLink>
              <RouterLink class="mobile-nav-link" to="/journal" @click="closeMobileMenu">AI 旅游日记</RouterLink>
            </nav>
          </div>
        </transition>
      </div>
    </header>

    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { RouterLink, RouterView } from "vue-router";

const isMobileMenuOpen = ref(false);

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
};

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false;
};
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4e5d1 0%, #f9f0e6 55%, #fff7f0 100%);
}

.topbar {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(244, 229, 209, 0.65);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0,0,0,0.04);
  position: sticky;
  top: 0;
  z-index: 100;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: radial-gradient(circle at 30% 30%, #9b7bff 0%, #5c3bff 55%, #3a1cff 100%);
}

.brand-text {
  font-weight: 800;
  font-size: 18px;
  color: #2a2a2a;
  letter-spacing: 0.2px;
}

.topnav-desktop {
  display: flex;
  gap: 18px;
}

.topnav-link {
  color: #3b3b3b;
  text-decoration: none;
  font-weight: 700;
  font-size: 15px;
  padding: 8px 14px;
  border-radius: 10px;
  transition: background 0.2s ease, color 0.2s ease;
}

.topnav-link:hover {
  background: rgba(92, 59, 255, 0.08);
}

.topnav-link.router-link-exact-active {
  color: #5c3bff;
  background: rgba(92, 59, 255, 0.1);
}

.main {
  width: 100%;
}

/* Mobile Navigation Styles */
.topnav-mobile {
  display: none;
}

.hamburger-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.hamburger-btn .line {
  width: 24px;
  height: 3px;
  background: #3b3b3b;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.hamburger-btn.is-active .line:nth-child(1) {
  transform: translateY(8px) rotate(45deg);
}

.hamburger-btn.is-active .line:nth-child(2) {
  opacity: 0;
}

.hamburger-btn.is-active .line:nth-child(3) {
  transform: translateY(-8px) rotate(-45deg);
}

.mobile-menu-overlay {
  position: fixed;
  top: 68px; /* Match topbar height */
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
}

.mobile-menu-content {
  background: #fff7f0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.mobile-nav-link {
  color: #3b3b3b;
  text-decoration: none;
  font-size: 18px;
  font-weight: 700;
  padding: 12px 16px;
  border-radius: 12px;
  transition: background 0.2s ease;
}

.mobile-nav-link:hover, .mobile-nav-link.router-link-exact-active {
  background: rgba(92, 59, 255, 0.08);
  color: #5c3bff;
}

.mobile-menu-fade-enter-active, .mobile-menu-fade-leave-active {
  transition: opacity 0.3s ease;
}

.mobile-menu-fade-enter-from, .mobile-menu-fade-leave-to {
  opacity: 0;
}

/* Media Query for Responsiveness */
@media (max-width: 768px) {
  .topnav-desktop {
    display: none;
  }
  .topnav-mobile {
    display: block;
  }
  .topbar {
    padding: 0 16px;
  }
}
</style>
