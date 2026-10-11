package com.example.library.mapper;

import com.example.library.model.User;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 预制账号的查询接口，供登录使用。
 */
public interface UserMapper {
    /**
     * 根据账号名称查询用户，匹配时区分大小写。
     *
     * @param username 已由调用方去除首尾空白的账号名称
     * @return 找到时返回包含该用户的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询用户失败
     */
    Optional<User> findByUsername(@Param("username") String username);
}
