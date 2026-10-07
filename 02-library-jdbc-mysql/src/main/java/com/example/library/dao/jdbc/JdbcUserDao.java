package com.example.library.dao.jdbc;

import com.example.library.dao.UserDao;
import com.example.library.exception.StorageException;
import com.example.library.model.Role;
import com.example.library.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * 通过当前连接查询预制账号，连接由执行器管理。
 */
public final class JdbcUserDao implements UserDao {
    private final Connection connection;

    public JdbcUserDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT id, username, role
                FROM users
                WHERE username = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result = result.next() ? Optional.of(mapUser(result)) : Optional.empty()
            }
        } catch (SQLException exception) {
            throw new StorageException("查询用户失败", exception);
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(result.getLong("id"),
                result.getString("username"),
                Role.valueOf(result.getString("role")));
    }
}
