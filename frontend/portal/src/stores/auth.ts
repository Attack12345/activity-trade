import { defineStore } from 'pinia';
import type { LoginResp } from '../types';

interface AuthState {
  token: string;
  nickname: string;
  role: number;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('at_token') ?? '',
    nickname: localStorage.getItem('at_nick') ?? '',
    role: Number(localStorage.getItem('at_role') ?? 0)
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.role === 1
  },
  actions: {
    setLogin(r: LoginResp) {
      this.token = r.token;
      this.nickname = r.nickname;
      this.role = r.role;
      localStorage.setItem('at_token', r.token);
      localStorage.setItem('at_nick', r.nickname);
      localStorage.setItem('at_role', String(r.role));
    },
    logout() {
      this.token = '';
      this.nickname = '';
      this.role = 0;
      localStorage.removeItem('at_token');
      localStorage.removeItem('at_nick');
      localStorage.removeItem('at_role');
    }
  }
});