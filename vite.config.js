
import { defineConfig } from 'vite';
import { resolve } from 'node:path';

export default defineConfig({
  build: { rollupOptions: { input: { owner: resolve(import.meta.dirname, 'index.html'), customer: resolve(import.meta.dirname, 'customer.html') } } },
  esbuild: {
    jsx: 'automatic'
  },

  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,

    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
});
