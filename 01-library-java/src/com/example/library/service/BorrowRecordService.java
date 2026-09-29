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

public class BorrowRecordService {
    private final BookDao bookDao;
    private final BorrowRecordDao borrowRecordDao;
    private final UserDao userDao;

    public BorrowRecordService(BookDao bookDao, BorrowRecordDao borrowRecordDao, UserDao userDao) {
        this.bookDao = bookDao;
        this.borrowRecordDao = borrowRecordDao;
        this.userDao = userDao;
    }

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

    public List<BorrowRecordView> findAll() {
        return borrowRecordDao.findAll().stream().map(this::toView).toList();
    }

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
