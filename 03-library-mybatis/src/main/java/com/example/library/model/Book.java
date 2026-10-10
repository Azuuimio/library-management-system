package com.example.library.model;

import java.math.BigDecimal;

/**
 * 图书信息，包含总数量、删除标记和图书数据版本号。
 *
 * <p>可借数量由总数量减去未归还数量得到，不单独保存在图书中。
 * 构造器和 setter 不校验字段，新增和修改时由业务层校验。
 */
public final class Book {
    /** 图书编号。 */
    private long id;

    /** 书名。 */
    private String title;

    /** 作者。 */
    private String author;

    /** 价格。 */
    private BigDecimal price;

    /** 图书总数量，包括已借出但未归还的数量。 */
    private int totalQuantity;

    /**
     * 是否已删除；为 {@code true} 时仍保留数据供历史借阅查询。
     */
    private boolean deleted;

    /**
     * 图书数据版本号，管理员修改或删除成功时递增，用于识别过期提交；借还书不改变此版本号。
     */
    private long version;

    public Book() {
    }

    public Book(long id, String title, String author, BigDecimal price,
                int totalQuantity, boolean deleted, long version) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.deleted = deleted;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
