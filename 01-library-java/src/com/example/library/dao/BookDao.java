package com.example.library.dao;

import com.example.library.model.Book;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 图书数据的查询和保存接口。
 *
 * <p>业务层负责校验图书字段和借阅限制，删除操作通过更新删除标记完成。
 */
public interface BookDao {
    /**
     * 根据编号查询图书，包括已标记为删除的图书。
     *
     * @param id 图书编号
     * @return 找到时返回包含该图书的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     */
    Optional<Book> findById(long id);

    /**
     * 查询全部未删除的图书。
     *
     * @return 未删除的图书列表；没有符合条件的图书时返回空列表
     */
    List<Book> findAllNotDeleted();

    /**
     * 查询书名包含关键词的未删除图书，匹配时忽略大小写。
     *
     * @param keyword 已由调用方去除首尾空白的关键词，不能为 {@code null} 或空字符串
     * @return 匹配的图书列表；没有匹配项时返回空列表
     */
    List<Book> searchNotDeletedByTitle(String keyword);

    /**
     * 保存新图书，为其分配编号并将删除标记设为 {@code false}。
     *
     * @param title 已校验并去除首尾空白的书名
     * @param author 已校验并去除首尾空白的作者
     * @param price 已校验并补齐两位小数的价格
     * @param totalQuantity 已校验的图书总数量
     * @return 保存后的图书，包含分配的编号
     * @throws com.example.library.exception.StorageException 编号无法分配或保存失败
     */
    Book insert(String title, String author, BigDecimal price, int totalQuantity);

    /**
     * 按编号替换已有图书的全部字段，包括删除标记。
     *
     * @param book 由业务层完成校验的图书数据
     * @throws com.example.library.exception.StorageException 对应编号的图书不存在或保存失败
     */
    void update(Book book);
}
