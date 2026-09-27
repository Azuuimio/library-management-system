package com.example.library.dao;

import com.example.library.model.Book;
import com.example.library.model.Borrow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BorrowDao {
    Optional<Borrow> findById(long id);

    List<Borrow> findAll();

    List<Borrow> findByUserId(long userId);

    long countActiveByBookId(long bookId);

    boolean existsActive(long userId, long bookId);

    Borrow insert(long userId, long bookId, LocalDateTime borrowedAt);

    void update(Borrow record);
}
