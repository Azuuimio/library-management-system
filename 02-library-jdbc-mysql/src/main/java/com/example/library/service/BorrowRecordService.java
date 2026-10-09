package com.example.library.service;

import com.example.library.dao.BorrowRecordDao;
import com.example.library.db.JdbcExecutor;
import com.example.library.exception.BusinessException;
import com.example.library.exception.StorageException;
import com.example.library.model.Book;
import com.example.library.model.BorrowRecord;
import com.example.library.model.User;
import com.example.library.model.view.BorrowRecordView;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 处理借书、还书和借阅记录查询。
 *
 * <p>借还书通过新增或更新借阅记录反映，不修改图书总数量。
 * 调用方负责将借还操作限定为已登录读者，此类不检查用户角色。
 */
public final class BorrowRecordService {
    private final JdbcExecutor executor;

    public BorrowRecordService(JdbcExecutor executor) {
        this.executor = executor;
    }

    /**
     * 为当前读者借出一本指定图书，并保存未归还的借阅记录。
     *
     * @param currentUser 已登录的读者，由调用方保证不为 {@code null} 且角色为读者
     * @param bookId 要借阅的图书编号
     * @return 新建的借阅记录，借阅时间取当前本地时间并截去不足一秒的部分
     * @throws BusinessException 图书不存在或已删除、该读者仍有此书未归还，或没有可借数量
     * @throws StorageException 编号无法分配、数据查询或保存借阅记录失败，或数据库连接、事务处理失败
     */
    public BorrowRecord borrowBook(User currentUser, long bookId) {
        return executor.executeInTransaction(context -> {
            Book book = context.bookDao().findByIdForUpdate(bookId).filter(value -> !value.deleted())
                    .orElseThrow(() -> new BusinessException("图书不存在或已删除"));
            if (context.borrowRecordDao().existsUnreturned(currentUser.id(), bookId)) {
                throw new BusinessException("你已借阅此书，请先归还再借阅");
            }
            if (context.borrowRecordDao().countUnreturnedByBookId(bookId) >= book.totalQuantity()) {
                throw new BusinessException("库存不足，暂时无法借阅");
            }
            return context.borrowRecordDao().insert(currentUser.id(), bookId, now());
        });
    }

    /**
     * 归还当前读者的一条借阅记录所对应的图书，并填写归还时间。
     *
     * <p>归还时间取当前本地时间并截去不足一秒的部分。
     * 若该时间早于借阅时间，则拒绝归还，要求用户校准系统时钟。
     *
     * @param currentUser 已登录的读者，由调用方保证不为 {@code null} 且角色为读者
     * @param recordId 借阅记录编号
     * @throws BusinessException 记录不存在、不属于当前读者、已经归还，或当前时间早于借阅时间
     * @throws StorageException 数据查询或保存归还时间失败，或数据库连接、事务处理失败
     */
    public void returnBook(User currentUser, long recordId) {
        executor.executeInTransaction(context -> {
            // 先查询借阅记录，确定需要锁定的图书。
            BorrowRecord first = findOwnedById(context.borrowRecordDao(), recordId, currentUser.id());
            context.bookDao().findByIdForUpdate(first.bookId())
                    .orElseThrow(() -> new StorageException("借阅记录关联的图书不存在"));
            // 等待图书锁期间归还状态可能变化，取得锁后重新查询。
            BorrowRecord current = findOwnedById(context.borrowRecordDao(), recordId, currentUser.id());
            if (current.isReturned()) {
                throw new BusinessException("这条借阅记录已经归还，不能重复归还");
            }
            LocalDateTime returnedAt = now();
            if (returnedAt.isBefore(current.borrowedAt())) {
                throw new BusinessException("当前系统时间早于借阅时间，请校准时钟后重试");
            }
            if (context.borrowRecordDao().markReturnedIfUnreturned(recordId, currentUser.id(), returnedAt) != 1) {
                throw new StorageException("归还图书的受影响行数异常");
            }
            return null;
        });
    }

    /**
     * 查询全部借阅记录，并补充当前账号名称、书名和图书删除标记。
     *
     * @return 包括已归还和未归还记录的展示信息；没有记录时返回空列表
     * @throws StorageException 查询借阅展示信息或数据库连接处理失败
     */
    public List<BorrowRecordView> findAll() {
        return executor.executeQuery(context -> context.borrowRecordDao().findAllViews());
    }

    /**
     * 查询当前用户的全部借阅记录，并补充当前账号名称、书名和图书删除标记。
     *
     * @param currentUser 已登录的用户，由调用方保证不为 {@code null}
     * @return 该用户已归还和未归还记录的展示信息；没有记录时返回空列表
     * @throws StorageException 查询借阅展示信息或数据库连接处理失败
     */
    public List<BorrowRecordView> findByUser(User currentUser) {
        return executor.executeQuery(context -> context.borrowRecordDao().findViewsByUserId(currentUser.id()));
    }

    private BorrowRecord findOwnedById(BorrowRecordDao borrowRecordDao, long recordId, long userId) {
        return borrowRecordDao.findById(recordId)
                .filter(borrowRecord -> borrowRecord.userId() == userId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在或不属于当前读者"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
