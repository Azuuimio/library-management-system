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
        return bookDao.insert(BookValidator.validateAndNormalizeText(title, "书名"),
                BookValidator.validateAndNormalizeText(author, "作者"),
                BookValidator.validateAndNormalizePrice(price),
                BookValidator.validateQuantity(totalQuantity));
    }

    public void delete(long bookId) {
        Book book = findNotDeletedById(bookId);
        if (borrowRecordDao.countUnreturnedByBookId(bookId) != 0) {
            throw new BusinessException("该图书仍有未归还记录，不能删除");
        }
        bookDao.update(new Book(book.id(), book.title(), book.author(), book.price(), book.totalQuantity(), true));
    }

    public void update(long bookId, String title, String author,
                       BigDecimal price, int totalQuantity) {
        Book current = findNotDeletedById(bookId);
        Book replacement = new Book(current.id(),
                BookValidator.validateAndNormalizeText(title, "书名"),
                BookValidator.validateAndNormalizeText(author, "作者"),
                BookValidator.validateAndNormalizePrice(price),
                BookValidator.validateQuantity(totalQuantity),
                false);
        if (replacement.totalQuantity() < borrowRecordDao.countUnreturnedByBookId(bookId)) {
            throw new BusinessException("总数量不能小于当前未归还数量");
        }
        bookDao.update(replacement);
    }

    public BookView findById(long bookId) {
        return toView(findNotDeletedById(bookId));
    }

    public List<BookView> findAllNotDeleted() {
        return bookDao.findAllNotDeleted().stream().map(this::toView).toList();
    }

    public List<BookView> searchNotDeletedByTitle(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException("搜索关键词不能为空");
        }
        return bookDao.searchNotDeletedByTitle(keyword.strip()).stream().map(this::toView).toList();
    }

    private Book findNotDeletedById(long bookId) {
        return bookDao.findById(bookId).filter(book -> !book.deleted())
                .orElseThrow(() -> new BusinessException("图书不存在或已删除"));
    }

    private BookView toView(Book book) {
        return new BookView(book, borrowRecordDao.countUnreturnedByBookId(book.id()));
    }
}
