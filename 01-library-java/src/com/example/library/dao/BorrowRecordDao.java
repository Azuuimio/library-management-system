package com.example.library.dao;

import com.example.library.model.BorrowRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 借阅记录的查询和保存接口。
 *
 * <p>每条记录代表借出一本图书；归还时间为 {@code null} 的记录计入未归还数量。
 */
public interface BorrowRecordDao {
    /**
     * 根据编号查询借阅记录。
     *
     * @param id 借阅记录编号
     * @return 找到时返回包含该记录的 {@link Optional}；找不到时返回 {@link Optional#empty()}
     */
    Optional<BorrowRecord> findById(long id);

    /**
     * 查询全部借阅记录，包括已归还和未归还的记录。
     *
     * @return 按借阅编号升序排列的全部借阅记录；没有记录时返回空列表
     */
    List<BorrowRecord> findAll();

    /**
     * 查询指定用户的全部借阅记录，包括已归还和未归还的记录。
     *
     * @param userId 用户编号
     * @return 该用户的借阅记录；没有记录时返回空列表
     */
    List<BorrowRecord> findByUserId(long userId);

    /**
     * 统计指定图书尚未归还的数量。
     *
     * @param bookId 图书编号
     * @return 该图书归还时间为 {@code null} 的记录数
     */
    long countUnreturnedByBookId(long bookId);

    /**
     * 检查指定用户是否仍有该图书未归还。
     *
     * @param userId 用户编号
     * @param bookId 图书编号
     * @return 存在同时匹配用户和图书的未归还记录时返回 {@code true}，否则返回 {@code false}
     */
    boolean existsUnreturned(long userId, long bookId);

    /**
     * 保存新的借阅记录，为其分配编号并将归还时间设为 {@code null}。
     *
     * @param userId 由业务层确定的借阅读者编号
     * @param bookId 由业务层确定的图书编号
     * @param borrowedAt 精确到秒的借阅时间，不能为 {@code null}
     * @return 保存后的借阅记录，包含分配的编号
     * @throws com.example.library.exception.StorageException 编号无法分配或保存失败
     */
    BorrowRecord insert(long userId, long bookId, LocalDateTime borrowedAt);

    /**
     * 按编号替换已有借阅记录，供归还操作填写归还时间。
     *
     * @param record 由业务层完成校验的借阅记录
     * @throws com.example.library.exception.StorageException 对应编号的记录不存在或保存失败
     */
    void update(BorrowRecord record);
}
