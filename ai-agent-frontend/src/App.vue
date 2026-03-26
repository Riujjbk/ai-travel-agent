<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <div class="logo-mark" />
        <div class="brand-text">AI LvYou</div>
      </RouterLink>

      <nav class="topnav">
        <RouterLink class="topnav-link" to="/travel">AI 旅游助手</RouterLink>
        <RouterLink class="topnav-link" to="/manus">AI 超级智能体</RouterLink>
      </nav>
    </header>

    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from "vue-router";
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
  padding: 0 20px;
  background: rgba(244, 229, 209, 0.55);
  backdrop-filter: blur(10px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: radial-gradient(circle at 30% 30%, #9b7bff 0%, #5c3bff 55%, #3a1cff 100%);
}

.brand-text {
  font-weight: 800;
  color: #2a2a2a;
  letter-spacing: 0.2px;
}

.topnav {
  display: flex;
  gap: 18px;
}

.topnav-link {
  color: #3b3b3b;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  padding: 8px 10px;
  border-radius: 10px;
}

.topnav-link:hover {
  background: rgba(92, 59, 255, 0.08);
}

.main {
  /* Remove padding from main to allow full-page sections */
  width: 100%;
}
</style>

