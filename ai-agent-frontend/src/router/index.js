import { createRouter, createWebHistory } from "vue-router";
import Home from "../pages/Home.vue";
import TravelChat from "../pages/TravelChat.vue";
import ManusChat from "../pages/ManusChat.vue";
import JournalChat from "../pages/JournalChat.vue";

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
      component: TravelChat
    },
    {
      path: "/manus",
      name: "manus",
      component: ManusChat
    },
    {
      path: "/journal",
      name: "journal",
      component: JournalChat
    },
    {
      path: "/:pathMatch(.*)*",
      redirect: "/"
    }
  ]
});

export default router;

