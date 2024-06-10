package icu.niunai.service.impl;

import com.alibaba.fastjson2.JSON;
import icu.niunai.entity.dto.SessionUser;
import icu.niunai.service.UserService;
import icu.niunai.utils.AESUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;
import icu.niunai.constant.Constants;
import icu.niunai.entity.po.User;
import icu.niunai.mapper.UserMapper;
import icu.niunai.utils.RedisUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.*;

import static java.util.concurrent.TimeUnit.DAYS;
import static icu.niunai.utils.AESUtil.AESEncode;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    public Map<String, Object> login(HttpServletRequest request, String email, String password) throws GeneralSecurityException {
        User user = userMapper.selectUser(0, email, 0);
        //身份认证，首先检查账号密码，其次检测token有效性来检测是否新建token
        if (user == null || !user.getPassword().equals(password)) {
            return null;
        }
        SessionUser sessionUser = new SessionUser(user.getUserId(), user.getEmail(), user.getNickName(), user.getIsAdmin(), user.getAvatarPath(), user.getBirthday(), user.getUseSpace(), user.getTotalSpace(), user.getBan());
        Map<String, Object> map = new HashMap<>();
        String token = request.getHeader("token");
        if (token != null) {
            String redisString = RedisUtil.get(token);
            SessionUser redisSessionUser = JSON.parseObject(redisString, SessionUser.class);
            //当此用户使用的token与redis所存的token对应userid相符才重用token
            if (redisSessionUser == null || user.getUserId() != redisSessionUser.getUserId()) {
                //验证失败则新建token
                token = UUID.randomUUID().toString();
            }

        } else {
            token = UUID.randomUUID().toString();
        }
        //redis的token不加密
        RedisUtil.setEx(token, JSON.toJSONString(sessionUser), 7, DAYS);
        //前端的token加密
        token = AESEncode(token);
        map.put("token", token);
        map.put("tokenUser", sessionUser);

        //return前，将用户的文件存放、头像和回收站文件夹创建好
        File upload = new File(request.getServletContext().getRealPath("/" + email + "/upload/"));
        File avatar = new File(request.getServletContext().getRealPath("/" + email + "/avatar/"));
        File recycle = new File(request.getServletContext().getRealPath("/" + email + "/recycle/"));
        File split = new File(request.getServletContext().getRealPath("/" + email + "/split/"));

        upload.mkdirs();
        avatar.mkdirs();
        recycle.mkdirs();
        split.mkdirs();
        return map;
    }

    public void register(HttpServletResponse response, String email, String password, String nickName, Date birthday) {
        if (userMapper.selectUser(0, email, 0) == null)
            userMapper.insertUser(email, password, nickName, birthday, new Date());
        else
            //登录名已存在
            response.setStatus(250);
    }

    //返回头像路径
    public String setAvatar(HttpServletRequest request,MultipartFile file, int userId, String email) throws IOException {
        String path = request.getServletContext().getRealPath("/" + email + "/avatar/");
        File avatarFolder = new File(path);
        //头像文件夹删除创建
        if (avatarFolder.exists() &&  avatarFolder.delete()) System.out.println("用户头像目录删除");
        if(avatarFolder.mkdirs()) System.out.println("用户头像目录创建");
        String avatarFilePath = path + file.getOriginalFilename();

        //获取不带图片后缀的文件path
        String[] split;
        if(Constants.SLASH.equals("\\")){
            split = avatarFilePath.split(Constants.SLASH+Constants.SLASH);
        }else{
            split = avatarFilePath.split(Constants.SLASH);
        }

        split[split.length-1] = split[split.length-1].split("\\.")[0];
        File avatarFile = new File(String.join(Constants.SLASH,split));


        //MultipartFile写入文件对象。
        file.transferTo(avatarFile);

        String zipFilePath = avatarFile.getPath() + "(zipped)";

        //压缩头像
        //scale和size只能选一种来设置。同时出现会报错
        Thumbnails.of(avatarFile.getPath())
                .size(256,256)
                .outputQuality(0.2f)
                .outputFormat("jpg")
                .toFile(zipFilePath);

        //删除未压缩头像
        if(avatarFile.delete()) System.out.println("未压缩头像删除");
        //修改用户头像地址
        userMapper.updateUser(userId, null, null, null, 0L, 0L, 0L, 0L, zipFilePath+".jpg", 0, 0);
        return zipFilePath + ".jpg";
    }

    public void getAvatar(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        //首先获取头像绝对路径
        User user = userMapper.selectUser(userId, null, 0);
        String avatarPath = user.getAvatarPath();
        //定位头像
        if (avatarPath != null && !avatarPath.isEmpty()) {
            File avatarFile = new File(avatarPath);
            //流约等于容器的意思
            //创建输入流（从哪里输入）
            InputStream inputStream = Files.newInputStream(avatarFile.toPath());
            //创建输出流（输出到哪里）
            OutputStream outputStream = response.getOutputStream();

            //从输入流写进输出流
            //缓冲区
            byte[] buffer = new byte[1024];
            //已读字节数，用于
            int bytesRead;
            //循环用缓冲区读输入流，直到输入流读干净
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                //循环用缓冲区写进输出流，直到缓冲区写干净。还有偏移量和长度信息。
                outputStream.write(buffer, 0, bytesRead);
            }
            response.flushBuffer();
        }
    }

    public void updateUser(int userId,String nickName,String password,Date birthday,String avatarPath,HttpServletRequest request) {
        userMapper.updateUser(userId, nickName, birthday, password, 0L, 0L,0L, 0L,avatarPath, 0, 0);
    }

    public void passwordCheck(HttpServletRequest request, HttpServletResponse response, String oldPassword, int userId) {
        User user = userMapper.selectUser(userId, null, 0);
        if (!user.getPassword().equals(oldPassword)) {
            //旧密码输入错误
            response.setStatus(250);
        }
    }

    public List<SessionUser> selectUsers(HttpServletRequest request, int userId) {
        List<SessionUser> sessionUsers = new ArrayList<>();
        User userSelf = userMapper.selectUser(userId, null, 0);
        SessionUser sessionUser = new SessionUser(userSelf.getUserId(), userSelf.getEmail(), userSelf.getNickName(), userSelf.getIsAdmin(), userSelf.getAvatarPath(), userSelf.getBirthday(), userSelf.getUseSpace(), userSelf.getTotalSpace(), userSelf.getBan());
        //本人第一优先级
        sessionUsers.add(sessionUser);
        //其他管理员们第二优先,除了本人
        List<SessionUser> sessionAdmins = userMapper.selectUsers(1,userId);
        sessionUsers.addAll(sessionAdmins);
        //其他人第三优先
        List<SessionUser> sessionOthers = userMapper.selectUsers(-1,userId);
        sessionUsers.addAll(sessionOthers);
        return sessionUsers;
    }

    public void adminUpdate(HttpServletRequest request,
                            HttpServletResponse response,
                            int userId,
                            float spaceVariation,
                            int ban,
                            int isAdmin) throws GeneralSecurityException {
        //预先检查用户是否为管理员
        String token = request.getHeader("token");
        //解密token
        token = AESUtil.AESDecode(token);
        SessionUser sessionUser = com.alibaba.fastjson.JSON.parseObject(RedisUtil.get(token), SessionUser.class);
        if (sessionUser.getIsAdmin() != 1) {
            //权限不足
            response.setStatus(250);
        } else {
            //执行修改
            Long spaceVariationBytes = (long) (spaceVariation * 1024 * 1024 * 1024);
            userMapper.updateUser(userId, null, null, null, 0L, 0L, 0L, spaceVariationBytes, null, ban, isAdmin);
        }
    }
}
