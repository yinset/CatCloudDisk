<script setup>
import {
  Delete,
  EditPen,
  PieChart,
  Search,
  UploadFilled,
  User,
  UserFilled,
  VideoPause,
  VideoPlay
} from '@element-plus/icons-vue'
</script>

<script>
import {ElMessage} from "element-plus";
import {httpPost} from "@/utils/axios.js";
import {progressHandle, transform} from "@/utils/tools.js";
import SparkMD5 from "spark-md5"

export default {
  mounted() {
    this.uploading.totalSize = 0
    window.addEventListener('storage', () => {
      window.location.reload()
    })

    this.getSelfAvatar()
  },
  data() {
    return {
      uploading: {
        //上传中的文件总大小
        totalSize: 0,
        //上传列表信息，提供给上传列表显示
        uploadList: [],
        //上传列表每条文件的进度条
        progressPercent: 0,
        //上传图标角标大小
        totalCount: "",
      },
      userInfo: {
        avatarFile: undefined,
        avatarPath: "",
        nickName: "",
        birthday: "",
        dialogFlag: false
      },
      updatePassword: {
        dialogFlag: false,
        oldPassword: "",
        newPassword: ""
      },
      admin: {
        tableLoading: false,
        tableData: [],
        dialogFlag: false,
        selectUsers: []
      },
      //上传列表更新的锁变量
      sign: false,
      userAvatarPath: "",
      userSpaceString: transform(JSON.parse(localStorage.getItem("tokenUser")).useSpace) + "/" + transform(JSON.parse(localStorage.getItem("tokenUser")).totalSpace),
      userSpacePercent: (JSON.parse(localStorage.getItem("tokenUser")).useSpace / JSON.parse(localStorage.getItem("tokenUser")).totalSpace) * 100,
      isAdmin: JSON.parse(localStorage.getItem("tokenUser")).isAdmin,
      //更新refresh，用于当作key，刷新组件
      refresh: undefined,
      nickName: JSON.parse(localStorage.getItem("tokenUser")).nickName,
      //文件分片大小
      chunkSize: 1024 * 1024,
      //并发数量
      maxUploadPool:10,
      maxDownloadPool:2
    }
  },
  methods: {
    logout() {
      localStorage.removeItem("nowFolderId")
      localStorage.removeItem("token")
      localStorage.removeItem("tokenUser")
      window.location.href = '/login'
    },

    async upload(data) {
      //在上传前检查容量是否足够，如果容量不够及时阻止上传并返回“添加上传任务失败，容量不足”
      let filesSize = 0
      data.fileList.forEach(file => {
        filesSize += parseInt(file.size)
      })
      let useSpace = JSON.parse(localStorage.getItem("tokenUser")).useSpace
      let totalSpace = JSON.parse(localStorage.getItem("tokenUser")).totalSpace
      if (totalSpace - useSpace - this.uploading.totalSize < filesSize) {
        ElMessage.error("添加上传任务失败，容量不足(上传中的大小也算哦)")
        //将超容量的文件pop出
        data.fileList.pop()
        return
      }

      //上传状态角标
      if (this.uploading.totalCount === "") {
        this.uploading.totalCount = 1
      } else {
        this.uploading.totalCount++
      }
      //上传列表增加大小
      this.uploading.totalSize += data.file.size

      //当前上传文件
      let file = data.file
      //获取文件MD5
      await this.getMD5(file)

      /**
       * 重新包装文件对象 待发给上传列表显示
       */
      let fileObject = {
        fileUid: file.uid,
        fileMD5: file.fileMD5,
        name: file.name,
        schedule: 0,
        speed: "",
        timeLeft: "",
        status: 0
      }
      this.uploading.uploadList.unshift(fileObject)
      let formData = new FormData()
      //key-value中，若value是对象，则必须序列化，或raw来保存。不然对象信息会丢失(实体对象用序列化，引用对象用raw)
      //后端接受使用multipartFile，故前端不能直接传file.raw，目测Formdata可以
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      formData.append("file", file.raw)
      formData.append("nowFolderId", data.nowFolderId)
      formData.append("fileUid", file.uid)
      formData.append("fileMD5", fileObject.fileMD5)
      formData.append("userId", tokenUser.userId)
      formData.append("email", tokenUser.email)
      //此处首先进行秒传验证，如果文件不能进行秒传则继续进行upload，如果可以秒传则进行秒传。
      let flashTransferFlag = await this.flashTransfer(fileObject, data)
      if (flashTransferFlag === "upload" || flashTransferFlag === "error") {
        await httpPost("/file/uploadFile", formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          },
          onUploadProgress: function (event) {
            let temp
            //数据格式化类上锁
            if (!this.sign) {
              this.sign = true
              temp = progressHandle(event, file)
              this.sign = false
            }
            if (temp !== undefined && temp !== null) {
              this.uploading.progressPercent = parseInt(temp[0])
              this.uploading.uploadList.filter(i => {
                if (parseInt(i.fileUid) === file.raw.uid && temp[0] < 100) {
                  i.schedule = temp[0]
                  i.speed = temp[1]
                  i.timeLeft = temp[2]
                }
              })
            } else if (file.firstGetTime) {
              //waiting
            }
            //结尾.bind(this)后可使用this.xxx,很棒
          }.bind(this),
          timeout: 1000000
        })
      }
      // 上传完毕，用户空间更新
      tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      tokenUser.useSpace += parseInt(file.raw.size)
      localStorage.setItem("tokenUser", JSON.stringify(tokenUser))
      // 容量更新请求
      let res = await httpPost("/file/uploadCheck", this.$qs.stringify({
        userId: tokenUser.userId,
        fileUid: file.uid,
        fileMD5: fileObject.fileMD5
      }))
      if (res.status === 200) {
        //容量显示更新
        this.uploading.totalSize -= data.file.size
        let useSpace = JSON.parse(localStorage.getItem("tokenUser")).useSpace
        let totalSpace = JSON.parse(localStorage.getItem("tokenUser")).totalSpace
        this.userSpaceString = transform(useSpace) + "/" + transform(totalSpace)
        this.userSpacePercent = (useSpace / totalSpace) * 100

        this.uploading.uploadList.filter(i => {
          if (parseInt(i.fileUid) === file.raw.uid) {
            i.schedule = 100
            i.speed = ""
            if (flashTransferFlag === "upload" || flashTransferFlag === "error")
              i.timeLeft = "已完成"
            if (flashTransferFlag === "success")
              i.timeLeft = "秒传"
          }
        })
      } else {
        this.uploading.uploadList.filter(i => {
          if (parseInt(i.fileUid) === file.raw.uid) {
            i.schedule = 0
            i.speed = ""
            i.timeLeft = "文件损坏，请重试。"
            ElMessage.error("发现上传过程后的文件损坏，请重试")
          }
        })
      }
      //上传完成，修改角标
      if (this.uploading.totalCount > 1) {
        this.uploading.totalCount--
      } else {
        this.uploading.totalCount = ""
      }
      this.refresh = new Date().getTime()
    },


    /**
     * 传入文件，得到MD5会以键值对方式注入对象
     */
    getMD5(file) {
      return new Promise((resolve) => {
        //获取md5
        let preFileSize = 2097152
        let fileReader = new FileReader()
        let spark = new SparkMD5.ArrayBuffer()
        //Blob就是file.row的类型
        let md5File = file.raw
        md5File = md5File.slice(0, preFileSize)
        fileReader.readAsArrayBuffer(md5File)
        fileReader.onload = (e) => {
          spark.append(e.target.result)
          //直接把md5赋给对象
          file.fileMD5 = spark.end(false)
          resolve()
        }
      })
    },
    flashTransfer(fileObject, data) {
      return new Promise(async resolve => {
        let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
        let res = await httpPost("/file/flashTransfer", this.$qs.stringify({
          fileMD5: fileObject.fileMD5,
          fileUid: fileObject.fileUid,
          nowFolderId: data.nowFolderId,
          userId: tokenUser.userId,
          email: tokenUser.email
        }))
        if (res.status === 200) {
          //秒传命中
          resolve("success")
        } else if (res.status === 250) {
          //秒传未命中
          resolve("upload")
        } else {
          resolve("error")
        }
      })
    },

    toTop() {
      document.documentElement.scrollTop = 0
    },

    deleteRefresh(useSpace) {
      if (useSpace === "\"\"") {
        useSpace = "0"
      }
      let totalSpace = parseInt(JSON.parse(localStorage.getItem("tokenUser")).totalSpace)
      this.userSpaceString = transform(parseInt(useSpace)) + "/" + transform(totalSpace)
      this.userSpacePercent = (parseInt(useSpace) / totalSpace) * 100
    },

    async uploadAvatar(rawFile) {
      if (rawFile.file.type !== 'image/jpeg' && rawFile.file.type !== 'image/png' && rawFile.file.type !== 'image/webp' && rawFile.file.type !== 'image/gif') {
        ElMessage.error('sorry,头像必须是JPG/png/webp/gif格式')
        return false
      } else if (rawFile.file.size / 1024 / 1024 > 2) {
        ElMessage.error('上传的头像请小于2MB')
        return false
      }
      ElMessage.success("头像更新中......")
      let formData = new FormData()
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      formData.append("avatar", rawFile.file)
      formData.append("userId", tokenUser.userId)
      formData.append("email", tokenUser.email)
      tokenUser.avatarPath = await httpPost("/user/setAvatar", formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      this.getSelfAvatar()
    },

    getAvatar(userId) {
      return new Promise((resolve) => {
        if (userId === undefined || userId === "" || userId === null || userId === 0) {
          userId = JSON.parse(localStorage.getItem("tokenUser")).userId
        }
        httpPost("/user/getAvatar", this.$qs.stringify({userId: userId}), {responseType: 'blob'})
            .then(res => {
              let binaryData = []
              binaryData.push(res.data)
              resolve(URL.createObjectURL(new Blob(binaryData, {type: "application/zip"})))
            })
      })
    },

    async getSelfAvatar() {
      let res = await httpPost("/user/getAvatar", this.$qs.stringify({userId: JSON.parse(localStorage.getItem("tokenUser")).userId}), {responseType: 'blob'})
      let binaryData = []
      binaryData.push(res.data)
      let url = URL.createObjectURL(new Blob(binaryData, {type: "application/zip"}))
      this.userAvatarPath = url
      this.userInfo.avatarPath = url

      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      tokenUser.avatarPath = url
      localStorage.setItem("tokenUser", JSON.stringify(tokenUser))
    },

    userInfoButtonClick() {
      // 获取信息到input中
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      this.userInfo.avatarPath = this.userAvatarPath
      this.userInfo.nickName = tokenUser.nickName
      this.userInfo.birthday = tokenUser.birthday
      this.userInfo.dialogFlag = true
    },

    userInfoCheck(tokenUser) {
      let nickName = this.userInfo.nickName
      if (nickName === "") {
        ElMessage.warning("昵称不可为空(*^_^*)")
        return false
      } else if (tokenUser.nickName === this.userInfo.nickName && tokenUser.avatarPath === this.userInfo.avatarPath && tokenUser.birthday === this.userInfo.birthday) {
        //说明什么都没改变，执行中止
        return false
      }
      return true
    },

    userInfoUploadAvatar(rawFile) {
      if (rawFile.file.type !== 'image/jpeg' && rawFile.file.type !== 'image/png' && rawFile.file.type !== 'image/webp' && rawFile.file.type !== 'image/gif') {
        ElMessage.error('sorry,头像必须是JPG/png/webp/gif格式')
        return false
      } else if (rawFile.file.size / 1024 / 1024 > 2) {
        ElMessage.error('上传的头像请小于2MB')
        return false
      }
      //上传头像的参数的.file 才是blob对象
      //返回的是blob url，并不是文件绝对路径。
      this.userInfo.avatarPath = URL.createObjectURL(rawFile.file)
      this.userInfo.avatarFile = rawFile
    },

    async userInfoConfirmClick() {

      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      if (this.userInfoCheck(tokenUser)) {
        this.nickName = this.userInfo.nickName
        if (this.userInfo.avatarFile !== undefined) {
          await this.uploadAvatar(this.userInfo.avatarFile)
        }
        let res = await httpPost("/user/updateUser", this.$qs.stringify({
          userId: tokenUser.userId,
          password: undefined,
          nickName: this.nickName,
          birthdayString: this.userInfo.birthday,
          avatarPath: undefined
        }))
        if (res.status === 200) {
          ElMessage.success("更新资料成功~")
          //更新tokenUser
          tokenUser.nickName = this.userInfo.nickName
          tokenUser.birthday = this.userInfo.birthday
          tokenUser.avatarPath = undefined
          localStorage.setItem("tokenUser", JSON.stringify(tokenUser))
        } else {
          ElMessage.error("更新出错(┬┬﹏┬┬)，存在bug，请联系管理员")
        }
      }
      this.userInfo.dialogFlag = false
    },

    userInfoCancelClick() {
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      //头像等信息的还原
      this.userInfo.nickName = tokenUser.nickName
      this.userInfo.avatarPath = tokenUser.avatarPath
      this.userInfo.birthday = tokenUser.birthday
      this.userInfo.dialogFlag = false
    },

    updatePasswordCheck() {
      let oldPassword = this.updatePassword.oldPassword
      let newPassword = this.updatePassword.newPassword
      if (oldPassword === "") {
        ElMessage.error("旧密码不可为空(*^_^*)")
        return false
      } else if (newPassword === "") {
        ElMessage.error("新密码不可为空(*^_^*)")
        return false
      }
      return true
    },

    async updatePasswordConfirmClick() {
      if (this.updatePasswordCheck()) {
        //匹配旧密码
        let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
        let res = await httpPost("/user/passwordCheck", this.$qs.stringify({
          oldPassword: this.updatePassword.oldPassword,
          userId: tokenUser.userId
        }))
        if (res.status === 250) {
          ElMessage.error("旧密码不匹配，请重新输入")
        } else if (res.status === 200) {
          let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
          let res = await httpPost("/user/updateUser", this.$qs.stringify({
            userId: tokenUser.userId,
            nickName: undefined,
            password: this.updatePassword.newPassword,
            birthday: undefined,
            avatarPath: undefined
          }))
          if (res.status === 200) {
            ElMessage.success("修改密码成功")
          } else {
            ElMessage.error("修改密码失败，有bug，请联系管理员")
          }
          this.updatePassword.dialogFlag = false
          this.updatePassword.oldPassword = ""
          this.updatePassword.newPassword = ""
        } else {
          ElMessage.error("旧密码检测出错，请联系管理员")
        }
      }
    },
    updatePasswordCancelClick() {
      this.updatePassword.dialogFlag = false
      this.updatePassword.oldPassword = ""
      this.updatePassword.newPassword = ""
    },
    updatePasswordDialog(){
      this.updatePassword.dialogFlag=true
    },
    async adminButtonClick() {
      this.admin.dialogFlag = true
      this.admin.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let userTemp = {}
      let res = await httpPost("/user/selectUsers", this.$qs.stringify({userId: tokenUser.userId}))

      let index = 0
      let length = res.data.length
      while (index < length) {
        userTemp.userId = res.data[index].userId
        if (res.data[index].isAdmin === 1) {
          userTemp.nickName = "👑" + res.data[index].nickName
          userTemp.isAdmin = true
        } else {
          userTemp.nickName = res.data[index].nickName
          userTemp.isAdmin = false
        }
        // userTemp.avatarPath = this.getAvatar(res.data[index].userId)
        userTemp.avatarPath = await this.getAvatar(res.data[index].userId)
        userTemp.useSpace = res.data[index].useSpace
        userTemp.totalSpace = res.data[index].totalSpace
        userTemp.ban = res.data[index].ban !== -1;
        userTemp.percent = (res.data[index].useSpace / res.data[index].totalSpace) * 100
        userTemp.spaceString = transform(res.data[index].useSpace) + "/" + transform(res.data[index].totalSpace)
        userTemp.sizeChange = 0.00
        //原因：对象是引用类型，传递的是引用地址，所以你两个数组引用的是同一个对象，只要其中一个数组改变，就会导致对象改变，进而另一个引用的数组也会改
        //解决办法：JSON.parse(JSON.stringify(userTemp))
        let userTempObject = JSON.parse(JSON.stringify(userTemp))
        this.admin.tableData.push(userTempObject)
        index++
      }
      this.admin.tableLoading = false
      //在打开那一刻存档方便比对
      localStorage.setItem("adminTableData", JSON.stringify(this.admin.tableData))
    },

    async adminConfirmClick() {
      let index = 0
      let save = JSON.parse(localStorage.getItem("adminTableData"))
      while (index < this.admin.tableData.length) {
        let i = this.admin.tableData[index]
        if (save[index].sizeChange !== i.sizeChange || save[index].ban !== i.ban || save[index].isAdmin !== i.isAdmin) {
          //检测容量修改是否合法
          //注意它默认给的是String
          let saveTotalSpaceGB = Number((save[index].totalSpace / 1024 / 1024 / 1024).toFixed(2))
          let saveUseSpaceGB = Number((save[index].useSpace / 1024 / 1024 / 1024).toFixed(2))
          if (i.sizeChange + saveTotalSpaceGB < 0) {
            ElMessage.error("容量修改失败，请检查用户容量修改大小是否正确(╯‵□′)╯︵┻━┻")
            return
          } else if (saveTotalSpaceGB + i.sizeChange < saveUseSpaceGB) {
            ElMessage.error("容量修改失败，总容量低于用户所用容量大小/(ㄒoㄒ)/~~")
            return
          }

          let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
          let res = await httpPost("/user/adminUpdate", this.$qs.stringify({
            userId: i.userId,
            spaceVariation: i.sizeChange,
            ban: i.ban,
            isAdmin: i.isAdmin
          }))
          if (res.status === 200 && i.userId === tokenUser.userId) {
            //本用户容量更改，页面刷新
            tokenUser.totalSpace = parseInt(i.totalSpace + (i.sizeChange * 1024 * 1024 * 1024))
            localStorage.setItem("tokenUser", JSON.stringify(tokenUser))
            window.location.href = "/document"
          } else if (res.status === 200) {
            this.admin.dialogFlag = false
            window.location.href = "/document"
          } else if (res.status === 250) {
            ElMessage.error("权限不足")
          } else {
            this.admin.dialogFlag = false
            ElMessage.error("后台错误，出现bug，请联系管理员")
          }
        }
        index++
      }
    },

    adminCancelClick() {
      this.admin.tableData = []
      this.admin.dialogFlag = false
    },

    handleSelectionChange(rows) {
      if (rows.length > 1) {
        let del_row = rows.shift();
        this.$refs.Table.toggleRowSelection(del_row, false);
      }
      this.admin.selectUsers = JSON.parse(JSON.stringify(rows));
    },

    cellClass(row) {
      if (row.columnIndex === 5) {
        return 'disabledCheck'
      }
    },
  }
}
</script>

<template>
  <el-container>

    <!--dialog-->
    <el-dialog
        v-model="userInfo.dialogFlag"
        title="个人资料"
        width="500"
        :before-close="userInfoCancelClick"
    >
      <div class="userInfoDialogDivFirst">
        <el-text tag="b" style="width: 50px">头像：</el-text>
        <el-upload
            :show-file-list="false"
            :http-request="userInfoUploadAvatar"
        >
          <el-avatar v-if="userInfo.avatarPath" :key="userInfo.avatarPath" :src="userInfo.avatarPath"
                     id="userInfoAvatar"/>
          <el-avatar v-else id="userInfoAvatar" :icon="UserFilled"/>
        </el-upload>
      </div>
      <div class="userInfoDialogDiv">
        <el-text tag="b" style="width: 50px">昵称：</el-text>
        <el-input v-model.trim="userInfo.nickName" style="width: 250px" :maxlength="36"></el-input>
      </div>
      <div class="userInfoDialogDiv">
        <el-text tag="b" style="width: 50px">生日：</el-text>
        <el-date-picker
            type="date"
            placeholder="未设置"
            v-model.trim="userInfo.birthday"
            :editable="false"
        />
      </div>
      <el-text></el-text>
      <template #footer>
        <div class="dialog-footer">
          <!--          在点击确定后，同步两个头像的路径。点击删除后，清空dialog输入的数据-->
          <el-button @click="userInfoCancelClick">取消</el-button>
          <el-button type="primary" @click="userInfoConfirmClick">确定修改</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
        v-model="updatePassword.dialogFlag"
        title="修改密码"
        width="400"
    >
      <div class="userInfoDialogDivFirst">
        <el-text tag="b" style="width: 80px">原密码：</el-text>
        <el-input type="password" v-model.trim="updatePassword.oldPassword" style="width: 250px" :maxlength="16"
                  show-password></el-input>
      </div>
      <div class="userInfoDialogDiv">
        <el-text tag="b" style="width: 80px">新密码：</el-text>
        <el-input type="password" v-model.trim="updatePassword.newPassword" style="width: 250px" :maxlength="16"
                  show-password></el-input>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="updatePasswordCancelClick">取消</el-button>
          <el-button type="primary" @click="updatePasswordConfirmClick">
            确定修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
        id="manageUser"
        v-model="admin.dialogFlag"
        title="用户管理"
        width="880"
        :before-close="adminCancelClick"
    >
      <el-table
          @selection-change="handleSelectionChange"
          id="usersData"
          :data="admin.tableData" style="width: 100%"
          v-loading="admin.tableLoading"
          :header-cell-class-name="cellClass"
      >
        <el-table-column prop="avatarPath" label="用户" width="65px">
          <template v-slot="scope">
            <el-avatar v-if="scope.row.avatarPath" :key="scope.row.avatarPath" :src="scope.row.avatarPath" id="avatar"/>
            <el-avatar v-else id="avatar" :icon="UserFilled"/>
          </template>
        </el-table-column>
        <el-table-column prop="nickName" label="" width="175px">
          <template v-slot="scope">
            <el-text v-text="scope.row.nickName"></el-text>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="容量" width="250px">
          <template v-slot="scope">
            <div id="adminSize">
              <!--el-progress直接添加width无效，而style="width: 200px"则成功生效width，我z啊-->
              <el-progress :percentage="scope.row.percent" :show-text="false"
                           style="width: 90px;margin-right: 10px"></el-progress>
              <el-text v-text="scope.row.spaceString"></el-text>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sizeChange" label="容量修改">
          <template v-slot="scope">
            <el-input-number v-model="scope.row.sizeChange" :precision="2" :step="0.1"/>
            <el-text id="sizeUnit">GB</el-text>
          </template>
        </el-table-column>
        <el-table-column label="管理权限" width="80">
          <template v-slot="scope">
            <!--         加switch开关-->
            <el-switch
                v-model="scope.row.isAdmin"
                class="ml-2"
                style="--el-switch-on-color: #13ce66;"
            />
          </template>
        </el-table-column>
        <!--        css里面去除了全选选项，定义了禁用表头-->
        <el-table-column width="55">
          <template v-slot="scope">
            <el-checkbox v-model="scope.row.ban"></el-checkbox>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adminCancelClick">取消</el-button>
          <el-button type="primary" @click="adminConfirmClick">
            确定修改
          </el-button>
        </div>
      </template>
    </el-dialog>
    <!--dialog-->

    <el-header style="background-color: black;">
      <img src="@/assets/logo/sanhua.png" style="width: 60px;height: 60px" alt="">
      <span style="font-size: 22px;color: white;position: absolute;margin-top: 16px">阿猫网盘</span>
      <el-button id="logout-button" @click="logout">退出登录</el-button>

      <el-badge id="upload-window-button" :value="uploading.totalCount" class="item">

        <el-popover placement="bottom" :width="750" trigger="click">
          <template #reference>
            <el-button :icon="UploadFilled" circle></el-button>
          </template>
          <el-table :data="uploading.uploadList"
                    :row-style="{height: '60px'}"
                    max-height="600">
            <el-table-column width="150" property="name" label="文件名"/>
            <el-table-column width="250" property="progress-bar" label="">
              <template v-slot="scope">
                <el-progress :percentage="parseInt(scope.row.schedule)"></el-progress>
              </template>
            </el-table-column>
            <el-table-column width="100" property="action" label="">
              <template v-slot="scope">
                <el-button v-if="scope.row.status === 1" :icon="VideoPlay" @click="scope.row.status === 0" circle />
                <el-button v-else :icon="VideoPause" circle @click="scope.row.status === 1" />
                <el-button type="danger" :icon="Delete" circle />
              </template>
            </el-table-column>
            <el-table-column width="100" property="speed" label="上传速度"/>
            <el-table-column width="100" property="timeLeft" label="剩余时间"/>
          </el-table>
        </el-popover>

      </el-badge>
    </el-header>

  </el-container>
  <el-container>
    <el-aside style="overflow: hidden;min-height: 100vh;background-color: black; width:250px">
      <el-menu
          :default-active="$route.path"
          router
          class="el-menu-vertical-demo"
          active-text-color=yellow
          background-color="#000000FF"
          text-color=white
      >
        <el-menu-item index="/document">
          <span class="menu-span">个人文件</span>
        </el-menu-item>
        <el-menu-item index="/recycle">
          <span class="menu-span">回收站</span>
        </el-menu-item>


        <div id="capacity">
          <div id="user-info">
            <el-upload
                :show-file-list="false"
                :http-request="uploadAvatar"
            >
              <el-avatar v-if="userAvatarPath" :key="userAvatarPath" :src="userAvatarPath" id="avatar"/>
              <el-avatar v-else id="avatar" :icon="UserFilled"/>
            </el-upload>

            <el-popover trigger="hover" placement="top-start">
              <el-button id="user-popover-button" type="primary" :icon="User" @click="userInfoButtonClick">个人资料
              </el-button>
              <el-button class="user-popover-item" type="primary" :icon="EditPen"
                         @click="updatePasswordDialog">
                修改密码
              </el-button>
              <el-button class="user-popover-item" type="primary" :key="isAdmin" v-if="isAdmin===1" :icon="PieChart"
                         @click="adminButtonClick">用户管理
              </el-button>

              <template #reference>
                <el-text id="nick-name" size="large" type="success">{{ nickName }}</el-text>
              </template>
            </el-popover>

          </div>

          <el-progress :percentage="userSpacePercent" :show-text="false"></el-progress>
          <el-text v-text="userSpaceString"></el-text>
          <br/>
          <el-button id="button-to-top" @click="toTop" size="small">返回顶部</el-button>
        </div>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view v-slot="{ Component }" style="">
        <component @upload="upload" @deleteRefresh="deleteRefresh" :is="Component" :key="refresh"></component>
      </router-view>
    </el-main>
  </el-container>
</template>

<style>
#manageUser {
  user-select: none;
}

#sizeUnit {
  margin-left: 10px;
}

#adminSize {
  display: flex;
}

/* 去掉全选按钮 */
.el-table .disabledCheck .cell .el-checkbox__inner {
  display: none !important;
}

.el-table .disabledCheck .cell::before {
  content: '禁用';
  text-align: center;
  line-height: 37px;
}

#userInfoAvatar {
  width: 100px;
  height: 100px;
}

.userInfoDialogDivFirst, .userInfoDialogDiv {
  margin-top: 10px;
  display: flex;
}

.userInfoDialogDiv {
  margin-top: 30px;
}

.el-menu-vertical-demo {
  border-right: none !important;
}

.menu-span {
  margin-left: 25px;
}

#logout-button {
  float: right;
  margin-top: 15px;
}

#upload-window-button {
  float: right;
  margin-top: 15px;
  margin-right: 20px;
}

#capacity {
  width: 190px;
  height: 135px;
  position: fixed;
  bottom: 30px;
  left: 25px;
}

#button-to-top {
  margin-top: 8px;
}

#user-info {
  margin-bottom: 20px;
  display: flex;
}

#nick-name {
  margin-left: 10px;
  position: relative;
  bottom: 3px;
}

.user-popover-item {
  margin-top: 8px;
  position: relative;
  right: 5px;
}

#user-popover-button {
  margin-left: 7px;
}
</style>