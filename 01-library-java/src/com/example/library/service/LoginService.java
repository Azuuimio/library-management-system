package com.example.library.service;

import com.example.library.dao.UserDao;
import com.example.library.exception.BusinessException;
import com.example.library.model.User;

public final class LoginService {
    private final UserDao userDao;

    public LoginService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User login(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("账号不能为空");
        }
        return userDao.findByUsername(username.strip())
                .orElseThrow(() -> new BusinessException("账号不存在，请输入预制账号"));
    }
}
