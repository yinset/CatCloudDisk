package icu.niunai.mapper;

import icu.niunai.entity.po.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface FileMapper {
    File selectFile(@Param("fileId")int fileId,
                    @Param("userId")int userId,
                    @Param("fileUid")Long fileUid,
                    @Param("filePath")String filePath,
                    @Param("fileName")String fileName,
                    @Param("fileMD5")String fileMD5,
                    @Param("delFlag")int delFlag,
                    @Param("delIndirectFlag")int delIndirectFlag,
                    @Param("hardDelFlag") int hardDelFlag,
                    @Param("splitUploadFlag") int splitUploadFlag,
                    @Param("splitUploadCount") int splitUploadCount);

    /**
     * 查询文件群
     * @param userId 用户Id
     * @param fileNameLike 填写的话前后自动添加通配符
     * @param folderType  是否文件夹
     * @param fileSuperId -1代表根目录，0代表*
     * @param delFlag   是否直接删除
     * @param delIndirectFlag 是否间接删除
     * @param  hardDelFlag 是否彻底删除
     * @return 返回文件List
     */
    List<File> selectFiles(@Param("userId")int userId,
                           @Param("fileNameLike")String fileNameLike,
                           @Param("folderType")int folderType,
                           @Param("fileSuperId")int fileSuperId,
                           @Param("delFlag")int delFlag,
                           @Param("delIndirectFlag")int delIndirectFlag,
                           @Param("hardDelFlag")int hardDelFlag);


    void insertFile(@Param("userId")int userId,
                    @Param("fileName")String fileName,
                    @Param("fileSize")Long fileSize,
                    @Param("folderType")int folderType,
                    @Param("fileSuperId")int fileSuperId,
                    @Param("filePath")String filePath,
                    @Param("createTime")Date createTime,
                    @Param("fileUid")Long fileUid,
                    @Param("relativePath")String relativePath,
                    @Param("fileMD5")String fileMD5);

    /**
     *
     * @param fileId 文件Id
     * @param delFlag 是否直接删除
     * @param delIndirectFlag 是否彻底删除
     * @param recoverFlag 是否还原
     * @param filePath 文件路径
     * @param relativePath 文件相对根目录路径
     * @param fileName 文件名
     * @param createTime 文件创建时间
     * @param fileSuperId 父文件夹Id 根目录为-1
     * @param hardDelFlag 是否被彻底删除
     * @param cyclePath 回收站文件夹
     * @param deleteTime 被删除的时间
     */
    void updateFile(@Param("fileId")int fileId,
                    @Param("delFlag")int delFlag,
                    @Param("delIndirectFlag")int delIndirectFlag,
                    @Param("recoverFlag")int recoverFlag,
                    @Param("filePath")String filePath,
                    @Param("relativePath")String relativePath,
                    @Param("fileName")String fileName,
                    @Param("createTime")Date createTime,
                    @Param("fileSuperId")int fileSuperId,
                    @Param("hardDelFlag")int hardDelFlag,
                    @Param("cyclePath")String cyclePath,
                    @Param("deleteTime") Date deleteTime);


    void deleteFile(@Param("fileId")int fileId,
                    @Param("delFlag")int delFlag,
                    @Param("delIndirectFlag")int delIndirectFlag);



    String selectFilePath(int fileId);
}

