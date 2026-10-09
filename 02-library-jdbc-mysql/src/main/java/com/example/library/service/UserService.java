package com.example.library.service;

import com.example.library.db.JdbcExecutor;
import com.example.library.exception.BusinessException;
import com.example.library.model.User;

/**
 * 根据预制账号名称完成登录，本阶段不校验密码。
 */
public final class UserService {
    private final JdbcExecutor executor;

    public UserService(JdbcExecutor executor) {
        this.executor = executor;
    }

    /**
     * 去除账号名称首尾空白，并查找对应的预制用户。
     *
     * @param username 用户输入的账号名称，匹配时区分大小写
     * @return 登录成功的用户，包含编号和角色
     * @throws BusinessException 账号为 {@code null}、空字符串或仅包含空白字符，或对应用户不存在
     */
    public User login(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("账号不能为空");
        }
        return executor.executeQuery(context -> context.userDao().findByUsername(username.strip())
                        .orElseThrow(() -> new BusinessException("账号不存在，请输入预制账号")));
    }
}
