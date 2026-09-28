package com.example.library.dao.csv;

import com.example.library.dao.BorrowRecordDao;
import com.example.library.exception.StorageException;
import com.example.library.model.BorrowRecord;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvBorrowRecordDao implements BorrowRecordDao {
    static final List<String> HEADER = List.of("id", "userId", "bookId", "borrowedAt", "returnedAt");
    private final Path file;
    NavigableMap<Long, BorrowRecord> records = new TreeMap<>();

    public CsvBorrowRecordDao(Path directory) {
        file = directory.toAbsolutePath().normalize().resolve("borrow_records.csv");
        List<List<String>> rows = CsvFileIO.read(file, HEADER);
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                put(new BorrowRecord(Long.parseLong(row.get(0)),
                        Long.parseLong(row.get(1)),
                        Long.parseLong(row.get(2)),
                        LocalDateTime.parse(row.get(3)),
                        row.get(4).isEmpty() ? null : LocalDateTime.parse(row.get(4))));
            } catch (RuntimeException exception) {
                throw CsvFileIO.invalidRow(file, i, exception);
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

    private void put(BorrowRecord value) {
        if (value.id() <= 0 || records.putIfAbsent(value.id(), value) != null) {
            throw new IllegalArgumentException("编号必须为不重复的正整数");
        }
    }

    private long nextId() {
        try {
            return records.isEmpty() ? 1 : Math.addExact(records.lastKey(), 1);
        } catch (ArithmeticException exception) {
            throw new StorageException("编号已达到 long 类型上限", exception);
        }
    }

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
                    value.returnedAt() == null ? "" : value.returnedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        CsvFileIO.write(file, rows);
        records = copy;
    }
}