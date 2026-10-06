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

/**
 * 在三个 CSV 文件加载后，检查用户、图书和借阅记录之间的数据一致性。
 *
 * <p>检查失败时抛出数据异常，由程序入口终止启动。
 */
public final class CsvDataValidator {
    private CsvDataValidator() {
    }

    /**
     * 检查已加载的数据是否满足账号、图书字段和借阅关联规则。
     *
     * <p>账号必须恰有一个管理员，账号名称须唯一、无首尾空白且长度为 1 到 50 个 Unicode 码点。
     * 图书字段须通过 {@link BookValidator} 校验，书名和作者不能有首尾空白。
     *
     * <p>借阅记录须关联存在的读者和图书，借阅时间不能为 {@code null} 且须精确到秒。
     * 已填写的归还时间须精确到秒，且不能早于借阅时间。
     * 未归还记录不能关联已删除图书，同一读者不能有同一本图书的多条未归还记录，
     * 每本图书的未归还数量不能超过总数量。
     *
     * <p>此方法只检查数据，不修改字段或回写文件。
     *
     * @param userDao 已加载账号数据的 DAO
     * @param bookDao 已加载图书数据的 DAO
     * @param borrowRecordDao 已加载借阅数据的 DAO
     * @throws StorageException 任一规则检查失败，或检查过程中出现运行时异常
     */
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
                check(user != null && user.role() == Role.READER, "借阅记录关联的用户不存在或不是读者");
                check(book != null, "借阅记录关联的图书不存在");
                check(record.borrowedAt() != null && record.borrowedAt().getNano() == 0,
                        "借阅时间须为非空的整秒时间");
                if (record.isReturned()) {
                    check(record.returnedAt().getNano() == 0,
                            "归还时间须为整秒时间");
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
