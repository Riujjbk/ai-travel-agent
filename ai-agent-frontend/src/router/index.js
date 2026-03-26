import { createRouter, createWebHistory } from "vue-router";
import Home from "../pages/Home.vue";
import ChatPage from "../pages/ChatPage.vue";

const API_MODE_TRAVEL = "travel";
const API_MODE_MANUS = "manus";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: Home
    },
    {
      path: "/travel",
      name: "travel",
      component: ChatPage,
      props: { mode: API_MODE_TRAVEL }
    },
    {
      path: "/manus",
      name: "manus",
      component: ChatPage,
      props: { mode: API_MODE_MANUS }
    },
    {
      path: "/:pathMatch(.*)*",
      redirect: "/"
    }
  ]
});

export default router;

