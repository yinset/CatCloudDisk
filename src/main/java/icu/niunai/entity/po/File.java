package icu.niunai.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class File implements Cloneable {
    private int fileId;
    private int userId;
    private String fileName;
    private Long fileSize;
    /**
     * -1代表根目录
     */
    private int fileSuperId;  //所属文件夹的file_id
    private String filePath;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
    private int folderType; //0:文件 1:目录
    private int delFlag; //1为删除 -1未删除
    private int delIndirectFlag;//在文件夹里被间接删除
    private int hardDelFlag;
    private int recoverFlag;
    private Long fileUid;
    private String relativePath;
    private String cyclePath;
    private String fileMD5;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date deleteTime;
    private int splitUploadFlag;
    private int splitUploadCount;
    public File(int userId, String fileName, Long fileSize, int fileSuperId, String filePath, Date createTime, int folderType, int delFlag, int delIndirectFlag, int hardDelFlag, int recoverFlag, Long fileUid, String relativePath, String cyclePath, String fileMD5,Date deleteTime,int splitUploadFlag,int splitUploadCount) {
        this.userId = userId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileSuperId = fileSuperId;
        this.filePath = filePath;
        this.createTime = createTime;
        this.folderType = folderType;
        this.delFlag = delFlag;
        this.delIndirectFlag = delIndirectFlag;
        this.hardDelFlag = hardDelFlag;
        this.recoverFlag = recoverFlag;
        this.fileUid = fileUid;
        this.relativePath = relativePath;
        this.cyclePath = cyclePath;
        this.fileMD5 = fileMD5;
        this.deleteTime = deleteTime;
        this.splitUploadFlag = splitUploadFlag;
        this.splitUploadCount = splitUploadCount;
    }
    public File(){

    }

    @Override
    public Object clone(){
        File file = null;
        try {
            file = (File)super.clone();
        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        return file;
    }
}
