package com.example.library.service;

import com.example.library.dao.BookDao;
import com.example.library.dao.BorrowRecordDao;
import com.example.library.dao.UserDao;
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
public class BorrowRecordService {
    private final BookDao bookDao;
    private final BorrowRecordDao borrowRecordDao;
    private final UserDao userDao;

    public BorrowRecordService(BookDao bookDao, BorrowRecordDao borrowRecordDao, UserDao userDao) {
        this.bookDao = bookDao;
        this.borrowRecordDao = borrowRecordDao;
        this.userDao = userDao;
    }

    /**
     * 为当前读者借出一本指定图书，并保存未归还的借阅记录。
     *
     * @param currentUser 已登录的读者，由调用方保证不为 {@code null} 且角色为读者
     * @param bookId 要借阅的图书编号
     * @return 新建的借阅记录，借阅时间取当前本地时间并截去不足一秒的部分
     * @throws BusinessException 图书不存在或已删除、该读者仍有此书未归还，或没有可借数量
     * @throws StorageException 编号无法分配或保存借阅记录失败
     */
    public BorrowRecord borrowBook(User currentUser, long bookId) {
        Book book = bookDao.findById(bookId).filter(value -> !value.deleted())
                .orElseThrow(() -> new BusinessException("图书不存在或已删除"));
        if (borrowRecordDao.existsUnreturned(currentUser.id(), bookId)) {
            throw new BusinessException("你已借阅此书，请先归还再借阅");
        }
        if (borrowRecordDao.countUnreturnedByBookId(bookId) >= book.totalQuantity()) {
            throw new BusinessException("库存不足，暂时无法借阅");
        }
        return borrowRecordDao.insert(currentUser.id(), bookId, now());
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
     * @throws StorageException 保存归还时间失败
     */
    public void returnBook(User currentUser, long recordId) {
        BorrowRecord record = findOwnedById(recordId, currentUser.id());
        if (record.isReturned()) {
            throw new BusinessException("这条借阅记录已经归还，不能重复归还");
        }
        LocalDateTime returnedAt = now();
        if (returnedAt.isBefore(record.borrowedAt())) {
            throw new BusinessException("当前系统时间早于借阅时间，请校准时钟后重试");
        }
        borrowRecordDao.update(new BorrowRecord(record.id(), record.userId(), record.bookId(),
                record.borrowedAt(), returnedAt));
    }

    /**
     * 查询全部借阅记录，并补充当前用户名、书名和图书删除标记。
     *
     * @return 包括已归还和未归还记录的展示信息；没有记录时返回空列表
     * @throws StorageException 某条记录关联的用户或图书不存在
     */
    public List<BorrowRecordView> findAll() {
        return borrowRecordDao.findAll().stream().map(this::toView).toList();
    }

    /**
     * 查询当前用户的全部借阅记录，并补充当前用户名、书名和图书删除标记。
     *
     * @param currentUser 已登录的用户，由调用方保证不为 {@code null}
     * @return 该用户已归还和未归还记录的展示信息；没有记录时返回空列表
     * @throws StorageException 某条记录关联的用户或图书不存在
     */
    public List<BorrowRecordView> findByUser(User currentUser) {
        return borrowRecordDao.findByUserId(currentUser.id()).stream().map(this::toView).toList();
    }

    private BorrowRecord findOwnedById(long recordId, long userId) {
        return borrowRecordDao.findById(recordId)
                .filter(borrowRecord -> borrowRecord.userId() == userId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在或不属于当前读者"));
    }

    private BorrowRecordView toView(BorrowRecord record) {
        User reader = userDao.findById(record.userId())
                .orElseThrow(() -> new StorageException("借阅记录关联的读者不存在"));
        Book book = bookDao.findById(record.bookId())
                .orElseThrow(() -> new StorageException("借阅记录关联的图书不存在"));
        return new BorrowRecordView(record, reader.username(), book.title(), book.deleted());
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
