package com.example.library.service;

import com.example.library.dao.BookDao;
import com.example.library.dao.BorrowRecordDao;
import com.example.library.exception.BusinessException;
import com.example.library.model.Book;
import com.example.library.model.view.BookView;
import com.example.library.validation.BookValidator;

import java.math.BigDecimal;
import java.util.List;

public final class BookService {
    private final BookDao bookDao;
    private final BorrowRecordDao borrowRecordDao;

    public BookService(BookDao bookDao, BorrowRecordDao borrowRecordDao) {
        this.bookDao = bookDao;
        this.borrowRecordDao = borrowRecordDao;
    }

    public Book add(String title, String author, BigDecimal price, int totalQuantity) {
        return bookDao.insert(BookValidator.text(title, "书名"),
                BookValidator.text(author, "作者"),
                BookValidator.price(price),
                BookValidator.quantity(totalQuantity));
    }

    public void delete(long bookId) {
        Book book = requireNotDeleted(bookId);
        if (borrowRecordDao.countUnreturnedByBookId(bookId) != 0) {
            throw new BusinessException("该图书仍有未归还记录，不能删除");
        }
        bookDao.update(new Book(book.id(), book.title(), book.author(), book.price(), book.totalQuantity(), true));
    }

    public void update(long bookId, String title, String author,
                       BigDecimal price, int totalQuantity) {
        Book current = requireNotDeleted(bookId);
        Book replacement = new Book(current.id(), BookValidator.text(title, "书名"),
                BookValidator.text(author, "作者"), BookValidator.price(price), BookValidator.quantity(totalQuantity), false);
        if (replacement.totalQuantity() < borrowRecordDao.countUnreturnedByBookId(bookId)) {
            throw new BusinessException("总数量不能小于当前未归还数量");
        }
        bookDao.update(replacement);
    }

    public BookView get(long bookId) {
        return toView(requireNotDeleted(bookId));
    }

    public List<BookView> search(String keyword) {
        if (keyword == null) {
            throw new BusinessException("查询关键词不能为 null");
        }
        return bookDao.findNotDeletedByTitle(keyword.strip()).stream().map(this::toView).toList();
    }

    public List<BookView> findAll() {
        return bookDao.findAllNotDeleted().stream().map(this::toView).toList();
    }

    private Book requireNotDeleted(Long bookId) {
        return bookDao.findById(bookId).filter(book -> !book.deleted())
                .orElseThrow(() -> new BusinessException("图书不存在或已删除"));
    }

    private BookView toView(Book book) {
        return new BookView(book, borrowRecordDao.countUnreturnedByBookId(book.id()));
    }
}
