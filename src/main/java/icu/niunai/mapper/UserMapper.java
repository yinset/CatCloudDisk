package icu.niunai.mapper;

import icu.niunai.entity.dto.SessionUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import icu.niunai.entity.po.User;

import java.util.Date;
import java.util.List;

@Mapper
public interface UserMapper {
    User selectUser(@Param("userId") int userId,
                    @Param("email") String email,
                    @Param("isAdmin") int isAdmin);

    List<SessionUser> selectUsers(@Param("isAdmin") int isAdmin,
                                  @Param("userId") int userId);


    void insertUser(@Param("email") String email,
                    @Param("password") String password,
                    @Param("nickName") String nickName,
                    @Param("birthday") Date birthday,
                    @Param("createTime") Date createTime);

    /**
     * @param userId 用户Id
     * @param nickName 用户昵称
     * @param birthday 生日
     * @param password 密码
     * @param useSpace         当useSpace与useSpaceChange同在时，useSpace无效
     * @param useSpaceChange 容量改变量
     * @param totalSpace       同上
     * @param totalSpaceChange 总容量改变量
     * @param avatarPath 头像路径
     * @param ban 禁用 1是 -1否 0忽略参数
     * @param isAdmin 管理员 1是 -1否 0忽略参数
     */
    void updateUser(@Param("userId") int userId,
                    @Param("nickName") String nickName,
                    @Param("birthday") Date birthday,
                    @Param("password") String password,
                    @Param("useSpace") Long useSpace,
                    @Param("useSpaceChange") Long useSpaceChange,
                    @Param("totalSpace") Long totalSpace,
                    @Param("totalSpaceChange") Long totalSpaceChange,
                    @Param("avatarPath") String avatarPath,
                    @Param("ban") int ban,
                    @Param("isAdmin") int isAdmin);
}
