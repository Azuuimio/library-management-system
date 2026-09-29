package com.example.library.model;

import java.time.LocalDateTime;

/**
 * 一条借阅记录，表示某位读者借阅一本图书及其归还情况。
 *
 * <p>归还时间为 {@code null} 表示尚未归还。
 * 归还时以填写了归还时间的新记录替换原记录，保留原编号和借阅时间。
 * 此记录的构造器不校验字段。
 *
 * @param id 借阅记录编号
 * @param userId 借阅读者的用户编号
 * @param bookId 所借图书的编号
 * @param borrowedAt 借阅时间
 * @param returnedAt 归还时间；尚未归还时为 {@code null}
 */
public record BorrowRecord(long id, long userId, long bookId,
                           LocalDateTime borrowedAt, LocalDateTime returnedAt) {
    /**
     * 根据归还时间是否已填写，判断图书是否已归还。
     *
     * @return 归还时间不为 {@code null} 时返回 {@code true}
     */
    public boolean isReturned() {
        return returnedAt != null;
    }
}
