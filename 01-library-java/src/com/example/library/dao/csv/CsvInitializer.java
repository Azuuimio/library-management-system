package com.example.library.dao.csv;

import com.example.library.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvInitializer {
    private CsvInitializer() {
    }

    public static void initialize(Path directory) {
        try {
            Files.createDirectories(directory);
            List<String> names = List.of("users.csv", "books.csv", "borrow_records.csv");
            long existing = names.stream().filter(name -> Files.exists(directory.resolve(name))).count();
            if (existing == names.size()) {
                return;
            }
            if (existing != 0) {
                throw new StorageException("数据文件不完整：需要 users.csv、books.csv、borrow_records.csv；请恢复完整备份或使用新的空目录");
            }
            CsvFileIO.write(directory.resolve("users.csv"), List.of(CsvUserDao.HEADER,
                    List.of("1", "admin", "ADMIN"), List.of("2", "reader1", "READER"),
                    List.of("3", "reader2", "READER")));
            CsvFileIO.write(directory.resolve("books.csv"), List.of(CsvBookDao.HEADER));
            CsvFileIO.write(directory.resolve("borrow_records.csv"), List.of(CsvBorrowRecordDao.HEADER));
        } catch (IOException exception) {
            throw new StorageException("初始化数据目录失败：" + directory, exception);
        }
    }
}
