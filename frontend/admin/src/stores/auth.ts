import { defineStore } from 'pinia';
import type { LoginResp } from '../types';

interface AuthState {
  token: string;
  nickname: string;
  role: number;
}

export const useAuthStore = defineStore('admin-auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('at_admin_token') ?? '',
    nickname: localStorage.getItem('at_admin_nick') ?? '',
    role: Number(localStorage.getItem('at_admin_role') ?? 0)
  }),
  getters: {
    isLogin: (s) => !!s.token
  },
  actions: {
    setLogin(r: LoginResp) {
      this.token = r.token;
      this.nickname = r.nickname;
      this.role = r.role;
      localStorage.setItem('at_admin_token', r.token);
      localStorage.setItem('at_admin_nick', r.nickname);
      localStorage.setItem('at_admin_role', String(r.role));
    },
    logout() {
      this.token = '';
      this.nickname = '';
      this.role = 0;
      localStorage.removeItem('at_admin_token');
      localStorage.removeItem('at_admin_nick');
      localStorage.removeItem('at_admin_role');
    }
  }
});