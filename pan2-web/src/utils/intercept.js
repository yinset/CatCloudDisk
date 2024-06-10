// 添加请求拦截器
import axios from "axios";
import VueRouter from "@/router/index.js";

axios.interceptors.request.use(
    function (config) {
        // 在发送请求之前添加认证头
        config.headers['token'] = localStorage.getItem("token")
        let tokenUser = localStorage.getItem("tokenUser")
        if(tokenUser !== null){
            config.headers['email'] = JSON.parse(tokenUser).email
        }


        //在XMLHttpRequest不允许设置一些headers，他们会被浏览器自动设置。(安全漏洞)
        // config.headers.set('connection','close')
        // console.log(config.headers.get('connection'))
        return config;
    },
    function (error) {
        // 对请求错误做些什么
        return Promise.reject(error);
    }
);

// 添加响应拦截器
axios.interceptors.response.use(
    function (response) {
        // 对响应数据做些什么
        return response;
    },
    function (error) {
        // 对响应错误做些什么
        if (error.response.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('tokenUser')
            VueRouter.push('/login')
        }
        return Promise.reject(error);
    }
)

//这句话什么意思？
export default axios;