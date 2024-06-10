import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { visualizer } from 'rollup-plugin-visualizer'
import importToCDN  from "vite-plugin-cdn-import"
// https://vitejs.dev/config/

export default defineConfig({
    plugins: [
        vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
        }),
        Components({
            resolvers: [ElementPlusResolver()],
        }),
        visualizer({
            open: false,
            gzipSize: true, // 收集 gzip 大小并将其显示
            brotliSize: true, // 收集 brotli 大小并将其显示}),
        }),
        importToCDN({
            modules: [
                {
                    name: "vue",
                    var: "Vue",
                    path: "https://cdn.bootcdn.net/ajax/libs/vue/3.4.21/vue.global.prod.min.js",
                },
                {
                  name:"element-plus",
                  var:"ElementPlus",
                  path:"https://cdn.bootcdn.net/ajax/libs/element-plus/2.6.2/index.full.min.js",
                  css:"https://cdn.bootcdn.net/ajax/libs/element-plus/2.6.2/index.css"
                },
                {
                    name:"axios",
                    var:"axios",
                    path:"https://cdn.bootcdn.net/ajax/libs/axios/1.6.8/axios.min.js"
                },
                {
                    name:"vue-router",
                    var:"VueRouter",
                    path:"https://cdn.bootcdn.net/ajax/libs/vue-router/4.3.2/vue-router.global.prod.min.js"
                },
                {
                    name:"qs",
                    var:"qs",
                    path:"https://cdn.bootcdn.net/ajax/libs/qs/6.12.0/qs.min.js"
                },
                {
                    name:"spark-md5",
                    var:"SparkMD5",
                    path:"https://cdn.bootcdn.net/ajax/libs/spark-md5/3.0.2/spark-md5.min.js"
                },
                {
                    name:"element-plus-icons-vue",
                    var:"ElementPlusIconsVue",
                    path:"https://cdn.bootcdn.net/ajax/libs/element-plus-icons-vue/2.3.1/global.iife.min.js"
                }
            ],
        }),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    }
})
