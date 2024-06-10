import {createRouter, createWebHistory} from 'vue-router'
import login from "@/views/login.vue";
import register from "@/views/register.vue";
import Layout from "@/views/layout.vue";
import document from "@/views/document.vue"
import recycle from "@/views/recycle.vue";

const routes = [
    {
        path: "/",
        redirect: "/document",
        name: "layout",
        component: Layout,
        children: [
            {
                path: "/document",
                name: "文件列表",
                component: document
            },
            {
                path: "/recycle",
                name: "回收站",
                component: recycle
            }
        ]
    },
    {
        path: "/login",
        name: "登录",
        component: login
    },
    {
        path: "/register",
        name: "注册",
        component: register
    },
]


const VueRouter = createRouter({
    routes,
    history: createWebHistory(import.meta.env.BASE_URL)
})

VueRouter.beforeEach(((to, from, next) => {
    if (to.path === '/login' || to.path === '/register') {
        next();
    } else {
        let token = localStorage.getItem('token')
        let tokenUser = localStorage.getItem('tokenUser')
        if (token === null || token === '' || token === "undefined" || tokenUser === null || tokenUser === '' || tokenUser === "undefined") {
            next('/login')
        } else {
            next()
        }
    }
}))

export default VueRouter
