package com.example.library.dao.csv;

import com.example.library.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 在首次运行时创建三个 CSV 文件，并写入预制账号。
 */
public final class CsvInitializer {
    private CsvInitializer() {
    }

    /**
     * 创建数据目录，并根据三个数据文件是否存在决定是否初始化。
     *
     * <p>users.csv、books.csv 和 borrow_records.csv 全部存在时直接返回，不检查内容。
     * 三个文件均不存在时，创建账号 admin、reader1、reader2，以及只有表头的图书和借阅文件。
     * 只有部分文件存在时抛出异常，要求恢复完整备份或使用新的空目录。
     *
     * <p>三个文件按顺序分别写入；后续文件写入失败时，已写入的文件会保留。
     *
     * @param directory 数据目录，不存在时创建
     * @throws StorageException 创建目录失败、数据文件只存在一部分，或写入文件失败
     */
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
