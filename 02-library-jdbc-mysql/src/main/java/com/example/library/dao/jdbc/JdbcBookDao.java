package com.example.library.dao.jdbc;

import com.example.library.dao.BookDao;
import com.example.library.exception.StorageException;
import com.example.library.model.Book;
import com.example.library.model.view.BookView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 通过当前连接保存图书，使用聚合查询生成展示信息，连接由执行器管理。
 */
public final class JdbcBookDao implements BookDao {
    private final Connection connection;

    public JdbcBookDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Book> findByIdForUpdate(long id) {
        String sql = """
                SELECT id, title, author, price, total_quantity, deleted, version
                FROM books
                WHERE id = ?
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapBook(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("锁定并查询图书失败", exception);
        }
    }

    @Override
    public Optional<BookView> findNotDeletedViewById(long id) {
        String sql = """
                SELECT b.id,
                       b.title,
                       b.author,
                       b.price,
                       b.total_quantity,
                       b.deleted,
                       b.version,
                       COUNT(r.id) AS unreturned_quantity
                FROM books AS b
                LEFT JOIN borrow_records AS r
                    ON b.id = r.book_id AND r.returned_at IS NULL
                WHERE b.deleted = 0 AND b.id = ?
                GROUP BY b.id,
                         b.title,
                         b.author,
                         b.price,
                         b.total_quantity,
                         b.deleted,
                         b.version
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapView(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("查询图书展示信息失败", exception);
        }
    }

    @Override
    public List<BookView> findAllNotDeletedViews() {
        String sql = """
                SELECT b.id,
                       b.title,
                       b.author,
                       b.price,
                       b.total_quantity,
                       b.deleted,
                       b.version,
                       COUNT(r.id) AS unreturned_quantity
                FROM books AS b
                LEFT JOIN borrow_records AS r
                    ON b.id = r.book_id AND r.returned_at IS NULL
                WHERE b.deleted = 0
                GROUP BY b.id,
                         b.title,
                         b.author,
                         b.price,
                         b.total_quantity,
                         b.deleted,
                         b.version
                ORDER BY b.id ASC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<BookView> views = new ArrayList<>();
            while (result.next()) {
                views.add(mapView(result));
            }
            return List.copyOf(views);
        } catch (SQLException exception) {
            throw new StorageException("查询图书展示信息列表失败", exception);
        }
    }

    @Override
    public List<BookView> searchNotDeletedViewsByTitle(String keyword) {
        String escapedKeyword = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        String sql = """
                SELECT b.id,
                       b.title,
                       b.author,
                       b.price,
                       b.total_quantity,
                       b.deleted,
                       b.version,
                       COUNT(r.id) AS unreturned_quantity
                FROM books AS b
                LEFT JOIN borrow_records AS r
                    ON b.id = r.book_id AND r.returned_at IS NULL
                WHERE b.deleted = 0
                  AND LOWER(b.title) LIKE LOWER(?) ESCAPE '!'
                GROUP BY b.id,
                         b.title,
                         b.author,
                         b.price,
                         b.total_quantity,
                         b.deleted,
                         b.version
                ORDER BY b.id ASC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + escapedKeyword + "%");
            try (ResultSet result = statement.executeQuery()) {
                List<BookView> views = new ArrayList<>();
                while (result.next()) {
                    views.add(mapView(result));
                }
                return List.copyOf(views);
            }
        } catch (SQLException exception) {
            throw new StorageException("按书名搜索图书失败", exception);
        }
    }

    @Override
    public Book insert(String title, String author, BigDecimal price, int totalQuantity) {
        String sql = """
                INSERT INTO books (title, author, price, total_quantity)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, author);
            statement.setBigDecimal(3, price);
            statement.setInt(4, totalQuantity);
            if (statement.executeUpdate() != 1) {
                throw new StorageException("新增图书的受影响行数异常");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new StorageException("新增图书后未取得数据库生成的编号");
                }
                return new Book(keys.getLong(1), title, author, price, totalQuantity, false, 0);
            }
        } catch (SQLException exception) {
            throw new StorageException("保存新图书失败", exception);
        }
    }

    @Override
    public int update(long id, String title, String author, BigDecimal price, int totalQuantity, long expectedVersion) {
        String sql = """
                UPDATE books
                SET title = ?, author = ?, price = ?, total_quantity = ?, version = version + 1
                WHERE id = ? AND deleted = 0 AND version = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, author);
            statement.setBigDecimal(3, price);
            statement.setInt(4, totalQuantity);
            statement.setLong(5, id);
            statement.setLong(6, expectedVersion);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("保存图书修改失败", exception);
        }
    }

    @Override
    public int markDeleted(long id, long expectedVersion) {
        String sql = """
                UPDATE books
                SET deleted = 1, version = version + 1
                WHERE id = ? AND deleted = 0 AND version = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, expectedVersion);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("保存图书删除状态失败", exception);
        }
    }

    private Book mapBook(ResultSet result) throws SQLException {
        return new Book(
                result.getLong("id"),
                result.getString("title"),
                result.getString("author"),
                result.getBigDecimal("price"),
                result.getInt("total_quantity"),
                result.getBoolean("deleted"),
                result.getLong("version"));
    }

    private BookView mapView(ResultSet result) throws SQLException {
        return new BookView(mapBook(result),
                result.getLong("unreturned_quantity"));
    }
}
