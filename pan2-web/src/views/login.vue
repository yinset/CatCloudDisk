<script>
import {ElMessage} from "element-plus";
import {httpPost} from "@/utils/axios.js";

export default {
  mounted() {
    let emailRegister = sessionStorage.getItem("email")
    if (emailRegister !== undefined && emailRegister !== "") {
      this.loginForm.email = emailRegister
    }
  },

  data() {
    return {
      loginForm: {
        email: "",
        password: ""
      },
      loadingFlag: false
    }
  },

  methods: {
    toRegister() {
      this.$router.push('/register')
    },

    loginCheck() {
      if (this.loginForm.email === "") {
        ElMessage.warning("请填写邮箱(*^_^*)")
        return false
      } else if (this.loginForm.password === "") {
        ElMessage.warning("请填写密码(*^_^*)")
        return false
      } else {
        return true
      }
    },

    async login() {
      if (this.loginCheck()) {
        this.loadingFlag = true
        let res = await httpPost("/user/login", this.$qs.stringify({
          email: this.loginForm.email,
          password: this.loginForm.password
        }))
        if ((res.data !== "" && res.data !== undefined) && res.data.tokenUser.ban !== 1) {
        // 登录成功
          localStorage.setItem("token", res.data.token);
          localStorage.setItem("tokenUser", JSON.stringify(res.data.tokenUser));
          sessionStorage.setItem("email", "")
          this.loadingFlag = false
          this.$router.push('/document')
          ElMessage.success("欢迎使用")
        } else if (res.data !== "" && res.data !== undefined) {
          ElMessage.error("很抱歉，您的账号已被禁用")
        } else {
          ElMessage.error("邮箱或密码错误")
        }
        this.loadingFlag = false
      }
    }
  }
}
</script>

<template>
  <div id="window">
    <div id="window-introduction">
      <el-text tag="b" size="large" style="margin-top: 40px">阿猫网盘</el-text>
      <div id="author">
        <p>author:</p>
        <a href="https://github.com/yinset">jiahuiLuu</a>
        <img src="../assets/logo/github-mark.png" id="github-img"><br/>
        <span>1182552084@qq.com</span>
      </div>
    </div>
    <div id="window-login">
      <p class="blackbgtext">Please Login ( •̀ ω •́ )✧</p>
      <div id="input-login">
        <el-input type="text" id="email" name="email" class="inputLogin" placeholder="请输入邮箱/登录名" :maxlength="36"
                  v-model.trim="loginForm.email" oninput="value=value.replace(/[^\w.@]/g,'')"></el-input>
        <el-input type="password" id="password" name="password" class="inputLogin" placeholder="请输入密码"
                  :maxlength="16" v-model.trim="loginForm.password"
                  oninput="value=value.replace(/[^\w.@]/g,'')"></el-input>
        <!--        <el-checkbox v-model="remindMe" label="记住我" true-value="true" false-value="false"/>-->
      </div>

      <div>
        <el-button plain class="button-login" @click="login" v-loading="loadingFlag">登录</el-button>
        <el-button plain id="register-button" class="button-login" @click="toRegister">注册</el-button>
      </div>
    </div>
  </div>
  <div style="text-align: center">
    <a href="https://beian.miit.gov.cn/" target="_blank" >ICP备案号：冀ICP备2024047868号-2</a>
  </div>
</template>

<style scoped>
a{ text-decoration:none;}
.button-login {
  margin-bottom: 20px;
}

#input-login {
  display: grid;
}

#register-button {
  margin-left: 80px;
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
  width: 100%;
  height: 95%;
}

#window-introduction {
  width: 400px;
  height: 600px;
  display: grid;
}

#window-login {
  background-color: black;
  width: 400px;
  height: 400px;
  position: relative;
  //top: 200px;
  //right: 200px;
  display: grid;
  align-items: center;
  justify-content: center;
}

.inputLogin {
  height: 30px;
  width: 200px;
  position: relative;
  margin-bottom: 20px;
}

.blackbgtext {
  color: white;
}
</style>