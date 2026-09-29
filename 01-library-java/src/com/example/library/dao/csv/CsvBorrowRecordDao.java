package com.example.library.dao.csv;

import com.example.library.dao.BorrowRecordDao;
import com.example.library.exception.StorageException;
import com.example.library.model.BorrowRecord;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 使用 borrow_records.csv 保存借阅记录。
 *
 * <p>创建对象时读取文件，之后的查询使用内存中的数据。
 * 每次新增或更新都重写全部记录，写入成功后再更新内存数据。
 * 列表查询按记录编号升序返回不可修改的列表。
 *
 * <p>文件中的空归还时间对应 {@code null}，表示尚未归还。
 */
public final class CsvBorrowRecordDao implements BorrowRecordDao {
    static final List<String> HEADER = List.of("id", "userId", "bookId", "borrowedAt", "returnedAt");
    private final Path file;
    NavigableMap<Long, BorrowRecord> records = new TreeMap<>();

    /**
     * 从指定目录读取 borrow_records.csv，按编号保存到内存中。
     *
     * @param directory 数据目录，其中的 borrow_records.csv 必须已存在
     * @throws StorageException 读取失败、CSV 格式或字段转换失败，或记录编号不是正数或重复
     */
    public CsvBorrowRecordDao(Path directory) {
        file = directory.toAbsolutePath().normalize().resolve("borrow_records.csv");
        List<List<String>> rows = CsvFileIo.read(file, HEADER);
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                addLoadedRecord(new BorrowRecord(Long.parseLong(row.get(0)),
                        Long.parseLong(row.get(1)),
                        Long.parseLong(row.get(2)),
                        LocalDateTime.parse(row.get(3)),
                        row.get(4).isEmpty() ? null : LocalDateTime.parse(row.get(4))));
            } catch (RuntimeException exception) {
                throw CsvFileIo.invalidRowException(file, i, exception);
            }
        }
    }

    @Override
    public Optional<BorrowRecord> findById(long id) {
        return Optional.ofNullable(records.get(id));
    }

    @Override
    public List<BorrowRecord> findAll() {
        return List.copyOf(records.values());
    }

    @Override
    public List<BorrowRecord> findByUserId(long userId) {
        return records.values().stream().filter(record -> record.userId() == userId).toList();
    }

    @Override
    public long countUnreturnedByBookId(long bookId) {
        return records.values().stream()
                .filter(record -> record.bookId() == bookId && !record.isReturned()).count();
    }

    @Override
    public boolean existsUnreturned(long userId, long bookId) {
        return records.values().stream().anyMatch(record -> record.userId() == userId
                && record.bookId() == bookId && !record.isReturned());
    }

    @Override
    public BorrowRecord insert(long userId, long bookId, LocalDateTime borrowedAt) {
        BorrowRecord record = new BorrowRecord(nextId(), userId, bookId, borrowedAt, null);
        save(record);
        return record;
    }

    @Override
    public void update(BorrowRecord record) {
        if (!records.containsKey(record.id())) {
            throw new StorageException("要更新的借阅记录不存在");
        }
        save(record);
    }

    private void addLoadedRecord(BorrowRecord value) {
        if (value.id() <= 0 || records.putIfAbsent(value.id(), value) != null) {
            throw new IllegalArgumentException("编号必须为不重复的正整数");
        }
    }

    /**
     * 取已有最大编号加一；没有借阅记录时从 1 开始。
     *
     * @return 新借阅记录编号
     * @throws StorageException 最大编号已达到 {@link Long#MAX_VALUE}
     */
    private long nextId() {
        try {
            return records.isEmpty() ? 1 : Math.addExact(records.lastKey(), 1);
        } catch (ArithmeticException exception) {
            throw new StorageException("编号已达到 long 类型上限", exception);
        }
    }

    /**
     * 将借阅记录加入数据副本，并用副本中的全部记录重写 CSV 文件。
     *
     * <p>时间以 ISO 本地日期时间格式保存，尚未归还的记录将归还时间写为空字段。
     * 文件写入成功后才替换当前内存数据；写入抛出异常时，当前内存数据保持不变。
     *
     * @param record 要保存的记录；编号已存在时替换原记录
     * @throws StorageException 写入 CSV 文件失败
     */
    private void save(BorrowRecord record) {
        NavigableMap<Long, BorrowRecord> copy = new TreeMap<>(records);
        copy.put(record.id(), record);
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADER);
        for (BorrowRecord value : copy.values()) {
            rows.add(List.of(Long.toString(value.id()),
                    Long.toString(value.userId()),
                    Long.toString(value.bookId()),
                    value.borrowedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    value.returnedAt() == null
                            ? ""
                            : value.returnedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        CsvFileIo.write(file, rows);
        records = copy;
    }
}
