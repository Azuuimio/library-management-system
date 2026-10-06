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

/**
 * 使用 books.csv 保存图书数据。
 *
 * <p>创建对象时读取文件，之后的查询使用内存中的数据。
 * 新增或修改图书时，将全部图书数据写回文件；写入成功后，再更新内存中的数据。
 * 列表查询按图书编号升序返回不可修改的列表。
 *
 * <p>已删除的图书仍保留在文件中，用于查询历史借阅记录。
 */
public final class CsvBookDao implements BookDao {
    static final List<String> HEADER = List.of("id", "title", "author", "price", "totalQuantity", "deleted");
    private final Path file;
    NavigableMap<Long, Book> books = new TreeMap<>();

    /**
     * 从指定目录读取 books.csv，按编号保存到内存中。
     *
     * @param directory 数据目录，其中的 books.csv 必须已存在
     * @throws StorageException 读取失败、CSV 格式或字段转换失败、编号不是正数或重复，
     *                          或删除标记不是小写的 {@code true} 或 {@code false}
     */
    public CsvBookDao(Path directory) {
        file = directory.toAbsolutePath().normalize().resolve("books.csv");
        List<List<String>> rows = CsvFileIo.read(file, HEADER);
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
                throw CsvFileIo.invalidRowException(file, i, exception);
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

    /**
     * 取已有最大编号加一；没有图书时从 1 开始，已删除图书也参与计算。
     *
     * @return 新图书编号
     * @throws StorageException 最大编号已达到 {@link Long#MAX_VALUE}
     */
    private long nextId() {
        try {
            return books.isEmpty() ? 1 : Math.addExact(books.lastKey(), 1);
        } catch (ArithmeticException exception) {
            throw new StorageException("编号已达到 long 类型上限", exception);
        }
    }

    /**
     * 将图书加入数据副本，并用副本中的全部图书重写 CSV 文件。
     *
     * <p>文件写入成功后，才用副本替换当前内存数据。
     * 如果写入抛出异常，当前内存数据保持不变。
     *
     * @param book 要保存的图书；编号已存在时替换原图书
     * @throws StorageException 写入 CSV 文件失败
     */
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
        CsvFileIo.write(file, rows);
        books = copy;
    }
}
