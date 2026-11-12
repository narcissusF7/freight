import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * 司机端 H5 调试配置。
 *
 * - 5174：避开后台前端 5173，两个端可同时起，联调时一眼能分辨；
 * - /api 代理到 8088：H5 与 APP 共用同一套 api 路径，不需要为端写两套 baseUrl；
 * - 真机/APP 打包时通过 VITE_API_BASE 指向网关域名（见 .env.example）。
 */
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5174,
    host: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8088',
        changeOrigin: true
      }
    }
  }
})
