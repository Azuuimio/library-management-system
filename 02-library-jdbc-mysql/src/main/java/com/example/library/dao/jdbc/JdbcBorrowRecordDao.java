package com.example.library.dao.jdbc;

import com.example.library.dao.BorrowRecordDao;
import com.example.library.exception.StorageException;
import com.example.library.model.BorrowRecord;
import com.example.library.model.view.BorrowRecordView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 通过当前连接保存借阅记录，使用联表查询生成展示信息，连接由执行器管理。
 */
public class JdbcBorrowRecordDao implements BorrowRecordDao {
    private final Connection connection;

    public JdbcBorrowRecordDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<BorrowRecord> findById(long id) {
        String sql = """
                SELECT id AS record_id, user_id, book_id, borrowed_at, returned_at
                FROM borrow_records WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapRecord(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("查询借阅记录失败", exception);
        }
    }

    @Override
    public List<BorrowRecordView> findAllViews() {
        String sql = """
                SELECT r.id AS record_id,
                       r.user_id,
                       r.book_id,
                       r.borrowed_at,
                       r.returned_at,
                       u.username,
                       b.title AS book_title,
                       b.deleted AS book_deleted
                FROM borrow_records AS r
                JOIN users AS u ON u.id = r.user_id
                JOIN books AS b ON b.id = r.book_id
                ORDER BY r.id ASC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<BorrowRecordView> views = new ArrayList<>();
            while (result.next()) {
                views.add(new BorrowRecordView(
                        mapRecord(result),
                        result.getString("username"),
                        result.getString("book_title"),
                        result.getBoolean("book_deleted")));
            }
            return List.copyOf(views);
        } catch (SQLException exception) {
            throw new StorageException("查询借阅展示信息失败", exception);
        }
    }

    @Override
    public List<BorrowRecordView> findViewsByUserId(long userId) {
        String sql = """
                SELECT r.id AS record_id,
                       r.user_id,
                       r.book_id,
                       r.borrowed_at,
                       r.returned_at,
                       u.username,
                       b.title AS book_title,
                       b.deleted AS book_deleted
                FROM borrow_records AS r
                JOIN users AS u ON u.id = r.user_id
                JOIN books AS b ON b.id = r.book_id
                WHERE r.user_id = ?
                ORDER BY r.id ASC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<BorrowRecordView> views = new ArrayList<>();
                while (result.next()) {
                    views.add(new BorrowRecordView(
                            mapRecord(result),
                            result.getString("username"),
                            result.getString("book_title"),
                            result.getBoolean("book_deleted")));
                }
                return List.copyOf(views);
            }
        } catch (SQLException exception) {
            throw new StorageException("查询借阅展示信息失败", exception);
        }
    }

    @Override
    public long countUnreturnedByBookId(long bookId) {
        String sql = """
                SELECT COUNT(*)
                FROM borrow_records
                WHERE book_id = ? AND returned_at IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new StorageException("未取得未归还数量的统计结果");
                }
                return result.getLong(1);
            }
        } catch (SQLException exception) {
            throw new StorageException("统计未归还数量失败", exception);
        }
    }

    @Override
    public boolean existsUnreturned(long userId, long bookId) {
        String sql = """
                SELECT 1
                FROM borrow_records
                WHERE user_id = ? AND book_id = ? AND returned_at IS NULL
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, bookId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw new StorageException("查询重复借阅状态失败", exception);
        }
    }

    @Override
    public BorrowRecord insert(long userId, long bookId, LocalDateTime borrowedAt) {
        String sql = """
                INSERT INTO borrow_records (user_id, book_id, borrowed_at)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setLong(2, bookId);
            statement.setObject(3, borrowedAt);
            if (statement.executeUpdate() != 1) {
                throw new StorageException("新增借阅记录的受影响行数异常");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new StorageException("新增借阅记录后未取得数据库生成的编号");
                }
                return new BorrowRecord(keys.getLong(1), userId, bookId, borrowedAt, null);
            }
        } catch (SQLException exception) {
            throw new StorageException("保存新借阅记录失败", exception);
        }
    }

    @Override
    public int markReturnedIfUnreturned(long id, long userId, LocalDateTime returnedAt) {
        String sql = """
                UPDATE borrow_records
                SET returned_at = ?
                WHERE id = ? AND user_id = ? AND returned_at IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, returnedAt);
            statement.setLong(2, id);
            statement.setLong(3, userId);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("保存归还时间失败", exception);
        }
    }

    private BorrowRecord mapRecord(ResultSet result) throws SQLException {
        return new BorrowRecord(
                result.getLong("record_id"),
                result.getLong("user_id"),
                result.getLong("book_id"),
                result.getObject("borrowed_at", LocalDateTime.class),
                result.getObject("returned_at", LocalDateTime.class));
    }
}
