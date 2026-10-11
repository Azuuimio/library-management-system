package com.example.library.mapper;

import com.example.library.model.Book;
import com.example.library.model.view.BookView;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 图书数据的查询和保存接口。
 *
 * <p>业务层负责字段、库存和版本校验，删除操作通过更新删除标记完成。
 */
public interface BookMapper {
    /**
     * 锁定并查询指定图书，包括已删除图书，只能在显式事务中调用。
     *
     * @param id 图书编号
     * @return 找到时返回包含该图书的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     * @throws org.apache.ibatis.exceptions.PersistenceException 锁定或查询图书失败
     */
    Optional<Book> findByIdForUpdate(@Param("id") long id);

    /**
     * 查询未删除图书及当前未归还数量。
     *
     * @param id 图书编号
     * @return 找到时返回包含该图书展示信息的 {@link Optional}；不存在或已删除时返回 {@link Optional#empty()}
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询图书展示信息失败
     */
    Optional<BookView> findNotDeletedViewById(@Param("id") long id);

    /**
     * 按图书编号升序查询全部未删除的图书及当前未归还数量。
     *
     * @return 未删除的图书展示信息列表；没有符合条件的图书时返回空列表
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询图书展示信息失败
     */
    List<BookView> findAllNotDeletedViews();

    /**
     * 查询书名包含关键词的未删除图书，匹配时忽略大小写。
     *
     * @param keyword 已转义 !、% 和 _，且两端已添加 % 的 LIKE 匹配模式
     * @return 匹配的图书展示信息列表；没有匹配项时返回空列表
     * @throws org.apache.ibatis.exceptions.PersistenceException 按书名搜索图书失败
     */
    List<BookView> searchNotDeletedViewsByTitle(@Param("keyword") String keyword);

    /**
     * 保存新图书，并将数据库生成的编号写入图书对象。
     *
     * @param book 已由业务层校验的图书，删除标记和版本号使用数据库默认值
     * @return 受影响行数
     * @throws org.apache.ibatis.exceptions.PersistenceException 保存图书或回填编号失败
     */
    int insert(Book book);

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
     * @throws org.apache.ibatis.exceptions.PersistenceException 保存图书修改失败
     */
    int update(@Param("id") long id, @Param("title") String title, @Param("author") String author,
               @Param("price") BigDecimal price, @Param("totalQuantity") int totalQuantity,
               @Param("expectedVersion") long expectedVersion);

    /**
     * 标记图书为已删除并递增版本号，调用前须锁定图书并通过业务和版本校验。
     *
     * @param id 图书编号
     * @param expectedVersion 用户确认删除前查看的版本号
     * @return 受影响行数
     * @throws org.apache.ibatis.exceptions.PersistenceException 保存图书删除状态失败
     */
    int markDeleted(@Param("id") long id, @Param("expectedVersion") long expectedVersion);
}
