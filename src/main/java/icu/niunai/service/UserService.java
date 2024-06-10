package icu.niunai.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import icu.niunai.entity.dto.SessionUser;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface UserService {
    Map<String, Object> login(HttpServletRequest request, String email, String password) throws GeneralSecurityException;

    void register(HttpServletResponse response, String email, String password, String nickName, Date birthday);

    String setAvatar(HttpServletRequest request,MultipartFile file, int userId, String email) throws IOException;

    void getAvatar(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException;

    void updateUser(int userId,String nickName,String password,Date birthday,String avatarPath,HttpServletRequest request);

    void passwordCheck(HttpServletRequest request, HttpServletResponse response, String oldPassword, int userId);

    List<SessionUser> selectUsers(HttpServletRequest request, int userId);

    void adminUpdate(HttpServletRequest request, HttpServletResponse response, int userId, float spaceVariation, int ban, int isAdmin) throws GeneralSecurityException;
}
