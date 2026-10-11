package com.example.library.db;

import com.example.library.exception.StorageException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 从环境变量读取连接配置，加载 MyBatis 配置和 Mapper XML。
 */
public final class MybatisSessionFactory {
    private MybatisSessionFactory() {
    }

    /**
     * 创建可共享的会话工厂，此时尚未连接数据库。
     *
     * @return 已加载全部 Mapper 的会话工厂
     * @throws StorageException 连接配置缺失或无效，或 MyBatis 配置加载失败
     */
    public static SqlSessionFactory create() {
        String url = requireEnvironment("LIBRARY_DB_URL");
        String user = requireEnvironment("LIBRARY_DB_USER");
        String password = requireEnvironment("LIBRARY_DB_PASSWORD");
        if (url.isBlank() || !url.strip().startsWith("jdbc:mysql://")) {
            throw new StorageException("LIBRARY_DB_URL 须为有效的 MySQL JDBC URL");
        }
        if (user.isBlank()) {
            throw new StorageException("LIBRARY_DB_USER 不能为空");
        }
        Properties properties = new Properties();
        properties.setProperty("db.url", url.strip());
        properties.setProperty("db.user", user.strip());
        properties.setProperty("db.password", password);
        try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
            return new SqlSessionFactoryBuilder().build(inputStream, properties);
        } catch (IOException | PersistenceException exception) {
            throw new StorageException("MyBatis 配置加载失败", exception);
        }
    }


    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new StorageException("缺少数据库环境变量：" + name);
        }
        return value;
    }
}
