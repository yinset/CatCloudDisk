<script setup>
import {Back, Delete, FolderChecked, Search} from "@element-plus/icons-vue";
</script>

<script>
import {httpPost} from "@/utils/axios.js";
import {ElMessage} from "element-plus";

export default {
  emits: ['upload', 'deleteRefresh'],
  mounted() {
    this.getList()
  },

  data() {
    return {
      tableData: [],
      nowFolderId: 0,
      selectFiles: [],
      multiFlag: false,
      errorFlag: false,
      tableLoading: false,
      searchValue: "",
      breadcrumb:"",
      searchFlag: false,
      count:0
    }
  },

  methods: {
    async getList() {
      this.tableLoading = true
      this.searchFlag = false
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: undefined,
        folderType: 0,
        nowFolderId: 0,
        delFlag: 1,
        delIndirectFlag: 0,
        hardDelFlag: -1
      }))
      this.tableData = res.data
      this.tableLoading = false
    },

    async recoverFile(fileId,length) {
      this.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      await httpPost("/file/recoverFile", this.$qs.stringify({fileId: fileId,userId:tokenUser.userId})).catch(()=>{
        this.errorFlag = true
      })
      if(this.errorFlag === false && this.multiFlag === false){
        ElMessage.success("恢复文件成功")
      }else if(this.errorFlag === true){
        ElMessage.error("恢复文件失败，请联系管理员")
        this.errorFlag = false
      }else if(this.errorFlag === false && this.multiFlag === true && ++this.count >= length){
        ElMessage.success("批量恢复完成")
        this.count = 0
        this.selectFiles = []
      }
      await this.getList()
      this.tableLoading = false
    },

    async deleteFile(fileId,length) {
      this.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/hardDelete", this.$qs.stringify({fileId: fileId,userId:tokenUser.userId}))
      if(this.errorFlag === false && this.multiFlag === false){
        ElMessage.success("删除文件成功")
      }else if(this.errorFlag === true){
        ElMessage.error("删除文件失败，请联系管理员")
        this.errorFlag = false
      }else if(this.errorFlag === false && this.multiFlag === true && ++this.count >= length){
        ElMessage.success("批量删除完成")
        this.count = 0
        this.selectFiles = []
      }
      //修改用户容量
      tokenUser.useSpace = res.data
      localStorage.setItem("tokenUser", JSON.stringify(tokenUser))
      //下面的排列顺序有问题。loading在最后，loading不正常取消。放最前，数据刷新不及时
      this.tableLoading = false
      await this.getList()
      this.$emit('deleteRefresh', res.data)
    },

    caseTypeNamesFormat(row) {
      if (row.fileSize === null) {
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

    singleDelete(fileId){
      this.multiFlag = false
      this.deleteFile(fileId)
    },

    multipleDelete() {
      this.multiFlag = true
      let length = this.selectFiles.length
      let count = 0
      if(length === 0){
        ElMessage.warning("请先选择要删除的文件(*^_^*)")
        return
      }
      this.selectFiles.forEach(file => {
        this.deleteFile(file.fileId,length)
        if (++count === length) {
          ElMessage.info("批量删除中...")
        }
      })
    },

    singleRecover(fileId){
      this.multiFlag = false
      this.recoverFile(fileId)
    },

    multipleRecover() {
      this.multiFlag = true
      let length = this.selectFiles.length
      let count = 0
      if(length === 0){
        ElMessage.warning("请先选择要恢复的文件(*^_^*)")
        return
      }
      this.selectFiles.forEach(file => {
        this.recoverFile(file.fileId,length)
        if (++count === length) {
          ElMessage.info("批量还原中...")
        }
      })
    },

    async search() {
      this.searchFlag = true
      this.tableLoading = true
      let tokenUser = JSON.parse(localStorage.getItem("tokenUser"))
      let res = await httpPost("/file/selectFiles", this.$qs.stringify({
        userId: tokenUser.userId,
        fileNameLike: this.searchValue,
        folderType: 0,
        nowFolderId: 0,
        delFlag: 1,
        delIndirectFlag: -1,
        hardDelFlag: -1
      }))
      this.breadcrumb = "\"" + this.searchValue + "\"" + "的搜索结果"
      this.tableData = res.data
      this.tableLoading = false
    },
    goBack() {
      if (this.searchFlag === true) {
        this.getList()
        this.searchFlag = false
      }
      this.breadcrumb = ""
    },
    handleSelectionChange(rows) {
      this.selectFiles = rows
    }
  }
}
</script>

<template>
<div>
  <div id="document-bar">
    <el-button class="popover-bar-item" type="primary" :icon="Back" @click="goBack" plain></el-button>
    <el-button class="popover-bar-item" type="primary" :icon="FolderChecked" @click="multipleRecover" plain>批量还原
    </el-button>
    <el-button color = "black" class="popover-bar-item" type="primary" :icon="Delete" @click="multipleDelete" plain>批量删除</el-button>

    <el-input class="document-bar-item" :maxlength="36" v-model="searchValue">搜索框</el-input>
    <el-button class="document-bar-item" type="primary" :icon="Search" @click="search"></el-button>
  </div>
  <div style="margin-top: 10px;display: flex">
    <el-text v-if="breadcrumb === ''">回收站内的文件90天后自动删除~</el-text>
    <el-text v-else tag="b">{{ breadcrumb }}</el-text>

  </div>
  <el-table
      @selection-change="handleSelectionChange"
      id="tableData"
      :data="tableData" style="width: 100%"
      v-loading="tableLoading"
      :border="true"
  >

    <el-table-column
        type="selection"
        width="55">
    </el-table-column>
    <el-table-column prop="fileName" label="文件名"/>
    <el-table-column prop="fileSize" label="大小" :formatter="caseTypeNamesFormat"/>
    <el-table-column prop="relativePath" label="原位置"></el-table-column>
    <el-table-column prop="deleteTime" label="删除时间"></el-table-column>
    <el-table-column prop="action" label="操作">
      <template v-slot="scope">
        <el-button type="primary" :icon="FolderChecked" @click.stop="singleRecover(scope.row.fileId)" circle/>
        <el-button color="black" :icon="Delete" @click.stop="singleDelete(scope.row.fileId)" circle/>
      </template>
    </el-table-column>

  </el-table>
</div>
</template>

<style scoped>
#document-bar {
  display: flex;
  justify-content: space-between;
  margin-left: 10px;
}

.document-bar-item {
  margin-left: 10px;
}

#tableData {
  margin-top: 15px;
}
</style>