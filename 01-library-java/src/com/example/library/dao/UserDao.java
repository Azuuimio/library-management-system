package com.example.library.dao;

import com.example.library.model.User;

import java.util.Optional;

/**
 * 预制账号的查询接口，供登录和借阅记录展示使用。
 */
public interface UserDao {
    /**
     * 根据编号查询用户。
     *
     * @param id 用户编号
     * @return 找到时返回包含该用户的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     */
    Optional<User> findById(long id);

    /**
     * 根据账号名称查询用户，匹配时区分大小写。
     *
     * @param username 已由调用方去除首尾空白的账号名称
     * @return 找到时返回包含该用户的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     */
    Optional<User> findByUsername(String username);
}
