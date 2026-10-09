package com.example.library.dao;

import com.example.library.model.Book;
import com.example.library.model.view.BookView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 图书数据的查询和保存接口。
 *
 * <p>业务层负责字段、库存和版本校验，删除操作通过更新删除标记完成。
 */
public interface BookDao {
    /**
     * 锁定并查询指定图书，包括已删除图书，只能在显式事务中调用。
     *
     * @param id 图书编号
     * @return 找到时返回包含该图书的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     */
    Optional<Book> findByIdForUpdate(long id);

    /**
     * 查询未删除图书及当前未归还数量。
     *
     * @param id 图书编号
     * @return 找到时返回包含该图书展示信息的 {@link Optional}；不存在或已删除时返回 {@link Optional#empty()}
     */
    Optional<BookView> findNotDeletedViewById(long id);

    /**
     * 按图书编号升序查询全部未删除的图书及当前未归还数量。
     *
     * @return 未删除的图书展示信息列表；没有符合条件的图书时返回空列表
     */
    List<BookView> findAllNotDeletedViews();

    /**
     * 查询书名包含关键词的未删除图书，匹配时忽略大小写。
     *
     * @param keyword 已由调用方去除首尾空白的关键词，不能为 {@code null} 或空字符串
     * @return 匹配的图书展示信息列表；没有匹配项时返回空列表
     */
    List<BookView> searchNotDeletedViewsByTitle(String keyword);

    /**
     * 保存新图书，由数据库分配编号，并将删除标记和版本号设为默认值。
     *
     * @param title 已校验并去除首尾空白的书名
     * @param author 已校验并去除首尾空白的作者
     * @param price 已校验并补齐两位小数的价格
     * @param totalQuantity 已校验的图书总数量
     * @return 保存后的图书，包含数据库生成的编号
     * @throws com.example.library.exception.StorageException 编号无法分配或保存失败
     */
    Book insert(String title, String author, BigDecimal price, int totalQuantity);

    /**
     * 更新可编辑字段并递增版本号，调用前须锁定图书并通过业务和版本校验。
     *
     * @param id 图书编号
     * @param title 已校验的书名
     * @param author 已校验的作者
     * @param price 已校验的价格
     * @param totalQuantity 已校验且不小于未归还数量的总数量
     * @param expectedVersion 用户查看图书时的版本号
     * @return 受影响行数
     */
    int update(long id, String title, String author,
               BigDecimal price, int totalQuantity, long expectedVersion);

    /**
     * 标记图书为已删除并递增版本号，调用前须锁定图书并通过业务和版本校验。
     *
     * @param id 图书编号
     * @param expectedVersion 用户确认删除前查看的版本号
     * @return 受影响行数
     */
    int markDeleted(long id, long expectedVersion);
}
