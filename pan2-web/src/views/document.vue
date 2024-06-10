<script setup>
import {Back, Delete, Download, Folder, FolderAdd, House, Rank, Search, Star, Upload} from '@element-plus/icons-vue'
</script>

<script>
import {httpPost} from "@/utils/axios.js";
import {ElMessage} from "element-plus";
import {GlobalVariable} from "@/components/global.js";


export default {
  //向父组件传递参数
  emits: ['upload', 'deleteRefresh'],

  mounted() {
    //判断鼠标是点击还是拖动
    //监测鼠标是移动还是拖拽e
    let x1, x2, y1, y2
    window.addEventListener("mousedown", (e) => {
      x1 = e.clientX
      y1 = e.clientY
    })
    window.addEventListener("mouseup", (e) => {
      x2 = e.clientX
      y2 = e.clientY
      let _val = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
      //判断
      this.flag.click = _val >= 0 && _val <= 2;
    })

    if(isNaN(parseInt(localStorage.getItem("nowFolderId")))){
      localStorage.setItem("nowFolderId","-1")
    }

    this.nowFolderId = parseInt(localStorage.getItem("nowFolderId"))
    this.refreshFolder()
  },

  data() {
    return {
      flag: {
        //true代表判断是鼠标点击
        click: false,
        //主页是否为搜索表
        search: false,
        //个人文件列表加载图标
        tableLoading: false,
        //新建文件夹dialog显示
        createFolderDialog: false,
        //批量删除Flag
        multiDelete:false,
        multiDeleteCount:0
      },
      input: {
        newFolderName: ""
      },
      move: {
        nowFolderId: -1,
        tableData: [],
        //控制移动文件Dialog的确定按钮样式是批量还是单个
        multiFlag: false,
        //控制移动文件Dialog的显示
        dialogFlag: false,
        //移动文件table的加载图标
        tableLoading: false,
        //准备移动的文件Id
        fileId: 0,
        breadcrumb: "",
        fileCount:0
      },
      nowFolderId: -1,
      tableData: [],
      //多选框被选中的文件
      selectFiles: [],
      breadcrumb: "",
      searchInput: ""
    }
  },

  methods: {
    async handleChange(file, fileList) {
      let nowFolderId = this.nowFolderId
      this.$emit('upload', {file, fileList, nowFolderId})
    },

    uploadRunning() {
    },

    async getList() {
      this.flag.tableLoading = true
      this.flag.search = false
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: undefined,
        folderType: 0,
        nowFolderId: -1,
        delFlag: -1,
        delIndirectFlag: -1,
        hardDelFlag:-1
      }))
      this.tableData = res.data
      this.nowFolderId = -1
      localStorage.setItem("nowFolderId",this.nowFolderId)
      await this.getBreadcrumb()
      this.flag.tableLoading = false
    },

    async rowClick(row, column) {
      let nowFolderId
      if (column.label === '文件名' && row.folderType === 1 && this.flag.click) {
        nowFolderId = row.fileId
      } else if (column.label === '所在位置' && this.flag.click) {
        nowFolderId = row.fileSuperId
      } else {
        return
      }
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))

       let res = await httpPost("/file/selectFiles", this.$qs.stringify({
         userId: tokenUser.userId,
         fileNameLike: undefined,
         folderType: 0,
         nowFolderId: nowFolderId,
         delFlag: -1,
         delIndirectFlag: -1,
         hardDelFlag:-1
       }))
      this.tableData = res.data

      this.nowFolderId = row.fileId
      localStorage.setItem("nowFolderId",this.nowFolderId)
      await this.getBreadcrumb()
      this.flag.tableLoading = false
      this.flag.search = false
    },

    async createFolderConfirm() {
      this.flag.tableLoading = true
      this.flag.createFolderDialog = false
      if (this.input.newFolderName === "") {
        ElMessage.error("文件夹名不得为空")
        this.flag.tableLoading = false
        return
      }
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))

      let res = await httpPost("/file/createFolder", this.$qs.stringify({
        userId: tokenUser.userId,
        newFolderName: this.input.newFolderName.trim(),
        nowFolderId: this.nowFolderId,
        email: tokenUser.email
      }))
      if (res.status === 200) {
        ElMessage.success("文件夹创建成功")
      } else {
        ElMessage.success("文件夹创建失败")
      }
      this.input.newFolderName = ""
      await this.refreshFolder()
    },

    cancelCreateFolder() {
      this.flag.createFolderDialog = false
      this.newFolderName = ""
    },

    //下载有待优化
    downloadClick(file) {
      if (file.fileSize !== '文件夹' && file.fileSize !== undefined) {
        let link = document.createElement('a')
        link.style.display = '_blank'
        link.target = 'none'
        link.href = GlobalVariable.baseURL + '/file/download/' + file.fileId + "/" + localStorage.getItem("token") + "/" + file.fileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      } else {
        ElMessage.error("不可选中文件夹批量下载")
      }
    },

    async deleteClick(fileId,length) {
      this.flag.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/deleteFile", this.$qs.stringify({fileId: fileId,userId:tokenUser.userId, email: tokenUser.email}))
      if(res.status === 200 && this.flag.multiDelete === false){
        ElMessage.success("删除成功")
      }else if(++this.flag.multiDeleteCount>=length &&  this.flag.multiDelete === true){
        ElMessage.success("批量删除成功")
        this.flag.multiDeleteCount = 0
        this.selectFiles = []
      }
      await this.refreshFolder()
    },

    async goBack() {
      this.flag.tableLoading = true
      if (this.flag.search === true) {
        await this.refreshFolder()
        this.flag.search = false
        return
      }
      if (this.nowFolderId !== -1) {
        let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
        let res = await httpPost("/file/back", this.$qs.stringify({
          userId: tokenUser.userId,
          delFlag: -1,
          nowFolderId: this.nowFolderId,
          folderType: 0
        }))
        this.tableData = res.data
        //利用响应头获取当前父文件夹Id
        this.nowFolderId = parseInt(res.headers.get("nowFolderId"))
        localStorage.setItem("nowFolderId",this.nowFolderId)
        await this.getBreadcrumb()
      }
      this.flag.tableLoading = false
    },

    async refreshFolder() {
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({userId:tokenUser.userId,fileNameLike:undefined,folderType:0,nowFolderId:this.nowFolderId,delFlag:-1,delIndirectFlag:-1,hardDelFlag:-1}))
      if (res.status === 200) {
        this.tableData = res.data
      }
      await this.getBreadcrumb()
      this.flag.tableLoading = false
    },

    multiDownLoad() {
      let count = 0
      let length = this.selectFiles.length
      if(length === 0){
        ElMessage.warning("请先选择要下载的文件(*^_^*)")
        this.flag.tableLoading = false
        return
      }
      this.selectFiles.forEach(file => {
        this.downloadClick(file)
        if(++count>=length){
          ElMessage.success("批量下载中...")
        }
      })
      this.selectFiles = []
    },

    singleDelete(fileId){
      this.flag.multiDelete = false
      this.deleteClick(fileId)
    },

    multiDelete() {
      this.flag.multiDelete = true
      this.flag.tableLoading = true
      let count = 0
      let length = this.selectFiles.length
      if(length === 0){
        ElMessage.warning("请先选择要删除的文件(*^_^*)")
        this.flag.tableLoading = false
        return
      }
      this.selectFiles.forEach(file => {
        this.deleteClick(file.fileId,length)
        if(++count>=length){
          ElMessage.info("批量删除中...")
        }
      })
      this.selectFiles = []
    },

    async moveDialog(fileId) {
      // 移动文件界面显示以及数据准备
      this.move.tableLoading = true
      this.move.dialogFlag = true
      this.move.nowFolderId = this.nowFolderId
      this.move.fileId = fileId
      await this.getMoveBreadcrumb()
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: undefined,
        folderType: 1,
        nowFolderId: this.move.nowFolderId,
        delFlag: -1,
        delIndirectFlag: -1,
        hardDelFlag:-1
      }))
      this.move.tableData = res.data
      this.move.tableLoading = false

    },

    singleMoveDialog(fileId){
      this.move.multiFlag = false
      this.moveDialog(fileId)
    },

    multiMoveDialog() {
      let length = this.selectFiles.length
      if(length === 0) {
        ElMessage.warning("请先选择要移动的文件(*^_^*)")
        this.flag.tableLoading = false
        return
      }
        this.flag.tableLoading = true
        this.move.multiFlag = true
        this.moveDialog()
    },

    async moveHomeButtonClick() {
      this.move.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: undefined,
        folderType: 1,
        nowFolderId: -1,
        delFlag: -1,
        delIndirectFlag: -1,
        hardDelFlag:-1
      }))
      this.move.tableData = res.data
      this.move.nowFolderId = -1
      await this.getMoveBreadcrumb()
      this.move.tableLoading = false
    },

    async moveRowClick(row) {
      this.move.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: undefined,
        folderType: 1,
        nowFolderId: row.fileId,
        delFlag: -1,
        delIndirectFlag: -1,
        hardDelFlag:-1
      }))
      this.move.tableData = res.data
      this.move.nowFolderId = row.fileId
      await this.getMoveBreadcrumb()
      this.move.tableLoading = false
    },

    async moveBack() {
      this.move.tableLoading = true
      if (this.move.nowFolderId !== -1) {
        let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
        let res = await httpPost("/file/back", this.$qs.stringify({
          userId: tokenUser.userId,
          delFlag: -1,
          nowFolderId: this.move.nowFolderId,
          folderType: 1
        }))
        this.move.tableData = res.data
        this.move.nowFolderId = parseInt(res.headers.get("nowFolderId"))
        await this.getMoveBreadcrumb()
      }
      this.move.tableLoading = false
    },

    moveBeforeClose() {
      //关闭操作
      this.move.dialogFlag = false
    },

    async moveConfirm() {
      this.flag.tableLoading = true
      this.move.dialogFlag = false
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/moveFile", this.$qs.stringify({
        fileId: this.move.fileId,
        desFolderId: this.move.nowFolderId,
        email: tokenUser.email,
        userId: tokenUser.userId
      }))
      if (res.status === 200 && this.move.multiFlag === false) {
        ElMessage.success("文件移动成功")
      }
      else if (res.status === 250) {
        ElMessage.error('不允许移动至自己(╯‵□′)╯︵┻━┻')
        return
      }
      if(++this.move.fileCount >= this.selectFiles.length && this.move.multiFlag === true){
        ElMessage.success("批量移动完成")
        this.selectFiles = []
        this.move.fileCount = 0
      }
      //refreshFolder中有loading关闭
      await this.refreshFolder()
    },

    multiMoveConfirm() {
      this.move.dialogFlag = false
      let count = 0
      let length = this.selectFiles.length
      this.selectFiles.forEach(file => {
        this.move.fileId = file.fileId
        this.moveConfirm()
        if(++count>=length){
          ElMessage.info("批量移动中...")
        }
      })
    },

    async getBreadcrumb() {
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/getBreadcrumb", this.$qs.stringify({
        userId: tokenUser.userId,
        nowFolderId: this.nowFolderId,
        email: tokenUser.email
      }))
      this.breadcrumb = res.data
    },

    async getMoveBreadcrumb() {
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res =  await httpPost("/file/getBreadcrumb", this.$qs.stringify({
        userId: tokenUser.userId,
        nowFolderId: this.move.nowFolderId,
        email: tokenUser.email
      }))
      this.move.breadcrumb = res.data
    },

    async search() {
      this.flag.search = true
      this.flag.tableLoading = true
      this.breadcrumb = "\"" + this.searchInput + "\"" + "的搜索结果"
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: this.searchInput,
        folderType: 0,
        nowFolderId: -1,
        delFlag: -1,
        delIndirectFlag: -1,
        hardDelFlag:-1
      }))
      this.tableData = res.data
      this.flag.tableLoading = false
    },

    tableCellStyle({row, column, rowIndex, columnIndex}) {
      //控制文件名和所在为止的颜色
      if (columnIndex === 1) {
        return {'color': '#4573D5', 'text-decoration': 'underline', 'cursor': 'pointer'}
      }
      if (column.label === '所在位置') {
        return {'color': '#4573D5', 'text-decoration': 'underline', 'cursor': 'pointer'}
      }
    },

    handleSelectionChange(rows) {
      this.selectFiles = rows
    },

    caseTypeNamesFormat(row) {
      if (row.fileSize === '' || row.fileSize === "文件夹" || row.fileSize === null || row.fileSize === undefined) {
        return "文件夹"
      }
      return this.filterType(row.fileSize)
    },

    filterType(val) {
      if (val === 0) return "0B"
      let k = 1024
      let sizes = ["B", "KB", "MB", "GB", "TB"]
      let i = Math.floor(Math.log(val) / Math.log(k))
      return (val / Math.pow(k, i)).toPrecision(3) + "" + sizes[i]
    },
  }
}
</script>

<template>
  <div>
    <div id="document-bar">
      <el-upload
          :on-change="handleChange"
          :http-request="uploadRunning"
          :show-file-list="false"
          multiple
      >
        <el-button id="upload-button" class="document-bar-item" type="primary" :icon="Upload" plain>上传</el-button>
      </el-upload>

      <el-button class="document-bar-item" type="primary" :icon="FolderAdd" @click="flag.createFolderDialog = true" plain>
        新建文件夹
      </el-button>

      <el-popover
          id="popover-action"
          placement="bottom"
          trigger="hover"
      >
        <el-button class="popover-bar-item" type="primary" :icon="Download" @click="multiDownLoad" plain>批量下载
        </el-button>
        <el-button class="popover-bar-item" type="primary" :icon="Rank" @click="multiMoveDialog" plain>批量移动
        </el-button>
        <el-button class="popover-bar-item" type="primary" :icon="Delete" @click="multiDelete" plain>批量删除
        </el-button>
        <template #reference>
          <div>
            <el-button class="document-bar-item" type="primary" :icon="Folder" plain>批量操作</el-button>
          </div>
        </template>
      </el-popover>

      <el-input class="document-bar-item" :maxlength="36" v-model.trim="searchInput">搜索框</el-input>
      <el-button class="document-bar-item" type="primary" :icon="Search" @click="search"></el-button>
    </div>
    <div style="display: flex">
      <el-button id="back-button" class="document-bar-item" type="primary" @click="goBack" :icon="Back" plain></el-button>
      <el-button id="home-button" class="document-bar-item" type="primary" @click="getList" :icon="House"
                 plain></el-button>
      <div id="breadcrumb">
        <el-text size="large" tag="b">{{ breadcrumb }}</el-text>
      </div>
    </div>

    <el-dialog
        title="请输入新文件夹名字"
        v-model="flag.createFolderDialog"
        width="400px"
        style="margin-top: 20%">
      <el-input v-model.trim="input.newFolderName" :maxlength="36" name="newFolderName"/>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelCreateFolder">取消</el-button>
          <el-button type="primary" @click="createFolderConfirm">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-table
        @selection-change="handleSelectionChange"
        @row-click="rowClick"
        id="tableData"
        :data="tableData" style="width: 100%"
        v-loading="flag.tableLoading"
        :cell-style="tableCellStyle"
    >

      <el-table-column
          type="selection"
          width="55">
      </el-table-column>
      <el-table-column prop="fileName" label="文件名"/>
      <el-table-column prop="fileSize" label="大小" :formatter="caseTypeNamesFormat"/>
      <el-table-column prop="createTime" label="上传时间"/>
      <el-table-column prop="relativePath" label="所在位置" v-if="flag.search"/>
      <el-table-column prop="action" label="操作" width="150" slot-scope="scope" v-else>
        <template v-slot="scope">
          <div style="display: flex">
            <el-button class="actionButton" :icon="Rank" @click="singleMoveDialog(scope.row.fileId)" circle></el-button>
            <el-button class="actionButton" v-if="scope.row.fileSize !==undefined && scope.row.fileSize !== null" type="primary" :icon="Download"
                       @click.stop="downloadClick(scope.row)" circle/>
            <!--        这个v-if我一开始放在了el-button里面，结果老是报子组件未找到问题。原来是v-if放的太深了应该给到popover-->
            <el-popover
                v-if="scope.row.fileSize === undefined || scope.row.fileSize === null"
                placement="top-start"
                title="嗨嗨"
                :width="200"
                trigger="click"
                content="这是一个让强迫症舒服的按钮，功能敬请期待(*^_^*)"
            >
              <template #reference>
                <div style="display: flex">
                  <el-button class="actionButton" type="warning" :icon="Star" circle/>
                </div>
              </template>
            </el-popover>
            <el-button class="actionButton" type="danger" :icon="Delete" @click.stop="singleDelete(scope.row.fileId)" circle/>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
        v-model="move.dialogFlag"
        title="请进入要移动到的文件位置"
        width="50%"
        :before-close="moveBeforeClose"
    >
      <div id="editPath-bar">
        <el-button class="editPath-bar-item" type="primary" @click="moveBack" :icon="Back" plain></el-button>
        <el-button id="home-button-edit-path" class="editPath-bar-item" type="primary" @click="moveHomeButtonClick"
                   :icon="House" plain></el-button>
        <div id="editPathBreadCrumb">
          <el-text size="large" tag="b">{{ move.breadcrumb }}</el-text>
        </div>
      </div>
      <el-table
          @row-click="moveRowClick"
          id="tableData"
          :data="move.tableData" style="width: 100%;height: 500px"
          v-loading="move.tableLoading"

      >
        <el-table-column prop="blank" label=" " width="10px"/>
        <el-table-column prop="fileName" label="文件名"/>
        <el-table-column prop="fileSize" label="" :formatter="caseTypeNamesFormat"/>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="move.dialogFlag = false">取消</el-button>
          <el-button type="primary" v-if="move.multiFlag" @click="multiMoveConfirm">批量保存</el-button>
          <el-button type="primary" v-else @click="moveConfirm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>

#document-bar {
  display: flex;
  justify-content: space-between;
}

.document-bar-item {
  margin-left: 10px;
}

.popover-bar-item {
  margin-left: 11px;
  margin-top: 8px;
}

.editPath-bar-item {
  width: 40px;
}

#back-button,#home-button {
  margin-top: 10px;
  width: 40px;
}

#home-button-edit-path {
  width: 40px;
}

#upload-button {
  width: 92px;
}

#editPath-bar {
  margin-left: 10px;
  display: flex;
}

#editPathBreadCrumb {
  margin-top: 5px;
  margin-left: 18px;
}

#breadcrumb {
  margin-top: 14px;
  margin-left: 12px;
}

#tableData {
  margin-top: 10px;
}
.actionButton{
  margin-right: 10px;
}
.actionButton+.actionButton {
  margin-left: 0;
}
</style>