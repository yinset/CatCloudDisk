package icu.niunai.controller;

import icu.niunai.entity.dto.SessionUser;
import icu.niunai.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Map<String, Object> login(HttpServletRequest request, String email, String password) throws GeneralSecurityException {
        return userService.login(request,email,password);
    }

    @PostMapping("/register")
    public void register(HttpServletResponse response, String email, String password, String nickName, String birthday) throws ParseException {
        if(birthday != null){
            DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
            Date date = fmt.parse(birthday);
            userService.register(response,email,password,nickName,date);
        }else{
            userService.register(response,email,password,nickName,null);
        }
    }

    @PostMapping("/setAvatar")
    public String setAvatar(HttpServletRequest request,@RequestParam("avatar") MultipartFile file,@RequestParam("userId") int userId,@RequestParam("email") String email) throws IOException {
        return userService.setAvatar(request,file,userId,email);
    }

    @PostMapping("/getAvatar")
    public void getAvatar(HttpServletRequest request,HttpServletResponse response,int userId) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        userService.getAvatar(request,response,userId);
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());
    }

    @PostMapping("/updateUser")
    public void updateUser(int userId,String nickName,String password,String birthdayString,String avatarPath,HttpServletRequest request) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date birthday = dateFormat.parse(birthdayString);
        userService.updateUser(userId,nickName,password,birthday,avatarPath,request);
    }

    //用于修改密码前旧密码检查
    @PostMapping("/passwordCheck")
    public void passwordCheck(HttpServletRequest request,HttpServletResponse response,String oldPassword,int userId){
        userService.passwordCheck(request,response,oldPassword,userId);
    }

    @PostMapping("/selectUsers")
    public List<SessionUser> selectUsers(HttpServletRequest request, int userId){
        return userService.selectUsers(request,userId);
    }

    @PostMapping("/adminUpdate")
    public void adminUpdate(HttpServletRequest request,HttpServletResponse response,int userId,float spaceVariation,Boolean ban,Boolean isAdmin) throws GeneralSecurityException {
        int banInt = -1;
        int isAdminInt = -1;

        if(ban){banInt = 1;}
        if(isAdmin){isAdminInt = 1;}
        userService.adminUpdate(request,response,userId,spaceVariation,banInt,isAdminInt);
    }
}
