package icu.niunai.service.impl;

import com.alibaba.fastjson.JSON;
import icu.niunai.constant.Constants;
import icu.niunai.entity.dto.SessionUser;
import icu.niunai.entity.po.File;
import icu.niunai.mapper.FileMapper;
import icu.niunai.mapper.UserMapper;
import icu.niunai.utils.AESUtil;
import icu.niunai.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import icu.niunai.service.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileServiceImpl implements FileService {
    @Resource
    private FileMapper fileMapper;
    @Resource
    private UserMapper userMapper;

    public File selectFile(int fileId,int userId,Long fileUid,String filePath,String fileName,String fileMD5,int delFlag,int delIndirectFlag,int hardDelFlag, int splitUploadFlag, int splitUploadCount){
        //查询是否分片上传过
        return fileMapper.selectFile(fileId,userId,fileUid,filePath,fileName,fileMD5,delFlag,delIndirectFlag,hardDelFlag,splitUploadFlag,splitUploadCount);
    }

    public List<File> selectFiles(int userId, String fileNameLike, int folderType, int fileSuperId, int delFlag, int delIndirectFlag, int hardDelFlag) {
        return fileMapper.selectFiles(userId, fileNameLike, folderType, fileSuperId, delFlag, delIndirectFlag, hardDelFlag);
    }

    public List<File> back(HttpServletResponse response, int userId, int delFlag, int nowFolderId, int folderType) {
        /*如果当前目录是根目录，返回null什么都不做。
         * 如果当前不是根目录，
         *   查询nowFolder对象，获取它的父文件夹Id
         *       如果父文件夹Id为-1，则返回根目录
         *       如果父文件夹Id非-1，打开上一级文件夹
         *   然后通过响应头返回nowFolderId
         * */
        if (nowFolderId != -1) {
            //查询当前目录对象
            File nowFolder = fileMapper.selectFile(nowFolderId, userId, 0L, null, null, null, delFlag, delFlag,-1,0,0);
            response.setHeader("nowFolderId", String.valueOf(nowFolder.getFileSuperId()));
            return fileMapper.selectFiles(userId, null, folderType, nowFolder.getFileSuperId(), delFlag, -1, -1);
        }
        return null;
    }

    /**
     * 创建文件夹
     */
    public void createFolder(HttpServletRequest request, int userId, String newFolderName, int nowFolderId, String email) {
        String newFilePath;
        String nowFolderFilePath;
        String rootPath = request.getServletContext().getRealPath("/" + email + "/upload/");
        if (nowFolderId == -1) {
            //根目录
            nowFolderFilePath = rootPath;
            newFilePath = rootPath + newFolderName;
        } else {
            nowFolderFilePath = fileMapper.selectFilePath(nowFolderId);
            newFilePath = nowFolderFilePath + Constants.SLASH + newFolderName;
        }
        //查询有无同名文件夹
        if (fileMapper.selectFile(0, userId, 0L, newFilePath, null, null, -1, -1,-1,0,0) == null) {
            //如没有则创建文件夹
            //获取相对路径后数据库创建文件夹
            fileMapper.insertFile(userId, newFolderName, 0L, 1, nowFolderId, newFilePath, new Date(), new Date().getTime(), getRelativePath(rootPath, nowFolderFilePath), null);
            //文件系统创建文件夹
            java.io.File fileFolderIO = new java.io.File(newFilePath);
           if(fileFolderIO.mkdirs()){
               System.out.println("文件夹创建成功");
           }
        } else {
            //如果有同名文件夹
            //只修改数据库中的创建日期
            fileMapper.updateFile(0, 0, 0, 0, newFilePath, null, null, new Date(), 0, 0, null,null);
        }
    }

    /**
     * 删除文件
     * 你不能仅仅用时间戳uid来区分数据库中的文件，需要同时加上userId来区分各个文件
     * 每次执行uid有关操作后，休息1毫秒。(浏览器已自动处理)
     */
    public void deleteFile(HttpServletRequest request, int fileId, int userId, String email, String recycleFilePathBacktrace, File fileBacktrace) throws IOException {
        //获取待删除文件对象
        File file;
        if (fileBacktrace == null) {
            file = fileMapper.selectFile(fileId, userId, 0L, null, null, null, -1, -1,-1,-1,0);
        } else {
            file = fileBacktrace;
        }

        //获取待还原文件的绝对路径
        String recycleFilePath;
        if (recycleFilePathBacktrace == null) {
            recycleFilePath = request.getServletContext().getRealPath("/" + email + "/recycle/" + file.getFileUid());
        } else {
            recycleFilePath = recycleFilePathBacktrace + Constants.SLASH + file.getFileUid();
        }

        //创建待删除文件的java文件对象
        java.io.File fileInput = new java.io.File(file.getFilePath());
        //创建文件在回收站中的java文件对象
        java.io.File fileOutput = new java.io.File(recycleFilePath);
        //将文件在回收站的绝对路径录入数据库
        fileMapper.updateFile(fileId, 0, 0, 0, null, null, null, null, 0, 0, recycleFilePath,null);
        /*把物理文件移动到回收站中
         * 首先判断该文件是文件还是文件夹
         *   如果是文件，直接移动，数据库标记直接删除
         *   如果是文件夹，
         *       如果回收站没有对应uid文件夹存在（其子文件被还原，该文件夹还原标记1时对应uid文件夹仍在），创建此文件夹
         *       修改文件夹为直接删除
         *       然后list原文件夹所含子文件并遍历，并修改子文件们的数据库为间接删除，传入文件路径和list循环到的对象递归运行函数。
         */
        if (fileInput.isFile()) {
            Files.move(fileInput.toPath(), fileOutput.toPath());
            //然后数据库标记删除
        } else if (fileInput.isDirectory()) {
            if (!fileOutput.exists()) {
                if(fileOutput.mkdirs()){
                    System.out.println("文件夹创建成功");
                }
            }
            java.io.File[] files = fileInput.listFiles();
            assert files != null;
            for (java.io.File childFile : files) {
                //根据路径查询对象
                File childFilePo = fileMapper.selectFile(0, userId, 0L, childFile.getPath(), null, null, -1, -1, -1,-1,0);
                //递归运行此函数，将父文件路径和该文件对象一起递归
                deleteFile(request, childFilePo.getFileId(), userId, email, recycleFilePath, childFilePo);
            }
            //递归运行完后文件夹为空，此时删除
            //第一次删除文件夹直接删除，递归间接删除
            //空文件夹直接删除后修改数据库
            if(fileInput.delete()){
                System.out.println("文件删除成功");
            }
        }
        if (fileBacktrace == null && recycleFilePathBacktrace == null) {
            fileMapper.updateFile(fileId, 1, 0, 0, null, null, null, null, 0, 0, null,new Date());
        } else {
            fileMapper.updateFile(fileId, 0, 1, 0, null, null, null, null, 0, 0, null,new Date());
        }
    }

    /**
     * @param fileId 文件Id
     * @param indirectFlag 如果是递归还原文件，此标志为1.否则为-1
     */
    public void recoverFile(int fileId,int userId,int indirectFlag) throws IOException {
        File file;
        if(indirectFlag == -1){
            file = fileMapper.selectFile(fileId, userId, 0L, null, null, null, 1, 0,-1,-1,0);
        }else{
            file = fileMapper.selectFile(fileId, userId, 0L, null, null, null, 0, 1,-1,-1,0);
        }

        if(file.getFileSuperId() != -1){
            //检测上级目录是否已经被删除
            File superFolder = fileMapper.selectFile(file.getFileSuperId(),userId,null,null,null,null,1,1,0,0,0);
            if(superFolder!=null){
                //代表该文件的父级目录被删除了，文件系统进行创建文件夹操作和数据库递归打还原标记
                java.io.File superFolderIO = new java.io.File(superFolder.getFilePath());
                if(superFolderIO.mkdirs()){
                    System.out.println("文件夹创建成功");
                }

                //对象克隆
                File fileTemp = (File)file.clone();
                //如果存在未还原的父目录，递归往上修改恢复标志
                while(fileTemp.getFileSuperId() != -1){
                    File superTemp = fileMapper.selectFile(fileTemp.getFileSuperId(), userId, null, null, null, null, 1, 1, 0,0,0);
                    if(superTemp.getHardDelFlag() != 1){
                        fileMapper.updateFile(superTemp.getFileId(),0,0,1,null,null,null,null,0,-1,null,null);
                    }else{
                        fileMapper.updateFile(superTemp.getFileId(),-1,-1,1,null,null,null,null,0,-1,null,null);
                    }
                      fileTemp = superTemp;
                }
            }
        }

        //获得被还原文件的Java文件对象,cyclePath指文件按在回收站的物理路径
        java.io.File fileInput = new java.io.File(file.getCyclePath());
        java.io.File fileOutput = new java.io.File(file.getFilePath());
        /*判断还原的是文件还是目录
         *   如果还原的是文件，检查目的地文件是否有同名文件
         *       如有则进行同名冲突处理，文件名和路径都要处理
         *       如果没有则不做操作
         *       然后移动文件，修改数据库删除标志为-1。
         *   如果还原的是文件夹，检查目的地是否有同名文件夹
         *       如有则检测本文件夹还原标志是否为1
         *       （如果是1的话，代表文件夹被还原过，修改文件夹的delFlag族和recoverFlag为正常。）(如果不是1的话,还原文件夹的所有子文件修改fileSuperId)，然后将子文件move到同名文件夹下。删除被还原文件夹(如果还原标记不是1，也删除数据库）
         *
         */
        if (fileInput.isFile()) {
            //进行同名冲突处理
            fileOutput = nameConflict(fileOutput, file);
            // 然后移动文件，修改数据库删除标志为-1
            if (fileInput.exists()) {
                Files.move(fileInput.toPath(), fileOutput.toPath());
            }
            fileMapper.updateFile(fileId, -1, -1, 0, fileOutput.getPath(), null, fileOutput.getName(), null, 0, 0, null,null);
        } else if (fileInput.isDirectory()) {

//            if (file.getRecoverFlag() == 1) {
//                fileMapper.updateFile(fileId, -1, -1, 0, null, null, null, null, 0, -1, null);
//            }
            //查询被还原文件夹的子文件们
            List<File> files = fileMapper.selectFiles(userId, null, 0, fileId, 0, 1, -1);
            //查询目的地有无冲突的文件夹,结尾判断用
            File conflictFile = fileMapper.selectFile(0, userId, 0L, file.getFilePath(), null, null, -1, -1,-1,0,0);
            if (files != null) {
                for (File chileFile : files) {
                    //如果是存在冲突文件夹非被动还原，将还原文件夹的所有子文件修改fileSuperId
                    if (file.getRecoverFlag() != 1 && conflictFile != null) {
                        fileMapper.updateFile(chileFile.getFileId(), 0, 0, 0, null, null, null, null, conflictFile.getFileId(), -1, null,null);
                    }
                    //在递归前，目的地处创建父文件夹，防止NoSuchFile错误
                    if (!fileOutput.exists()){
                        if(fileOutput.mkdirs()){
                            System.out.println("文件夹创建成功");
                        }
                    }
                    //递归执行还原函数
                    recoverFile(chileFile.getFileId(),userId,1);
                }
            }
            //如果是存在冲突文件夹且非被动还原，数据库中删除回收站的目录
            //文件夹还原完毕后在(数据库)和文件系统中自删除
            if (file.getRecoverFlag() != 1 && conflictFile != null) {
                fileMapper.deleteFile(fileId, 1, 1);
            }else{
                //如果文件夹有被动还原过或者无被动还原且无冲突，修改文件夹的delFlag族和recoverFlag为-1,重置删除时间,并创建同样的文件夹（空文件夹在上面不会执行mkdirs这里mk一下）
                fileMapper.updateFile(fileId, -1, -1, 0, null, null, null, null, 0, -1, null,null);
                if(fileOutput.mkdirs()){
                    System.out.println("文件夹创建成功");
                }
            }
            if(fileInput.delete()){
                System.out.println("文件删除成功");
            }
        }
    }

    /**
     * 文件的彻底删除
     */
    public Long hardDelete(int fileId,int userId,int recursionFlag) {
        /*
        文件系统中在回收站的文件或文件夹要直接delete()

        数据库中,先更新用户容量，再对文件执行delete操作，
        文件夹添加hard_del_flag标记并遍历递归

        如果该文件夹的还原标志为1，则遍历旗下被间接删除的文件，遍历完后根文件夹的删除和恢复标志改为-1
        */

        File file = fileMapper.selectFile(fileId, userId, 0L, null, null, null, 0, 0,-1,-1,0);
        java.io.File fileIO = new java.io.File(file.getCyclePath());


        if (fileIO.isFile()) {
            //更新用户容量
            if(file.getDelFlag() != 1 || recursionFlag != 1){
                userMapper.updateUser(file.getUserId(), null, null, null, 0L, -fileIO.length(), 0L, 0L, null, 0, 0);
                //数据库中的文件执行delete操作
                fileMapper.deleteFile(fileId, 1, 1);
                //完事后,删除服务器中的文件
                if(fileIO.delete()){
                    System.out.println("文件删除成功");
                }
            }
        } else if (fileIO.isDirectory()) {
            //文件夹添加hard_del_flag标记
            fileMapper.updateFile(fileId, 0, 0, 0, null, null, null, null, 0, 1, null,file.getDeleteTime());
            //遍历递归子文件
            List<File> files = fileMapper.selectFiles(userId, null, 0, file.getFileId(), 0, 1, -1);
            for (File childFile : files) {
                hardDelete(childFile.getFileId(),userId,1);
            }
            //完事后,删除服务器中的文件
            if(recursionFlag == 1){
                fileIO.delete();
            }else if(recursionFlag == -1 && file.getRecoverFlag() == 1){
                fileMapper.updateFile(fileId, -1, -1, -1, null, null, null, null, 0, -1, null,null);
            }
        }
        //返回数据库中的用户已用容量
        return userMapper.selectUser(file.getUserId(), null, 0).getUseSpace();
    }

    public void uploadFile(HttpServletRequest request, MultipartFile file, int nowFolderId, Long fileUid, String fileMD5, int userId, String email) throws IOException {
        //获取文件在服务器上的路径
        String filePath;
        String superFolderPath = getSuperPathByNowFolderId(request,nowFolderId,email,userId);
        String uploadRootPath = request.getServletContext().getRealPath("/" + email + "/upload/");

        filePath = superFolderPath + file.getOriginalFilename();

        String relativizePath = getRelativePath(uploadRootPath, superFolderPath);

        //创建文件对象
        java.io.File fileOutput = new java.io.File(filePath);
        //创建数据库对象
        File fileInfo = new File(userId, file.getOriginalFilename(), file.getSize(), nowFolderId, fileOutput.getPath(), new Date(), -1, -1, -1, -1, -1, fileUid, relativizePath, null, fileMD5,null,-1,0);
        //重名冲突检测与处理
        fileOutput = nameConflict(fileOutput, fileInfo);
        //传输文件到服务器
        file.transferTo(fileOutput);
        //插入数据库
        fileMapper.insertFile(fileInfo.getUserId(), fileInfo.getFileName(), fileInfo.getFileSize(), fileInfo.getFolderType(), fileInfo.getFileSuperId(), fileInfo.getFilePath(), fileInfo.getCreateTime(), fileInfo.getFileUid(), fileInfo.getRelativePath(), fileInfo.getFileMD5());
    }

    public void download(HttpServletResponse response, int fileId, String token) throws IOException, GeneralSecurityException {
        if (token == null || token.isEmpty()) {
            response.sendError(401);
            return;
        }
        //解密token
        token = AESUtil.AESDecode(token);

        File downloadFile = fileMapper.selectFile(fileId, 0, 0L, null, null, null, -1, -1,-1,-1,0);
        int redisUserId = JSON.parseObject(RedisUtil.get(token), SessionUser.class).getUserId();
        int tempFileUserId = downloadFile.getUserId();
        if (redisUserId != tempFileUserId) {
            response.sendError(401);
            return;
        }

        InputStream inputStream = Files.newInputStream(new java.io.File(downloadFile.getFilePath()).toPath());
        OutputStream outputStream = response.getOutputStream();
        byte[] buffer = new byte[1024 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        //设置type为文件下载的二进制数据
        String contentType = "application/octet-stream";
        //设置文件的下载名
        response.setHeader("Content-Disposition", "attachment;filename=\"" + downloadFile.getFileName() + "\"");
        response.setContentType(contentType);
        response.addHeader("Content-Length","" + (downloadFile.getFileSize()));
        response.flushBuffer();
    }

    public void uploadCheck(HttpServletResponse response, int userId, Long fileUid, String fileMD5) {
        File file = fileMapper.selectFile(0, userId, fileUid, null, null, fileMD5, 0, 0,-1,-1,0);
        if (file != null) {
            //TODO 更新数据库用户空间
            userMapper.updateUser(userId,null,null,null,0L,file.getFileSize(),0L,0L,null,0,0);
        } else {
            //上传失败，可能是传输过程文件损坏，请重试
            response.setStatus(250);
        }
    }

    public void moveFile(HttpServletRequest request, HttpServletResponse response, int fileId, int desFolderId, String email, int userId) throws IOException {
        File file = fileMapper.selectFile(fileId, userId, 0L, null, null, null, -1, -1,-1,-1,0);
        String desFilePath;
        String desFolderPath;
        if (desFolderId == -1) {
            desFolderPath = request.getServletContext().getRealPath("/" + email + "/upload/");
        } else {
            desFolderPath = fileMapper.selectFile(desFolderId, userId, 0L, null, null, null, -1, -1,-1,-1,0).getFilePath() + Constants.SLASH;
        }
        desFilePath = desFolderPath + file.getFileName();

        if (desFolderPath.equals(file.getFilePath() + Constants.SLASH)) {
            //嵌套移动阻止
            response.setStatus(250);
            return;
        }

        java.io.File fileInput = new java.io.File(file.getFilePath());
        java.io.File fileOutput = new java.io.File(desFilePath);
        String relativePath = getRelativePath(request.getServletContext().getRealPath("/" + email + "/upload/"), desFolderPath);
        /*
         *  如果是文件，进行重名冲突处理后移动文件，数据库修改fileName、Path、relativePath和fileSuperId信息。
         *  如果是文件夹，检测目标地有无重名文件夹，
         *      如果有，将被移动文件夹的所有子文件遍历递归并修改父文件夹Id,然后移动到目标文件夹中。最后等遍历空则删除文件夹
         *      如果没有，数据库中修改文件夹Path和relativePath信息，将被移动文件夹的所有子文件遍历递归
         * */
        if (fileInput.isFile()) {
            fileOutput = nameConflict(fileOutput, file);
            Files.move(fileInput.toPath(), fileOutput.toPath());
            //数据库修改
            //获取相对路径

            fileMapper.updateFile(fileId, -1, -1, 0, desFilePath, relativePath, file.getFileName(), file.getCreateTime(), desFolderId, 0, null,null);
        } else if (fileInput.isDirectory()) {
            File tempFolder;
            if (!fileOutput.exists()) {
                //如果目标无重名文件夹，优先修改文件夹在数据库中的Path、relativePath信息方便递归。并顺带修改其父文件夹Id信息为目标Id
                fileMapper.updateFile(fileId, -1, -1, 0, desFilePath, relativePath, null, null, desFolderId, 0, null,null);
                tempFolder = file;
            } else {
                //如果目标有重名文件夹，获取重名文件夹对象
                tempFolder = fileMapper.selectFile(0, userId, 0L, fileOutput.getPath(), file.getFileName(), null, -1, -1,-1,0,0);
            }
            //文件夹遍历前进行目的地目录创建操作

            List<File> files = fileMapper.selectFiles(userId, null, 0, fileId, -1, -1, -1);
            File conflictFile = fileMapper.selectFile(0,userId,0L,fileOutput.getPath(),null,null,-1,-1,-1,-1,0);
            if (conflictFile != null) {
                /*
                 * 验证重名冲突不能用java文件对象的exists，否则自己移动自己时有误判
                 * 从数据库去就要到数据库来而不是不到数据库来
                 */
                fileMapper.deleteFile(fileId, -1, -1);
            }
            //文件夹遍历前在目的地创建父文件夹
            if(fileOutput.mkdirs()){
                System.out.println("文件夹创建成功");
            }
            for (File childFile : files) {
                moveFile(request, response, childFile.getFileId(), tempFolder.getFileId(), email, userId);
            }
            //遍历完后文件系统删除空文件夹
            if(fileInput.delete()){
                System.out.println("文件删除成功");
            }
        }
    }

    public String getBreadcrumb(HttpServletRequest request, int userId, int nowFolderId, String email) {
        if (nowFolderId == -1) {
            return Constants.SLASH;
        } else {
            return getRelativePath(
                    request.getServletContext().getRealPath("/" + email + "/upload/"),
                    fileMapper.selectFilePath(nowFolderId)
            );
        }
    }

    public List<String> beforeSplitUpload(HttpServletRequest request,int userId,String email,int nowFolderId,String fileName,Long fileSize,Long fileUid,String fileMD5 ,int splitCount){
        /*先检测数据库是否支持续传
            如果支持，跳过创建文件夹步骤，直接开始扫描确实的文件片，发送index数组给前端
            如果不支持，则创建续传文件夹，插入数据库条目，发送[-1]给前端，代表发送全部分片
         */
        File fileCheck = fileMapper.selectFile(0, userId, fileUid, null, null, fileMD5, 0, 0, -1, 1, splitCount);
        if(fileCheck != null){
            //该文件分片传输未完整，返回未上传完成的分片index数组
            String splitPath = request.getServletContext().getRealPath("/" + email + "/split/") + fileMD5;
            java.io.File fileIO = new java.io.File(splitPath);
            java.io.File[] childFiles;
            List<java.io.File> files;
            //将查询到的File数组转化为List
            if((childFiles = fileIO.listFiles()) != null){
                files = List.of(childFiles);
            }else{
                List<String> temp = new ArrayList<>();
                temp.add("-1");
                //只装个-1，表示需要所有分片
                return temp;
            }
            List<String> collect = files.stream().map(java.io.File::getName).toList();
            List<String> list = new ArrayList<>(Stream.iterate(0, x -> x + 1).limit(splitCount).map(Object::toString).toList());
            //获取未上传的分片编号并回复给前端
            list.removeAll(collect);
            return list;
        }else{
            //新文件
            String splitPath = request.getServletContext().getRealPath("/" + email + "/split/") + fileMD5;
            java.io.File splitFolder = new java.io.File(splitPath);
            splitFolder.mkdirs();

            String superPath = getSuperPathByNowFolderId(request, nowFolderId, email, userId);
            String filePath = superPath + fileName;
            String rootPath = request.getServletContext().getRealPath("/" + email + "/upload/");
            String relativePath = getRelativePath(rootPath,filePath);

            fileMapper.insertFile(userId,fileName,fileSize,-1,nowFolderId,filePath,new Date(),fileUid,relativePath,fileMD5);

            List<String> temp = new ArrayList<>();
            temp.add("-1");
            //只装个-1，表示需要所有分片
            return temp;
        }
    }

    public void splitUpload(HttpServletRequest request, int index,MultipartFile chunk,String fileMD5,String email) throws IOException {
        //存储上传的chunk
        String chunkFolderPath = request.getServletContext().getRealPath("/" + email + "/split/") + fileMD5;
        java.io.File chunkFile = new java.io.File(chunkFolderPath + Constants.SLASH + index);
        chunk.transferTo(chunkFile);
    }


    /**
     * 299为秒传命中标识，250为秒传未命中标识
     * @param request 获取路径用
     * @param response 发送状态
     * @param fileMD5  文件md5
     * @param nowFolderId 当前父文件夹Id
     * @param email 登录名
     */
    public void flashTransfer(HttpServletRequest request, HttpServletResponse response, String fileMD5, int nowFolderId, String email, int userId, Long fileUid) throws IOException {
        /*去数据库中 检测网盘内是否有相同MD5文件
         * 如果有，进行重名检测与处理，数据库复制字段，更改父文件Id和其Path/relativePath，fileUid使用当前时间戳
         * 文件系统copy文件。
         * */
        File selectFile = fileMapper.selectFile(0, 0, 0L, null, null, fileMD5, -1, -1,-1,-1,0);
        if (selectFile != null) {
            //修复408行fileMD5返回null的奇怪现象
            selectFile.setFileMD5(fileMD5);

            String flashFileFolderPath;
            if (nowFolderId != -1) {
                flashFileFolderPath = fileMapper.selectFile(nowFolderId, 0, 0L, null, null, null, -1, -1,-1,0,0).getFilePath() + Constants.SLASH;
            } else {
                //为根目录
                flashFileFolderPath = request.getServletContext().getRealPath("/" + email + "/upload/");
            }

            String flashFilePath = flashFileFolderPath + selectFile.getFileName();
            //获取相对路径
            String relativePath = getRelativePath(request.getServletContext().getRealPath("/" + email + "/upload/"), flashFileFolderPath);

            File flashFile = new File(userId, selectFile.getFileName(), selectFile.getFileSize(), nowFolderId, flashFilePath, new Date(), selectFile.getFolderType(), selectFile.getDelFlag(), selectFile.getDelIndirectFlag(), selectFile.getHardDelFlag(), selectFile.getRecoverFlag(), fileUid, relativePath, selectFile.getCyclePath(), selectFile.getFileMD5(),null,-1,0);
            java.io.File fileInput = new java.io.File(selectFile.getFilePath());
            java.io.File fileOutput = new java.io.File(flashFilePath);

            fileOutput = nameConflict(fileOutput, flashFile);
            //文件复制及数据库修改
            Files.copy(fileInput.toPath(), fileOutput.toPath());
            fileMapper.insertFile(flashFile.getUserId(), flashFile.getFileName(), flashFile.getFileSize(), flashFile.getFolderType(), flashFile.getFileSuperId(), flashFile.getFilePath(), flashFile.getCreateTime(), flashFile.getFileUid(), flashFile.getRelativePath(), flashFile.getFileMD5());
            //秒传命中标识为200

        } else {
            //秒传未命中标识
            response.setStatus(250);
        }
    }

    /**
     *
     * @param shortPath 传入参照路径
     * @param LongPath  请传入文件的父文件夹Path
     * @return 返回带斜杠的相对路径
     */
    private String getRelativePath(String shortPath, String LongPath) {
        //获取相对路径后数据库创建文件夹
        Path path1 = Paths.get(shortPath);
        Path path2 = Paths.get(LongPath);
        return Constants.SLASH + path1.relativize(path2);
    }

    /**
     * 重名处理的函数，若有重名冲突，函数更改引用文件对象fileCheck的路径名（加后缀）和更改fileInfo中的文件名和路径名。
     * 若不存在冲突则不变
     *
     * @param fileCheck 待更改文件的Java文件对象
     * @param fileInfo  待更改文件的po对象
     * @return 修改完成的文件对象
     */
    private java.io.File nameConflict(java.io.File fileCheck, File fileInfo) {
        int index = 1;
        String backupName = fileInfo.getFileName();
        String newFileName = fileInfo.getFileName();
        String newFileCheckPath = fileInfo.getFilePath();
        while (fileCheck.exists()) {
            //如果有同名文件,该对象的更名，这里应该加在文件类别名字之前
            String[] split = backupName.split("\\.");
            split[0] = split[0] + "(" + index + ")";
            newFileName = String.join(".", split);
            newFileCheckPath = fileNamePathUpdate(fileCheck, newFileName);
            fileCheck = new java.io.File(newFileCheckPath);
            index++;
        }
        fileInfo.setFileName(newFileName);
        fileInfo.setFilePath(newFileCheckPath);
        return fileCheck;
    }

    /**
     * 文件对象路径中的文件名将更改成传入的文件名。
     *
     * @param fileOriginal  文件原路径
     * @param newFileName 新文件名
     * @return 返回带有新文件名的文件路径
     */
    private String fileNamePathUpdate(java.io.File fileOriginal, String newFileName) {
        //该对象在数据库更新path
        String[] split;
        if (Constants.SLASH.equals("\\")) {
            // "\\\\"
            split = fileOriginal.getPath().split(Constants.SLASH + Constants.SLASH);
        } else {
            // "/"
            split = fileOriginal.getPath().split(Constants.SLASH);
        }
        split[split.length - 1] = newFileName;
        return String.join(Constants.SLASH, split);
    }

    private String getSuperPathByNowFolderId(HttpServletRequest request,int nowFolderId,String email,int userId){
        //获取文件在服务器上的路径
        String superFolderPath;
        String uploadRootPath = request.getServletContext().getRealPath("/" + email + "/upload/");
        if (nowFolderId == -1) {
            superFolderPath = uploadRootPath;
        } else {
            superFolderPath = fileMapper.selectFile(nowFolderId, userId, 0L, null, null, null, -1, -1,-1,0,0).getFilePath() + Constants.SLASH;
            //因为request获取的文件对象最后面有斜线。不需要额外增加斜线
        }
        return superFolderPath;
    }
}
