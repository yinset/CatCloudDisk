<script>
import {ElMessage} from "element-plus";
import {httpPost} from "@/utils/axios.js";
import {parseTime} from "element-plus/es/components/time-select/src/utils";

export default {
  data() {
    return {
      username: "",
      password: "",
      nickName: "",
      birthday: undefined
    }
  },
  methods: {
    registerCheck() {
      let username = this.username
      let password = this.password
      let nickName = this.nickName
      if (username === "") {
        ElMessage.warning("请填写邮箱/登录名(*^_^*)")
        return false
      } else if (password === "") {
        ElMessage.warning("请填写密码(*^_^*)")
        return false
      } else if (nickName === "") {
        ElMessage.warning("请填写昵称(*^_^*)")
        return false
      } else {
        return true
      }
    },
    async register() {
      if (this.registerCheck) {
        let birthday
        if(this.birthday === undefined || this.birthday === null){
          birthday = undefined
        }else{
          birthday = this.birthday
        }
        let res = await httpPost("/user/register", this.$qs.stringify({
          email: this.username,
          password: this.password,
          nickName: this.nickName,
          birthday: birthday
        }))

        if (res.status === 200) {
          ElMessage.success("注册成功（￣︶￣）↗　")
          sessionStorage.setItem("email", this.username)
          this.$router.push('/login')
        } else if (res.status === 250) {
          ElMessage.error("用户已存在")
        } else {
          ElMessage.error("注册失败，请联系管理员")
        }
      }
    },
    toLogin() {
      window.location.href = "/login"
    },
  }
}
</script>

<template>
  <div id="window">
    <div id="window-introduction">
      <el-text tag="b" size="large">阿猫网盘</el-text>
      <div id="author">
        <p>author:</p>
        <a href="https://github.com/yinset">hui</a>
        <img src="../assets/logo/github-mark.png" id="github-img" alt=""><br/>
        <span>1182552084@qq.com</span>
      </div>
    </div>
    <div id="window-login">
      <p class="blackbgtext">欢迎注册 ✌️</p>
      <el-input type="text" class="inputLogin" placeholder="邮箱/登录名" :maxlength="36"
                v-model.trim="username"></el-input>
      <el-input type="password" class="inputLogin" placeholder="密码" :maxlength="16"
                v-model.trim="password"></el-input>
      <el-input type="text" class="inputLogin" placeholder="昵称" :maxlength="16" v-model.trim="nickName"></el-input>
      <el-date-picker
          type="date"
          placeholder="生日（可选）"
          default-value="2000-01-01"
          v-model="birthday"
          :editable="false"
      />
      <el-button plain id="register-button" @click="register">提交！</el-button>
      <el-link id="register-text" @click="toLogin">已有账号？返回登录</el-link>
    </div>
  </div>


</template>

<style scoped>
#register-text {
  margin: auto;
}

#window-login {
  border-radius: 15px;
}

#github-img {
  width: 30px;
  height: 30px;
}

#author {

}

template {
  margin: 0;
  padding: 0;
}

#window {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  top: 175px
}

#window-introduction {
  width: 400px;
  height: 600px;
  display: grid;
}

#window-login {
  background-color: black;
  width: 400px;
  height: 650px;
  position: relative;
  //top: 200px;
  //right: 200px;
  display: grid;
  align-items: center;
  justify-content: center;
}

.inputLogin {
  height: 30px;
  width: 220px;
  position: relative;
}

.blackbgtext {
  color: white;
}
</style>