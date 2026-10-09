package com.example.library.service;

import com.example.library.dao.BookDao;
import com.example.library.db.JdbcExecutor;
import com.example.library.exception.BusinessException;
import com.example.library.exception.StorageException;
import com.example.library.model.Book;
import com.example.library.model.view.BookView;
import com.example.library.validation.BookValidator;

import java.math.BigDecimal;
import java.util.List;

/**
 * 处理图书的新增、修改、删除和查询，展示结果中的未归还数量由 DAO 查询计算。
 *
 * <p>删除图书和减少总数量前会检查借阅记录。
 * 管理员入口由控制台菜单提供，此类不检查调用者的角色。
 */
public final class BookService {
    private final JdbcExecutor executor;

    public BookService(JdbcExecutor executor) {
        this.executor = executor;
    }

    /**
     * 校验图书字段，去除文本首尾空白、补齐价格小数位后保存新图书。
     *
     * @param title 书名，规则见 {@link BookValidator#validateAndNormalizeText(String, String)}
     * @param author 作者，规则见 {@link BookValidator#validateAndNormalizeText(String, String)}
     * @param price 价格，规则见 {@link BookValidator#validateAndNormalizePrice(BigDecimal)}
     * @param totalQuantity 非负的图书总数量
     * @return 保存后的图书，包含新分配的编号
     * @throws BusinessException 任一字段未通过校验
     * @throws com.example.library.exception.StorageException 编号无法分配或保存失败
     */
    public Book add(String title, String author, BigDecimal price, int totalQuantity) {
        String normalizedTitle = BookValidator.validateAndNormalizeText(title, "书名");
        String normalizedAuthor = BookValidator.validateAndNormalizeText(author, "作者");
        BigDecimal normalizedPrice = BookValidator.validateAndNormalizePrice(price);
        int validatedQuantity = BookValidator.validateQuantity(totalQuantity);
        return executor.executeInTransaction(context -> context.bookDao().insert(
                normalizedTitle, normalizedAuthor, normalizedPrice, validatedQuantity));
    }

    /**
     * 将图书标记为已删除。
     *
     * <p>删除后，图书不再出现在图书列表和书名搜索结果中。
     * 图书数据仍然保留，历史借阅记录可以继续查询。
     *
     * @param bookId 要删除的图书编号
     * @param expectedVersion 确认删除前查看的图书版本号
     * @throws BusinessException 图书不存在、已删除、版本过期，或仍有未归还记录
     * @throws com.example.library.exception.StorageException 保存删除状态失败
     */
    public void delete(long bookId, long expectedVersion) {
        executor.executeInTransaction(context -> {
            Book book = findNotDeletedByIdForUpdate(context.bookDao(), bookId);
            if (book.version() != expectedVersion) {
                throw new BusinessException("图书信息已变化，请重新查看后删除");
            }
            if (context.borrowRecordDao().countUnreturnedByBookId(bookId) != 0) {
                throw new BusinessException("该图书仍有未归还记录，不能删除");
            }
            if (context.bookDao().markDeleted(bookId, expectedVersion) != 1) {
                throw new StorageException("删除图书的受影响行数异常");
            }
            return null;
        });
    }

    /**
     * 修改未删除图书的书名、作者、价格和总数量。
     *
     * <p>文本和价格的校验规则与新增图书相同。
     * 新的总数量不能小于该图书当前未归还的数量。
     *
     * @param bookId 要修改的图书编号
     * @param title 新书名，去除首尾空白后须为 1 到 200 个 Unicode 码点
     * @param author 新作者，去除首尾空白后须为 1 到 200 个 Unicode 码点
     * @param price 新价格，规则见 {@link BookValidator#validateAndNormalizePrice(BigDecimal)}
     * @param totalQuantity 新的非负总数量，包括已借出但未归还的数量
     * @param expectedVersion 输入修改值前查看的图书版本号
     * @throws BusinessException 图书不存在或已删除、版本过期、字段校验失败，或新总数量小于未归还数量
     * @throws com.example.library.exception.StorageException 保存修改失败
     */
    public void update(long bookId, String title, String author,
                       BigDecimal price, int totalQuantity, long expectedVersion) {
        String normalizedTitle = BookValidator.validateAndNormalizeText(title, "书名");
        String normalizedAuthor = BookValidator.validateAndNormalizeText(author, "作者");
        BigDecimal normalizedPrice = BookValidator.validateAndNormalizePrice(price);
        int validatedQuantity = BookValidator.validateQuantity(totalQuantity);
        executor.executeInTransaction(context -> {
            Book book = findNotDeletedByIdForUpdate(context.bookDao(), bookId);
            if (book.version() != expectedVersion) {
                throw new BusinessException("图书信息已变化，请重新查看后修改");
            }
            if (validatedQuantity < context.borrowRecordDao().countUnreturnedByBookId(bookId)) {
                throw new BusinessException("总数不能小于当前未归还数量");
            }
            if (context.bookDao().update(bookId, normalizedTitle, normalizedAuthor,
                    normalizedPrice, validatedQuantity, expectedVersion) != 1) {
                throw new StorageException("修改图书的受影响行数异常");
            }
            return null;
        });
    }

    /**
     * 查询指定的未删除图书，并附上当前未归还数量。
     *
     * @param bookId 图书编号
     * @return 供界面展示的图书信息
     * @throws BusinessException 图书不存在或已删除
     * @throws com.example.library.exception.StorageException 查询图书展示信息或数据库连接处理失败
     */
    public BookView findById(long bookId) {
        return executor.executeQuery(context -> context.bookDao().findNotDeletedViewById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在或已删除")));
    }

    /**
     * 查询全部未删除图书，并为每本图书附上当前未归还数量。
     *
     * @return 图书展示信息列表；没有未删除图书时返回空列表
     * @throws com.example.library.exception.StorageException 查询图书展示信息或数据库连接处理失败
     */
    public List<BookView> findAllNotDeleted() {
        return executor.executeQuery(context -> context.bookDao().findAllNotDeletedViews());
    }

    /**
     * 查询书名包含关键词的未删除图书，匹配时忽略大小写。
     *
     * @param keyword 书名关键词，查询前会去除首尾空白
     * @return 包含当前未归还数量的图书展示信息；没有匹配项时返回空列表
     * @throws BusinessException 关键词为 {@code null}、空字符串或仅包含空白字符
     * @throws com.example.library.exception.StorageException 查询图书展示信息或数据库连接处理失败
     */
    public List<BookView> searchNotDeletedByTitle(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException("搜索关键词不能为空");
        }
        return executor.executeQuery(context -> context.bookDao().searchNotDeletedViewsByTitle(keyword.strip()));
    }

    private Book findNotDeletedByIdForUpdate(BookDao bookDao, long bookId) {
        return bookDao.findByIdForUpdate(bookId).filter(book -> !book.deleted())
                .orElseThrow(() -> new BusinessException("图书不存在或已删除"));
    }
}
