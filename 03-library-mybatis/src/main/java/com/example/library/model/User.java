package com.example.library.model;

/**
 * 预制账号的用户信息，供登录和菜单选择使用。
 *
 * <p>本阶段使用无密码账号，构造器和 setter 不校验字段。
 */
public final class User {
    /** 用户编号。 */
    private long id;

    /** 预制账号名称。 */
    private String username;

    /** 用户角色。 */
    private Role role;

    public User() {
    }

    public User(long id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
