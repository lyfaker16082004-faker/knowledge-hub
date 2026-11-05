import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': loadEnv(mode, '.', '').VITE_PROXY_TARGET || 'http://localhost:8080' } },
}));