package com.example.library.dao;

import com.example.library.model.Book;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BookDao {
    Optional<Book> findById(long id);

    List<Book> findAllNotDeleted();

    List<Book> findNotDeletedByTitle(String keyword);

    Book insert(String title, String author, BigDecimal price, int totalQuantity);

    void update(Book book);
}
