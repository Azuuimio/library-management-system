package com.example.library.mapper;

import com.example.library.model.BorrowRecord;
import com.example.library.model.view.BorrowRecordView;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 借阅记录的查询和保存接口。
 *
 * <p>每条记录代表借出一本图书；归还时间为 {@code null} 的记录计入未归还数量。
 * 写入前由业务层锁定关联图书并检查借阅限制。
 */
public interface BorrowRecordMapper {
    /**
     * 根据编号查询借阅记录。
     *
     * <p>此查询不锁定借阅行。
     *
     * @param id 借阅记录编号
     * @return 找到时返回包含该记录的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询借阅记录失败
     */
    Optional<BorrowRecord> findById(@Param("id") long id);

    /**
     * 查询全部借阅记录，包括已归还和未归还的记录。
     *
     * @return 按借阅编号升序排列的全部借阅记录展示信息；没有记录时返回空列表
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询借阅展示信息失败
     */
    List<BorrowRecordView> findAllViews();

    /**
     * 查询指定用户的全部借阅记录，包括已归还和未归还的记录。
     *
     * @param userId 用户编号
     * @return 按借阅编号升序排列的该用户的借阅记录展示信息；没有记录时返回空列表
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询借阅展示信息失败
     */
    List<BorrowRecordView> findViewsByUserId(@Param("userId") long userId);

    /**
     * 统计指定图书尚未归还的数量。
     *
     * <p>用于写入判断时须先锁定图书。
     *
     * @param bookId 图书编号
     * @return 该图书归还时间为 {@code null} 的记录数
     * @throws org.apache.ibatis.exceptions.PersistenceException 统计未归还数量失败
     */
    long countUnreturnedByBookId(@Param("bookId") long bookId);

    /**
     * 检查指定用户是否仍有该图书未归还。
     *
     * <p>用于写入判断时须先锁定图书。
     *
     * @param userId 用户编号
     * @param bookId 图书编号
     * @return 存在同时匹配用户和图书的未归还记录时返回 {@code true}，否则返回 {@code false}
     * @throws org.apache.ibatis.exceptions.PersistenceException 查询重复借阅状态失败
     */
    boolean existsUnreturned(@Param("userId") long userId, @Param("bookId") long bookId);

    /**
     * 保存新的借阅记录，并将数据库生成的编号写入记录对象。
     *
     * @param record 已由业务层检查借阅限制的记录，借阅时间不能为 {@code null} 且精确到秒，
     *               归还时间使用数据库默认值
     * @return 受影响行数
     * @throws org.apache.ibatis.exceptions.PersistenceException 保存借阅记录或回填编号失败
     */
    int insert(BorrowRecord record);

    /**
     * 为属于当前读者且未归还的记录填写归还时间。
     *
     * @param id 借阅记录编号
     * @param userId 当前读者编号
     * @param returnedAt 由业务层校验、精确到秒的归还时间，不能为 {@code null}，
     *                   且不能早于借阅时间
     * @return 受影响行数
     * @throws org.apache.ibatis.exceptions.PersistenceException 保存归还时间失败
     */
    int markReturnedIfUnreturned(@Param("id") long id, @Param("userId") long userId,
                                @Param("returnedAt") LocalDateTime returnedAt);
}
