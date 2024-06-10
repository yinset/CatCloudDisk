package icu.niunai.service;

import icu.niunai.entity.po.File;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface FileService {
    File selectFile(int fileId,int userId,Long fileUid,String filePath,String fileName,String fileMD5,int delFlag,int delIndirectFlag,int hardDelFlag, int splitUploadFlag, int splitUploadCount);

    void createFolder(HttpServletRequest request, int userId, String newFolderName, int nowFolderId, String email);

    List<File> selectFiles(int userId, String fileNameLike, int folderType, int fileSuperId, int delFlag, int delIndirectFlag, int hardDelFlag);

    List<File> back(HttpServletResponse response,int userId, int delFlag, int nowFolderId, int folderType);

    void deleteFile(HttpServletRequest request, int fileId, int userId, String email, String recycleFilePathBacktrace, File fileBacktrace) throws IOException;

    void recoverFile(int fileId,int userId,int indirectFlag) throws IOException;

    Long hardDelete(int fileId,int userId,int recursionFlag);

    void uploadFile(HttpServletRequest request, MultipartFile file, int nowFolderId, Long fileUid, String fileMD5, int userId, String email) throws IOException;

    void flashTransfer(HttpServletRequest request, HttpServletResponse response, String fileMD5, int nowFolderId, String email, int userId, Long fileUid) throws IOException;

    void download(HttpServletResponse response, int fileId, String token) throws IOException, GeneralSecurityException;

    void uploadCheck(HttpServletResponse response, int userId, Long fileUid, String fileMD5);

    void moveFile(HttpServletRequest request, HttpServletResponse response, int fileId, int desFolderId, String email, int userId) throws IOException;

    String getBreadcrumb(HttpServletRequest request, int userId, int nowFolderId, String email);

    List<String> beforeSplitUpload(HttpServletRequest request,int userId,String email,int nowFolderId,String fileName,Long fileSize,Long fileUid,String fileMD5 ,int splitCount);

    void splitUpload(HttpServletRequest request, int index,MultipartFile chunk,String fileMD5,String email) throws IOException;
}
