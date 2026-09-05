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
        changeOrigin: true,
        // 后端未启动/重启窗口: 返回明确的 503, 避免前端看到无意义的 500
        configure: (proxy) => {
          proxy.on('error', (err, req, res) => {
            if (res && !res.headersSent) {
              res.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8' })
            }
            if (res && res.end) {
              res.end(JSON.stringify({ code: 503, message: '后端服务未启动或正在重启，请稍后重试 (' + err.code + ')' }))
            }
          })
        }
      }
    }
  },
  build: {
    rollupOptions: {
      maxParallelFileOps: 4,
      output: {
        // 重组件分包: 改善缓存命中与首屏并行加载
        manualChunks: {
          'vendor-element-plus': ['element-plus'],
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-docx': ['docx-preview']
        }
      }
    }
  }
})
