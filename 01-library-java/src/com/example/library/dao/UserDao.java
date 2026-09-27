package com.example.library.dao;

import com.example.library.model.User;

import java.util.Optional;

public interface UserDao {
    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);
}