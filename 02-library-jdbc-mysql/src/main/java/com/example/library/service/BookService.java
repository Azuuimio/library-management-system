package com.example.library.service;

import com.example.library.dao.BookDao;
import com.example.library.dao.BorrowRecordDao;
import com.example.library.exception.BusinessException;
import com.example.library.model.Book;
import com.example.library.model.view.BookView;
import com.example.library.validation.BookValidator;

import java.math.BigDecimal;
import java.util.List;

/**
 * 处理图书的新增、修改、删除和查询，并为展示结果计算未归还数量。
 *
 * <p>删除图书和减少总数量前会检查借阅记录。
 * 管理员入口由控制台菜单提供，此类不检查调用者的角色。
 */
public final class BookService {
    private final BookDao bookDao;
    private final BorrowRecordDao borrowRecordDao;

    public BookService(BookDao bookDao, BorrowRecordDao borrowRecordDao) {
        this.bookDao = bookDao;
        this.borrowRecordDao = borrowRecordDao;
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
        return bookDao.insert(BookValidator.validateAndNormalizeText(title, "书名"),
                BookValidator.validateAndNormalizeText(author, "作者"),
                BookValidator.validateAndNormalizePrice(price),
                BookValidator.validateQuantity(totalQuantity));
    }

    /**
     * 将图书标记为已删除。
     *
     * <p>删除后，图书不再出现在图书列表和书名搜索结果中。
     * 图书数据仍然保留，历史借阅记录可以继续查询。
     *
     * @param bookId 要删除的图书编号
     * @throws BusinessException 图书不存在、已删除，或仍有未归还记录
     * @throws com.example.library.exception.StorageException 保存删除状态失败
     */
    public void delete(long bookId) {
        Book book = findNotDeletedById(bookId);
        if (borrowRecordDao.countUnreturnedByBookId(bookId) != 0) {
            throw new BusinessException("该图书仍有未归还记录，不能删除");
        }
        bookDao.update(new Book(book.id(), book.title(), book.author(), book.price(), book.totalQuantity(), true));
    }

    /**
     * 修改未删除图书的书名、作者、价格和总数量。
     *
     * <p>文本和价格的校验规则与新增图书相同。
     * 新的总数量不能小于该图书当前未归还的数量。
     *
     * @param bookId 要修改的图书编号
     * @param title 新书名，规则见 {@link BookValidator#validateAndNormalizeText(String, String)}
     * @param author 新作者，规则见 {@link BookValidator#validateAndNormalizeText(String, String)}
     * @param price 新价格，规则见 {@link BookValidator#validateAndNormalizePrice(BigDecimal)}
     * @param totalQuantity 新的非负总数量，包括已借出但未归还的数量
     * @throws BusinessException 图书不存在或已删除、字段校验失败，或新总数量小于未归还数量
     * @throws com.example.library.exception.StorageException 保存修改失败
     */
    public void update(long bookId, String title, String author,
                       BigDecimal price, int totalQuantity) {
        Book current = findNotDeletedById(bookId);
        Book replacement = new Book(current.id(),
                BookValidator.validateAndNormalizeText(title, "书名"),
                BookValidator.validateAndNormalizeText(author, "作者"),
                BookValidator.validateAndNormalizePrice(price),
                BookValidator.validateQuantity(totalQuantity),
                false);
        if (replacement.totalQuantity() < borrowRecordDao.countUnreturnedByBookId(bookId)) {
            throw new BusinessException("总数不能小于当前未归还数量");
        }
        bookDao.update(replacement);
    }

    /**
     * 查询指定的未删除图书，并附上当前未归还数量。
     *
     * @param bookId 图书编号
     * @return 供界面展示的图书信息
     * @throws BusinessException 图书不存在或已删除
     */
    public BookView findById(long bookId) {
        return toView(findNotDeletedById(bookId));
    }

    /**
     * 查询全部未删除图书，并为每本图书附上当前未归还数量。
     *
     * @return 图书展示信息列表；没有未删除图书时返回空列表
     */
    public List<BookView> findAllNotDeleted() {
        return bookDao.findAllNotDeleted().stream().map(this::toView).toList();
    }

    /**
     * 查询书名包含关键词的未删除图书，匹配时忽略大小写。
     *
     * @param keyword 书名关键词，查询前会去除首尾空白
     * @return 包含当前未归还数量的图书展示信息；没有匹配项时返回空列表
     * @throws BusinessException 关键词为 {@code null}、空字符串或仅包含空白字符
     */
    public List<BookView> searchNotDeletedByTitle(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException("搜索关键词不能为空");
        }
        return bookDao.searchNotDeletedByTitle(keyword.strip()).stream().map(this::toView).toList();
    }

    private Book findNotDeletedById(long bookId) {
        return bookDao.findById(bookId).filter(book -> !book.deleted())
                .orElseThrow(() -> new BusinessException("图书不存在或已删除"));
    }

    private BookView toView(Book book) {
        return new BookView(book, borrowRecordDao.countUnreturnedByBookId(book.id()));
    }
}
