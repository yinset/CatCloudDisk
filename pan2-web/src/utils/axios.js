import axios from "@/utils/intercept.js";
import {GlobalVariable as $GlobalVariable} from "@/components/global.js";
export function httpPost(url, data = {}, params = {}) {
    return new Promise((resolve, reject) => {
        axios.post($GlobalVariable.baseURL + url, data, params)
            .then(res => {
                resolve(res)
            }).catch(err => {
            reject(err)
        })
    })
}