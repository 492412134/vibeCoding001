import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    port: 10990,
    proxy: {
      // 将 /api 开头的请求代理到 Gateway
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, ''), // 如果需要去掉 /api 前缀
      },
      // 健康检查代理
      '/actuator': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
  }
})
