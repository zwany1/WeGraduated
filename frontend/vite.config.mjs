import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    port: 5173,
    // 禁用开发期资源缓存: 普通刷新(F5)必然加载最新代码, 不再需要强制刷新/清缓存
    headers: {
      'Cache-Control': 'no-store'
    },
    proxy: {
      '/api': {
        target: 'http://localhost:13355',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      maxParallelFileOps: 4
    }
  }
})
