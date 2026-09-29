package com.example.library.dao.csv;

import com.example.library.exception.StorageException;
import com.example.library.model.Book;
import com.example.library.model.BorrowRecord;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.validation.BookValidator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CsvDataValidator {
    private CsvDataValidator() {
    }

    public static void validate(CsvUserDao userDao, CsvBookDao bookDao, CsvBorrowRecordDao borrowRecordDao) {
        try {
            check(userDao.users.values().stream().filter(user -> user.role() == Role.ADMIN).count() == 1,
                    "必须且只能有一个管理员");
            Set<String> usernames = new HashSet<>();
            for (User user : userDao.users.values()) {
                check(!user.username().isBlank() && user.username().equals(user.username().strip())
                        && user.username().codePointCount(0, user.username().length()) <= 50, "用户名格式无效");
                check(usernames.add(user.username()), "用户名重复");
            }
            for (Book book : bookDao.books.values()) {
                check(BookValidator.validateAndNormalizeText(book.title(), "书名").equals(book.title()), "书名首尾不能有空白");
                check(BookValidator.validateAndNormalizeText(book.author(), "作者").equals(book.author()), "作者首尾不能有空白");
                BookValidator.validateAndNormalizePrice(book.price());
                BookValidator.validateQuantity(book.totalQuantity());
            }
            Map<Long, Long> unreturnedCounts = new HashMap<>();
            Set<String> activePairs = new HashSet<>();
            for (BorrowRecord record : borrowRecordDao.records.values()) {
                User user = userDao.users.get(record.userId());
                Book book = bookDao.books.get(record.bookId());
                check(user != null && user.role() == Role.READER, "借阅记录关联的读者不存在");
                check(book != null, "借阅记录关联的图书不存在");
                check(record.borrowedAt() != null && record.borrowedAt().getNano() == 0,
                        "时间须为非空的整秒时间");
                if (record.isReturned()) {
                    check(record.returnedAt().getNano() == 0,
                            "时间须为非空的整秒时间");
                    check(!record.returnedAt().isBefore(record.borrowedAt()), "归还时间早于借阅时间");
                } else {
                    check(!book.deleted(), "已删除图书仍有未归还记录");
                    check(activePairs.add(record.userId() + ":" + record.bookId()), "同一读者重复借阅未归还");
                    unreturnedCounts.merge(record.bookId(), 1L, Long::sum);
                }
            }
            for (Map.Entry<Long, Long> entry : unreturnedCounts.entrySet()) {
                check(entry.getValue() <= bookDao.books.get(entry.getKey()).totalQuantity(), "未归还数量超过总数量");
            }
        } catch (RuntimeException exception) {
            throw new StorageException("数据完整性检查失败：" + exception.getMessage(), exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
