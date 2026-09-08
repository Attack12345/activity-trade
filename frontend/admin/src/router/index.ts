import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/activities',
    children: [
      { path: 'activities', name: 'activities', component: () => import('../views/activities/ActivityListView.vue') },
      { path: 'activities/:id/stock', name: 'stock', component: () => import('../views/activities/StockView.vue'), props: true },
      { path: 'settle', name: 'settle', component: () => import('../views/settle/SettleView.vue') },
      { path: 'metrics', name: 'metrics', component: () => import('../views/metrics/MetricsView.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/activities' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const token = localStorage.getItem('at_admin_token');
  const role = Number(localStorage.getItem('at_admin_role') ?? 0);
  if (to.name !== 'login' && !token) return { name: 'login', query: { redirect: to.fullPath } };
  if (to.name === 'login' && token) return { name: 'activities' };
  if (to.name !== 'login' && role !== 1) {
    localStorage.removeItem('at_admin_token');
    localStorage.removeItem('at_admin_nick');
    localStorage.removeItem('at_admin_role');
    return { name: 'login' };
  }
  return true;
});

export default router;