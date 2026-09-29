package com.example.library.dao.csv;

import com.example.library.dao.BookDao;
import com.example.library.exception.StorageException;
import com.example.library.model.Book;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class CsvBookDao implements BookDao {
    static final List<String> HEADER = List.of("id", "title", "author", "price", "totalQuantity", "deleted");
    private final Path file;
    NavigableMap<Long, Book> books = new TreeMap<>();

    public CsvBookDao(Path directory) {
        file = directory.toAbsolutePath().normalize().resolve("books.csv");
        List<List<String>> rows = CsvFileIO.read(file, HEADER);
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                addLoadedBook(new Book(Long.parseLong(row.get(0)),
                        row.get(1),
                        row.get(2),
                        new BigDecimal(row.get(3)),
                        Integer.parseInt(row.get(4)),
                        parseDeletedFlag(row.get(5))));
            } catch (RuntimeException exception) {
                throw CsvFileIO.invalidRow(file, i, exception);
            }
        }
    }

    @Override
    public Optional<Book> findById(long id) {
        return Optional.ofNullable(books.get(id));
    }

    @Override
    public List<Book> findAllNotDeleted() {
        return books.values().stream().filter(book -> !book.deleted()).toList();
    }

    @Override
    public List<Book> searchNotDeletedByTitle(String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return books.values().stream()
                .filter(book -> !book.deleted())
                .filter(book -> book.title().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    @Override
    public Book insert(String title, String author, BigDecimal price, int totalQuantity) {
        Book book = new Book(nextId(), title, author, price, totalQuantity, false);
        save(book);
        return book;
    }

    @Override
    public void update(Book book) {
        if (!books.containsKey(book.id())) {
            throw new StorageException("要更新的图书不存在");
        }
        save(book);
    }

    private void addLoadedBook(Book value) {
        if (value.id() <= 0 || books.putIfAbsent(value.id(), value) != null) {
            throw new IllegalArgumentException("编号必须为不重复的正整数");
        }
    }

    private boolean parseDeletedFlag(String text) {
        if (!text.equals("true") && !text.equals("false")) {
            throw new IllegalArgumentException("deleted 只能是 true 或 false");
        }
        return Boolean.parseBoolean(text);
    }

    private long nextId() {
        try {
            return books.isEmpty() ? 1 : Math.addExact(books.lastKey(), 1);
        } catch (ArithmeticException exception) {
            throw new StorageException("编号已达到 long 类型上限", exception);
        }
    }

    private void save(Book book) {
        NavigableMap<Long, Book> copy = new TreeMap<>(books);
        copy.put(book.id(), book);
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADER);
        for (Book value : copy.values()) {
            rows.add(List.of(Long.toString(value.id()),
                    value.title(),
                    value.author(),
                    value.price().toPlainString(),
                    Integer.toString(value.totalQuantity()),
                    Boolean.toString(value.deleted())));
        }
        CsvFileIO.write(file, rows);
        books = copy;
    }
}
