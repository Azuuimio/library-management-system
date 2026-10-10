package com.example.library.model;

import java.time.LocalDateTime;

/**
 * 一条借阅记录，表示某位读者借阅一本图书及其归还情况。
 *
 * <p>归还时间为 {@code null} 表示尚未归还。
 * 归还操作填写归还时间，保留原编号和借阅时间。
 * 构造器和 setter 不校验字段。
 */
public final class BorrowRecord {
    /** 借阅记录编号。 */
    private long id;

    /** 借阅读者的用户编号。 */
    private long userId;

    /** 所借图书的编号。 */
    private long bookId;

    /** 借阅时间。 */
    private LocalDateTime borrowedAt;

    /** 归还时间；尚未归还时为 {@code null}。 */
    private LocalDateTime returnedAt;

    public BorrowRecord() {
    }

    public BorrowRecord(long id, long userId, long bookId, LocalDateTime borrowedAt, LocalDateTime returnedAt) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowedAt = borrowedAt;
        this.returnedAt = returnedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public LocalDateTime getBorrowedAt() {
        return borrowedAt;
    }

    public void setBorrowedAt(LocalDateTime borrowedAt) {
        this.borrowedAt = borrowedAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    /**
     * 根据归还时间是否已填写，判断图书是否已归还。
     *
     * @return 归还时间不为 {@code null} 时返回 {@code true}，否则返回 {@code false}
     */
    public boolean isReturned() {
        return returnedAt != null;
    }
}
