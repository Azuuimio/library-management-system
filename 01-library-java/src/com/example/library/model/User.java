package com.example.library.model;

/**
 * 预制账号的用户信息，供登录和菜单选择使用。
 *
 * <p>本阶段使用无密码账号，此记录的构造器不校验字段。
 *
 * @param id 用户编号
 * @param username 登录时使用的账号名称
 * @param role 用户角色，决定登录后显示的菜单
 */
public record User(long id, String username, Role role) {
}
