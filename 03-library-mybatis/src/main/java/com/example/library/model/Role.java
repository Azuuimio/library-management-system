package com.example.library.model;

/**
 * 用户角色，用于区分管理员菜单和读者菜单。
 */
public enum Role {
    /** 管理图书并查看全部借阅记录的管理员。 */
    ADMIN,
    /** 查询图书、借还图书并查看本人借阅记录的读者。 */
    READER
}
