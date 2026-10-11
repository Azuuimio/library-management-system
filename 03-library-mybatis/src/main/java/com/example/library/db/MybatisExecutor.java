package com.example.library.db;

import com.example.library.exception.BusinessException;
import com.example.library.exception.StorageException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

import java.util.function.Function;

/**
 * 为每次回调创建独立会话，统一处理事务提交、回滚与会话关闭。
 *
 * <p>使用 JDBC 事务配置，通过 SqlSession 控制事务，由 MyBatis 管理连接。
 * MyBatis 在关闭事务时捕获的 SQLException 仅记录日志，不向调用方抛出。
 * 不支持嵌套事务；回调内不能调用会自行开启事务的业务方法，也不能等待用户输入。
 */
public final class MybatisExecutor {
    private final SqlSessionFactory sqlSessionFactory;

    public MybatisExecutor(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 在自动提交会话中执行查询，结束时关闭会话。
     *
     * @param work 只执行查询的回调；会话与 Mapper 不能在回调结束后继续使用
     * @param <T> 查询结果类型
     * @return 已读取并映射完成的查询结果
     * @throws BusinessException 业务检查未通过
     * @throws StorageException 查询或会话处理失败
     */
    public <T> T executeQuery(Function<SqlSession, T> work) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.SIMPLE, true)) {
            return work.apply(session);
        } catch (PersistenceException exception) {
            throw new StorageException("数据库查询或会话处理失败", exception);
        }
    }


    /**
     * 在 READ COMMITTED 事务中执行业务，成功则提交，失败则尝试回滚。
     *
     * @param work 同一连接和会话内的业务回调，不得自行提交、回滚或关闭会话
     * @param <T> 回调结果类型
     * @return 事务提交后的业务结果
     * @throws BusinessException 业务检查未通过，且回滚成功
     * @throws StorageException 数据库操作、事务处理或会话处理失败
     */
    public <T> T executeInTransaction(Function<SqlSession, T> work) {
        try (SqlSession session = sqlSessionFactory.openSession(
                ExecutorType.SIMPLE, TransactionIsolationLevel.READ_COMMITTED)) {
            try {
                T result = work.apply(session);
                session.commit(true);
                return result;
            } catch (RuntimeException | Error exception) {
                try {
                    session.rollback(true);
                } catch (RuntimeException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                    throw new StorageException("事务回滚失败", exception);
                }
                throw exception;
            }
        } catch (PersistenceException exception) {
            throw new StorageException("数据库操作、事务或会话处理失败", exception);
        }
    }
}
