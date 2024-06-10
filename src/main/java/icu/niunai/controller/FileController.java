package icu.niunai.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import icu.niunai.entity.po.File;
import icu.niunai.service.FileService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private FileService fileService;

    /**
     * 创建文件夹
     */
    @PostMapping("/createFolder")
    public void createFolder(HttpServletRequest request, int userId, String newFolderName, int nowFolderId, String email) {
        fileService.createFolder(request, userId, newFolderName, nowFolderId, email);
    }

    @PostMapping("/deleteFile")
    public void deleteFile(HttpServletRequest request, int fileId,int userId, String email) throws IOException {
        fileService.deleteFile(request, fileId, userId, email, null, null);
    }

    @PostMapping("/recoverFile")
    public void recoverFile(int fileId,int userId) throws IOException {
        fileService.recoverFile(fileId,userId,-1);
    }

    //返回值为用户当前容量（单位字节）
    @PostMapping("/hardDelete")
    public Long hardDelete(int fileId,int userId) {
        return fileService.hardDelete(fileId,userId,-1);
    }

    @PostMapping("/uploadFile")
    public void uploadFile(HttpServletRequest request, @RequestParam("file") MultipartFile file, @RequestParam("nowFolderId") int nowFolderId, @RequestParam("fileUid") String fileUid, @RequestParam("fileMD5") String fileMD5, @RequestParam("userId") int userId, @RequestParam("email") String email) throws IOException {
         /*
            因为百度查的 springboot  自带一些视图解析器的原因 所以要不能用MultipartFile来接收
            第一种方法就是用 HttpServletRequest 接收
            第二种方法去百度搜   听说要干掉冲突那个解析器
         */
//        MultipartFile file = ((MultipartHttpServletRequest) request).getFile("file");
//        或者在形参上 @RequestParam("file") MultipartFile file,
        fileService.uploadFile(request, file, nowFolderId, Long.valueOf(fileUid), fileMD5, userId, email);
    }

    @PostMapping("/flashTransfer")
    public void flashTransfer(HttpServletRequest request, HttpServletResponse response, String fileMD5, int nowFolderId, String email, int userId, Long fileUid) throws IOException {
        fileService.flashTransfer(request, response, fileMD5, nowFolderId, email, userId, fileUid);
    }

    @PostMapping("/selectFile")
    public File selectFile(int fileId,int userId,Long fileUid,String filePath,String fileName,String fileMD5,int delFlag,int delIndirectFlag,int hardDelFlag, int splitUploadFlag, int splitUploadCount){
        return fileService.selectFile(fileId,userId,fileUid,filePath,fileName,fileMD5,delFlag,delIndirectFlag,hardDelFlag,splitUploadFlag,splitUploadCount);
    }

    @PostMapping("/selectFiles")
    public List<File> selectFiles(int userId, String fileNameLike, int folderType, int nowFolderId, int delFlag, int delIndirectFlag, int hardDelFlag) {
        if(fileNameLike!=null){
            fileNameLike = "%" + fileNameLike + "%";
        }
        return fileService.selectFiles(userId, fileNameLike, folderType, nowFolderId, delFlag, delIndirectFlag, hardDelFlag);
    }

    //返回上一目录
    @PostMapping("/back")
    public List<File> back(HttpServletResponse response, int userId, int delFlag, int nowFolderId, int folderType) {
        return fileService.back(response, userId, delFlag, nowFolderId, folderType);
    }

    @GetMapping("/download/{fileId}/{token}/*")
    public void download(HttpServletResponse response, @PathVariable int fileId, @PathVariable String token) throws GeneralSecurityException, IOException {
        fileService.download(response, fileId, token);
    }

    //用fileUid + userId 组合检查。令前端的fileUid间隔一毫秒即可做到无重复。使用fileMD5检测文件完整性
    @PostMapping("/uploadCheck")
    public void uploadCheck(HttpServletResponse response, int userId, Long fileUid, String fileMD5) {
        fileService.uploadCheck(response, userId, fileUid, fileMD5);
    }

    @PostMapping("/moveFile")
    public void moveFile(HttpServletRequest request, HttpServletResponse response, int fileId, int desFolderId, String email, int userId) throws IOException {
        fileService.moveFile(request, response, fileId, desFolderId, email, userId);
    }

    @PostMapping("/getBreadcrumb")
    public String getBreadcrumb(HttpServletRequest request, int userId, int nowFolderId, String email) {
        return fileService.getBreadcrumb(request, userId, nowFolderId, email);
    }

    @PostMapping("/beforeSplitUpload")
    public List<Integer> beforeSplitUpload(HttpServletRequest request,int userId,String email,int nowFolderId, String fileName,Long fileSize,Long fileUid,String fileMD5 ,int splitCount){
        return fileService.beforeSplitUpload(request,userId,email,nowFolderId,fileName,fileSize,fileUid,fileMD5,splitCount).stream().map(Integer::parseInt).toList();
    }
    @PostMapping("/splitUpload")
    public void splitUpload(HttpServletRequest request,@RequestParam("index") int index,@RequestParam("chunk")MultipartFile chunk,@RequestParam("fileMD5") String fileMD5,@RequestParam("email")String email) throws IOException {
        fileService.splitUpload(request,index,chunk,fileMD5,email);
    }
}
