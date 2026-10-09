package com.example.library.db;

import com.example.library.exception.StorageException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 从环境变量读取连接配置，并创建独立连接，不缓存或共享 Connection。
 */
public final class ConnectionFactory {
    private final String url;
    private final String user;
    private final String password;

    /**
     * 读取并检查连接配置，此时尚未连接数据库。
     *
     * @throws StorageException 环境变量缺失、URL 或用户名为空白，
     *                          或 URL 不以 {@code jdbc:mysql://} 开头
     */
    public ConnectionFactory() {
        String configuredUrl = requireEnvironment("LIBRARY_DB_URL");
        String configuredUser = requireEnvironment("LIBRARY_DB_USER");
        String configuredPassword = requireEnvironment("LIBRARY_DB_PASSWORD");
        if (configuredUrl.isBlank()
                || !configuredUrl.strip().startsWith("jdbc:mysql://")) {
            throw new StorageException("LIBRARY_DB_URL 须为有效的 MySQL JDBC URL");
        }
        if (configuredUser.isBlank()) {
            throw new StorageException("LIBRARY_DB_USER 不能为空");
        }
        this.url = configuredUrl.strip();
        this.user = configuredUser.strip();
        this.password = configuredPassword;
    }

    /**
     * 创建新连接，由调用方负责关闭。
     *
     * @return 新的数据库连接
     * @throws SQLException 驱动无法创建连接
     */
    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new StorageException("缺少数据库环境变量：" + name);
        }
        return value;
    }
}
