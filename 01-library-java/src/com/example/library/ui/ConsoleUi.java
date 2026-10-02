package com.example.library.ui;

import com.example.library.exception.BusinessException;
import com.example.library.model.Book;
import com.example.library.model.BorrowRecord;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.model.view.BookView;
import com.example.library.model.view.BorrowRecordView;
import com.example.library.service.BookService;
import com.example.library.service.BorrowRecordService;
import com.example.library.service.UserService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 显示登录菜单和角色菜单，将用户输入交给业务服务并展示结果。
 *
 * <p>管理员可以管理图书和查看全部借阅，读者可以查询图书、借还图书和查看本人借阅。
 * 业务异常在对应操作中显示为失败提示；数据异常继续向程序入口传递。
 */
public final class ConsoleUi {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private final ConsoleIo io;
    private final UserService userService;
    private final BookService bookService;
    private final BorrowRecordService borrowRecordService;

    public ConsoleUi(ConsoleIo io, UserService userService,
                     BookService bookService, BorrowRecordService borrowRecordService) {
        this.io = io;
        this.userService = userService;
        this.bookService = bookService;
        this.borrowRecordService = borrowRecordService;
    }

    /**
     * 启动登录菜单，登录成功后进入对应角色的菜单。
     *
     * <p>退出登录后返回登录菜单；选择退出程序或输入流结束时，此方法返回。
     *
     * @throws com.example.library.exception.StorageException 操作中发生数据读写或关联错误
     * @throws java.io.UncheckedIOException 读取控制台输入失败
     */
    public void run() {
        try {
            io.println("欢迎使用图书管理系统！");
            while (true) {
                io.println("""
                        
                        ===菜单===
                        1. 登录
                        0. 退出""");
                switch (readChoice(1)) {
                    case 0 -> {
                        io.println("再见！");
                        return;
                    }
                    case 1 -> login();
                }
            }
        } catch (ConsoleIo.EndOfInputException ignored) {
            io.println("\n输入已结束，程序退出。");
        }
    }

    private void login() {
        io.println("\n===登录===");
        User currentUser;
        try {
            String username = io.readLine("账号：", true).strip();
            currentUser = userService.login(username);
        } catch (ConsoleIo.CancelledInputException ignored) {
            io.println("已取消登录。");
            return;
        } catch (BusinessException exception) {
            io.println("登录失败：" + exception.getMessage());
            pressEnterToContinue();
            return;
        }
        io.println("登录成功，欢迎 " + escapeControlChars(currentUser.username()) + "。");
        menu(currentUser);
    }

    private void menu(User currentUser) {
        boolean admin = currentUser.role() == Role.ADMIN;
        while (true) {
            int choice;
            if (admin) {
                io.println("""                          
                        
                        ===管理员===
                        1. 添加图书
                        2. 删除图书
                        3. 修改图书
                        4. 查询图书
                        5. 全部图书
                        6. 全部借阅
                        0. 退出登录""");
                choice = readChoice(6);
            } else {
                io.println("""

                        ===读者===
                        1. 查询图书
                        2. 全部图书
                        3. 借阅图书
                        4. 归还图书
                        5. 我的借阅
                        0. 退出登录""");
                choice = readChoice(5);
            }
            if (choice == 0) {
                io.println("已退出登录。");
                return;
            }
            try {
                if (admin) {
                    adminAction(choice);
                } else {
                    readerAction(currentUser, choice);
                }
                pressEnterToContinue();
            } catch (ConsoleIo.CancelledInputException ignored) {
                io.println("已取消操作。");
            }
        }
    }

    private void adminAction(int choice) {
        switch (choice) {
            case 1 -> addBook();
            case 2 -> deleteBook();
            case 3 -> updateBook();
            case 4 -> search();
            case 5 -> listAllBooks();
            case 6 -> listAllBorrowRecords();
        }
    }

    private void readerAction(User currentUser, int choice) {
        switch (choice) {
            case 1 -> search();
            case 2 -> listAllBooks();
            case 3 -> borrowBook(currentUser);
            case 4 -> returnBook(currentUser);
            case 5 -> listMyBorrowRecords(currentUser);
        }
    }

    private void addBook() {
        io.println("\n===添加图书===");
        try {
            String title = io.readText("书名：", "书名", null);
            String author = io.readText("作者：", "作者", null);
            BigDecimal price = io.readPrice("价格：", null);
            int quantity = io.readQuantity("总数：", null);
            Book book = bookService.add(title, author, price, quantity);
            io.println("添加成功。图书编号：" + book.id());
        } catch (BusinessException exception) {
            io.println("添加失败：" + exception.getMessage());
        }
    }

    private void deleteBook() {
        io.println("\n===删除图书===");
        try {
            long bookId = io.readId("请输入要删除的图书编号：");
            Book book = bookService.findById(bookId).book();
            if (io.confirm("确认删除图书：" + escapeControlChars(book.title()))) {
                bookService.delete(bookId);
                io.println("删除成功。");
            } else {
                io.println("已取消删除。");
            }
        } catch (BusinessException exception) {
            io.println("删除失败：" + exception.getMessage());
        }
    }

    /**
     * 展示图书当前信息并读取修改值，空输入保留对应字段的当前值。
     *
     * <p>数量输入代表总数，小于当前未归还数量时重新输入，保存前仍由业务层校验。
     */
    private void updateBook() {
        io.println("\n===修改图书===");
        try {
            long bookId = io.readId("请输入要修改的图书编号：");
            BookView current = bookService.findById(bookId);
            Book book = current.book();
            io.println("当前信息：");
            showBook(current);
            io.println("直接回车保留当前值。");
            String title = io.readText("书名 [" + escapeControlChars(book.title()) + "]：", "书名", book.title());
            String author = io.readText("作者 [" + escapeControlChars(book.author()) + "]：", "作者", book.author());
            BigDecimal price = io.readPrice("价格 [" + book.price().toPlainString() + "]：", book.price());
            int quantity;
            while (true) {
                quantity = io.readQuantity("总数 [" + book.totalQuantity() + "]：", book.totalQuantity());
                if (quantity >= current.unreturnedQuantity()) {
                    break;
                }
                io.println("总数不能小于当前未归还数量，请重新输入。");
            }
            bookService.update(bookId, title, author, price, quantity);
            io.println("修改成功。");
        } catch (BusinessException exception) {
            io.println("修改失败：" + exception.getMessage());
        }
    }

    private void listAllBorrowRecords() {
        io.println("\n===全部借阅===");
        showBorrowRecords(borrowRecordService.findAll());
    }

    private void borrowBook(User currentUser) {
        io.println("\n===借阅图书===");
        try {
            long bookId = io.readId("请输入图书编号：");
            BorrowRecord record = borrowRecordService.borrowBook(currentUser, bookId);
            io.println("借阅成功。借阅记录编号：" + record.id());
        } catch (BusinessException exception) {
            io.println("借阅失败：" + exception.getMessage());
        }
    }

    private void returnBook(User currentUser) {
        io.println("\n===归还图书===");
        try {
            long recordId = io.readId("请输入借阅记录编号（可从“我的借阅”查看）：");
            borrowRecordService.returnBook(currentUser, recordId);
            io.println("归还成功。");
        } catch (BusinessException exception) {
            io.println("归还失败：" + exception.getMessage());
        }
    }

    private void listMyBorrowRecords(User currentUser) {
        io.println("\n===我的借阅===");
        showBorrowRecords(borrowRecordService.findByUser(currentUser));
    }

    private void search() {
        io.println("\n===查询图书===");
        try {
            String keyword = io.readLine("请输入书名关键词：", true).strip();
            showBooks(bookService.searchNotDeletedByTitle(keyword), "未找到匹配的图书。");
        } catch (BusinessException exception) {
            io.println("查询失败：" + exception.getMessage());
        }
    }

    private void listAllBooks() {
        io.println("\n===全部图书===");
        showBooks(bookService.findAllNotDeleted(), "暂无图书。");
    }

    private void showBooks(List<BookView> books, String emptyMessage) {
        if (books.isEmpty()) {
            io.println(emptyMessage);
            return;
        }
        for (BookView view : books) {
            showBook(view);
        }
        io.println("共 " + books.size() + " 种图书。");
    }

    private void showBook(BookView view) {
        Book book = view.book();
        io.println("图书编号：" + book.id()
                + " | 书名：" + escapeControlChars(book.title())
                + " | 作者：" + escapeControlChars(book.author())
                + " | 价格：" + book.price().toPlainString()
                + " | 总数：" + book.totalQuantity()
                + " | 借出：" + view.unreturnedQuantity()
                + " | 可借：" + view.availableQuantity());
    }

    private void showBorrowRecords(List<BorrowRecordView> records) {
        if (records.isEmpty()) {
            io.println("暂无借阅记录。");
            return;
        }
        for (BorrowRecordView view : records) {
            BorrowRecord borrowRecord = view.borrowRecord();
            String returnedText = borrowRecord.isReturned()
                    ? DATE_TIME_FORMATTER.format(borrowRecord.returnedAt())
                    : "未归还";
            io.println("借阅记录编号：" + borrowRecord.id() + " | 读者：" + escapeControlChars(view.username())
                    + " | 图书：" + borrowRecord.bookId() + " / " + escapeControlChars(view.bookTitle())
                    + (view.bookDeleted() ? "（已删除）" : "")
                    + " | 借阅：" + DATE_TIME_FORMATTER.format(borrowRecord.borrowedAt())
                    + " | 归还：" + returnedText);
        }
        io.println("共 " + records.size() + " 条借阅记录。");
    }

    /**
     * 读取菜单编号，输入无效时持续提示并重新读取。
     *
     * @param maxChoice 当前菜单允许的最大编号，须为非负数
     * @return 0 到最大编号之间的整数，包含两端
     * @throws ConsoleIo.EndOfInputException 输入流已结束，无法继续读取
     * @throws java.io.UncheckedIOException 读取控制台输入失败
     */
    private int readChoice(int maxChoice) {
        while (true) {
            String choice = io.readLine("请选择功能：", false).strip();
            try {
                int number = Integer.parseInt(choice);
                if (number >= 0 && number <= maxChoice) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
            }
            io.println("输入无效，请输入 0 到 " + maxChoice + " 之间的整数。");
        }
    }

    private void pressEnterToContinue() {
        io.readLine("按回车键继续……", false);
    }

    /**
     * 将文本中的控制字符转换为可见文字，避免书名等内容打断控制台显示。
     *
     * <p>换行、回车和制表符分别显示为 {@code \n}、{@code \r} 和 {@code \t}；
     * 其他 ISO 控制字符显示为反斜杠、字母 u 和四位十六进制编码。
     *
     * @param text 要展示的文本，不能为 {@code null}，可以是空字符串
     * @return 控制字符已替换的文本，其他字符保持不变
     */
    private static String escapeControlChars(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length();) {
            int character = text.codePointAt(i);
            i += Character.charCount(character);
            switch (character) {
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        result.append(String.format("\\u%04x", character));
                    } else {
                        result.appendCodePoint(character);
                    }
                }
            }
        }
        return result.toString();
    }
}
