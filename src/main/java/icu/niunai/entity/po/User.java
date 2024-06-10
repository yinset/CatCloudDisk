package icu.niunai.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;


@Data
public class User {
    private int userId;
    private String nickName;
    private String email;
    private String password;
    private String avatarPath;
    private Date createTime;
    private Long useSpace;
    private Long totalSpace;
    private int ban;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    private int isAdmin;

    public User(int userId, String nickName, String email, String password, String avatarPath, Date createTime, Long useSpace, Long totalSpace, int ban, Date birthday, int isAdmin) {
        this.userId = userId;
        this.nickName = nickName;
        this.email = email;
        this.password = password;
        this.avatarPath = avatarPath;
        this.createTime = createTime;
        this.useSpace = useSpace;
        this.totalSpace = totalSpace;
        this.ban = ban;
        this.birthday = birthday;
        this.isAdmin = isAdmin;
    }

    public User(){

    }
}

