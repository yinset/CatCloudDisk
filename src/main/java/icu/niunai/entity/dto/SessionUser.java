package icu.niunai.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class SessionUser {
    private int userId;
    private String email;
    private String nickName;
    private int isAdmin;
    private String avatarPath;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    private Long useSpace;
    private Long totalSpace;
    private int ban;

    public SessionUser(int userId, String email, String nickName, int isAdmin, String avatarPath, Date birthday, Long useSpace, Long totalSpace, int ban) {
        this.userId = userId;
        this.email = email;
        this.nickName = nickName;
        this.isAdmin = isAdmin;
        this.avatarPath = avatarPath;
        this.birthday = birthday;
        this.useSpace = useSpace;
        this.totalSpace = totalSpace;
        this.ban = ban;
    }
}
