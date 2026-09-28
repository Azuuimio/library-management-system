package com.example.library.dao;

import com.example.library.model.BorrowRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BorrowRecordDao {
    Optional<BorrowRecord> findById(long id);

    List<BorrowRecord> findAll();

    List<BorrowRecord> findByUserId(long userId);

    long countUnreturnedByBookId(long bookId);

    boolean existsUnreturned(long userId, long bookId);

    BorrowRecord insert(long userId, long bookId, LocalDateTime borrowedAt);

    void update(BorrowRecord record);
}
