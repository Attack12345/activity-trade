import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('../views/HomeView.vue'), meta: { title: '活动列表' } },
    { path: '/activity/:id', component: () => import('../views/ActivityDetailView.vue'), meta: { title: '活动详情' } },
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { title: '登录' } },
    { path: '/register', component: () => import('../views/RegisterView.vue'), meta: { title: '注册' } },
    { path: '/orders', component: () => import('../views/OrdersView.vue'), meta: { title: '我的订单', auth: true } },
    { path: '/rights', component: () => import('../views/RightsView.vue'), meta: { title: '我的权益', auth: true } }
  ]
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.auth && !auth.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;