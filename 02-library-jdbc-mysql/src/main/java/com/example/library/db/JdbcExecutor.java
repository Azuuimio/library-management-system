package com.example.library.db;

import com.example.library.dao.BookDao;
import com.example.library.dao.BorrowRecordDao;
import com.example.library.dao.UserDao;
import com.example.library.dao.jdbc.JdbcBookDao;
import com.example.library.dao.jdbc.JdbcBorrowRecordDao;
import com.example.library.dao.jdbc.JdbcUserDao;
import com.example.library.exception.BusinessException;
import com.example.library.exception.StorageException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * 为每次回调创建并关闭连接，统一处理事务提交、回滚和清理异常。
 *
 * <p>不支持嵌套事务；回调内不能调用会自行开启事务的业务方法。
 */
public final class JdbcExecutor {
    private final ConnectionFactory connectionFactory;

    /**
     * 同一连接上的 DAO，只在执行器回调内使用。
     *
     * @param userDao 用户查询接口
     * @param bookDao 图书查询和保存接口
     * @param borrowRecordDao 借阅记录查询和保存接口
     */
    public record DaoContext(UserDao userDao, BookDao bookDao, BorrowRecordDao borrowRecordDao) {
    }

    public JdbcExecutor(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * 使用新连接执行查询，返回前关闭连接。
     *
     * @param work 只执行查询的回调，须在返回前读完并映射结果
     * @param <T> 查询结果类型
     * @return 已脱离连接的查询结果
     * @throws BusinessException 业务检查未通过，且连接关闭正常
     * @throws StorageException 查询、连接创建或关闭失败
     */
    public <T> T executeQuery(Function<DaoContext, T> work) {
        try (Connection connection = connectionFactory.open()) {
            return work.apply(new DaoContext(
                    new JdbcUserDao(connection),
                    new JdbcBookDao(connection),
                    new JdbcBorrowRecordDao(connection)));
        } catch (BusinessException exception) {
            for (Throwable suppressed : exception.getSuppressed()) {
                if (suppressed instanceof SQLException) {
                    throw new StorageException("数据库连接关闭失败", exception);
                }
            }
            throw exception;
        } catch (SQLException exception) {
            throw new StorageException("数据库连接创建或关闭失败", exception);
        }
    }

    /**
     * 在 READ COMMITTED 事务中执行业务，成功则提交，失败则尝试回滚。
     *
     * @param work 使用同一连接的业务回调，不得等待用户输入
     * @param <T> 回调结果类型
     * @return 事务提交且连接关闭后的业务结果
     * @throws BusinessException 业务检查未通过，且回滚和关闭正常
     * @throws StorageException 数据库操作、事务提交或回滚失败，或数据库连接创建、配置、关闭失败
     */
    public <T> T executeInTransaction(Function<DaoContext, T> work) {
        try (Connection connection = connectionFactory.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                T result = work.apply(new DaoContext(
                        new JdbcUserDao(connection),
                        new JdbcBookDao(connection),
                        new JdbcBorrowRecordDao(connection)));
                connection.commit();
                return result;
            } catch (RuntimeException | SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                    throw new StorageException("事务回滚失败", exception);
                }
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new StorageException("事务提交异常", exception);
            }
        } catch (BusinessException exception) {
            for (Throwable suppressed : exception.getSuppressed()) {
                if (suppressed instanceof SQLException) {
                    throw new StorageException("数据库连接关闭失败", exception);
                }
            }
            throw exception;
        } catch (SQLException exception) {
            throw new StorageException("数据库连接创建、配置或关闭失败", exception);
        }
    }
}
