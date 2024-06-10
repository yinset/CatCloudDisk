
import App from './App.vue'
import '@/assets/css/global.css'
import {GlobalVariable} from "@/components/global.js";
import VueRouter from "@/router/index.js";
import ElementPlus from 'element-plus'
import axios from './utils/intercept.js'
import VueAxios from "vue-axios"
// import Vue from 'vue'
import {createApp} from 'vue'
import qs from 'qs'
import 'element-plus/dist/index.css'
const app = createApp(App)
//
// const app = Vue.createApp(App)
app.config.globalProperties.$qs = qs
// app.config.globalProperties.$qs = Qs


app.use(VueRouter)
app.config.globalProperties.$router = VueRouter
app.use(ElementPlus)
app.use(VueAxios,axios)
app.config.globalProperties.$GlobalVariable = GlobalVariable
app.mount('#app')
