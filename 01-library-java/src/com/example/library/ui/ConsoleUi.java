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

public class ConsoleUi {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private final ConsoleIO io;
    private final UserService userService;
    private final BookService bookService;
    private final BorrowRecordService borrowService;

    public ConsoleUi(ConsoleIO io, UserService userService, BookService bookService, BorrowRecordService borrowService) {
        this.io = io;
        this.userService = userService;
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    public void run() {
        try {
            while (true) {
                io.println("""
                        
                        
                        ==== 菜单·登录 ====
                          1. 登录
                          0. 退出
                        ==================
                        """);
                switch (readChoice(1)) {
                    case 0 -> {
                        io.println("再见！");
                        return;
                    }
                    case 1 -> login();
                }
            }
        } catch (ConsoleIO.EndOfInput ignored) {
            io.println("\n输入已结束，程序退出。");
        }
    }

    private void login() {
        io.println("—— 登录 ——");
        String username = io.line("账号：");
        User currentUser;
        try {
            currentUser = userService.login(username);
        } catch (BusinessException exception) {
            io.println("登录失败：" + exception.getMessage());
            pressEnterToContinue();
            return;
        }
        io.println("登录成功，欢迎 " + visible(currentUser.username()) + "。");
        menu(currentUser);
    }

    private void menu(User currentUser) {
        boolean admin = currentUser.role() == Role.ADMIN;
        while (true) {
            int choice;
            if (admin) {
                io.println("""
                        
                        
                        === 菜单·管理员 ===
                          1. 添加图书
                          2. 删除图书
                          3. 修改图书
                          4. 查询图书
                          5. 全部图书
                          6. 全部借阅
                          0. 退出登录
                        ==================""");
                choice = readChoice(6);
            } else {
                io.println("""
                        
                        
                        ==== 菜单·读者 ====
                          1. 查询图书
                          2. 借阅图书
                          3. 归还图书
                          4. 我的借阅
                          0. 退出登录
                        ==================""");
                choice = readChoice(4);
            }
            if (choice == 0) {
                io.println("已退出登录。");
                return;
            }
            if (admin) {
                adminAction(choice);
            } else {
                readerAction(currentUser, choice);
            }
            pressEnterToContinue();
        }
    }

    private void adminAction(int choice) {
        switch (choice) {
            case 1 -> addBook();
            case 2 -> deleteBook();
            case 3 -> updateBook();
            case 4 -> search();
            case 5 -> listBooks();
            case 6 -> listAllRecords();
        }
    }

    private void readerAction(User currentUser, int choice) {
        switch (choice) {
            case 1 -> search();
            case 2 -> borrowBook(currentUser);
            case 3 -> returnBook(currentUser);
            case 4 -> listMyRecords(currentUser);
        }
    }

    private void addBook() {
        io.println("—— 添加图书 ——");
        try {
            String title = io.text("书名：", null);
            String author = io.text("作者：", null);
            BigDecimal price = io.price("价格：", null);
            int quantity = io.quantity("总数量：", null);
            Book book = bookService.add(title, author, price, quantity);
            io.println("添加成功。图书编号：" + book.id());
        } catch (BusinessException exception) {
            io.println("添加失败：" + exception.getMessage());
        }
    }

    private void deleteBook() {
        io.println("—— 删除图书 ——");
        try {
            long bookId = io.id("请输入要删除的图书编号：");
            Book book = bookService.findById(bookId).book();
            if (io.confirm("确认删除《" + visible(book.title()) + "》")) {
                bookService.delete(bookId);
                io.println("删除成功。");
            } else {
                io.println("已取消删除。");
            }
        } catch (BusinessException exception) {
            io.println("删除失败：" + exception.getMessage());
        }
    }

    private void updateBook() {
        io.println("—— 修改图书 ——");
        try {
            long bookId = io.id("请输入要修改的图书编号：");
            BookView current = bookService.findById(bookId);
            Book book = current.book();
            io.println("当前信息：");
            showBook(current);
            io.println("直接回车保留当前值；数量指总数量，不是可借数量。");
            String title = io.text("书名 [" + visible(book.title()) + "]：", book.title());
            String author = io.text("作者 [" + visible(book.author()) + "]：", book.author());
            BigDecimal price = io.price("价格 [" + book.price().toPlainString() + "]：", book.price());
            int quantity = io.quantity("总数量 [" + book.totalQuantity() + "]：", book.totalQuantity());
            bookService.update(bookId, title, author, price, quantity);
            io.println("修改成功。");
        } catch (BusinessException exception) {
            io.println("修改失败：" + exception.getMessage());
        }
    }

    private void search() {
        io.println("—— 查询图书 ——");
        try {
            showBooks(bookService.findByTitle(io.line("请输入书名（支持模糊匹配）：")));
        } catch (BusinessException exception) {
            io.println("查询失败：" + exception.getMessage());
        }
    }

    private int readChoice(int max) {
        while (true) {
            String choice = io.line("请选择功能：").strip();
            try {
                int number = Integer.parseInt(choice);
                if (number >= 0 && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {}
            io.println("输入无效，请输入0-" + max + "之间的整数。");
        }
    }

    private void borrowBook(User currentUser) {
        io.println("—— 借阅图书 ——");
        try {
            long bookId = io.id("请输入图书编号：");
            BorrowRecord record = borrowService.borrowBook(currentUser, bookId);
            io.println("借阅成功。借阅记录编号：" + record.id());
        } catch (BusinessException exception) {
            io.println("借阅失败：" + exception.getMessage());
        }
    }

    private void returnBook(User currentUser) {
        io.println("—— 归还图书 ——");
        try {
            long recordId = io.id("请输入借阅记录编号（可从“我的借阅”查看）：");
            borrowService.returnBook(currentUser, recordId);
            io.println("归还成功。");
        } catch (BusinessException exception) {
            io.println("归还失败：" + exception.getMessage());
        }
    }

    private void listBooks() {
        io.println("—— 全部图书 ——");
        try {
            showBooks(bookService.findAll());
        } catch (BusinessException exception) {
            io.println("查询失败：" + exception.getMessage());
        }
    }

    private void listAllRecords() {
        io.println("—— 全部借阅 ——");
        try {
            showRecords(borrowService.findAll());
        } catch (BusinessException exception) {
            io.println("查询失败：" + exception.getMessage());
        }
    }

    private void listMyRecords(User currentUser) {
        io.println("\n—— 我的借阅 ——");
        try {
            showRecords(borrowService.findByUser(currentUser));
        } catch (BusinessException exception) {
            io.println("查询失败：" + exception.getMessage());
        }
    }

    private void showBooks(List<BookView> books) {
        if (books.isEmpty()) {
            io.println("未找到匹配的图书。");
            return;
        }
        for (BookView view : books) {
            showBook(view);
        }
        io.println("共 " + books.size() + " 种图书。");
    }

    private void showBook(BookView view) {
        Book book = view.book();
        io.println("编号：" + book.id()
                   + " | 书名：" + visible(book.title())
                   + " | 作者：" + visible(book.author())
                   + " | 价格：" + book.price().toPlainString()
                   + " | 总数：" + book.totalQuantity()
                   + " | 借出：" + view.unreturnedQuantity()
                   + " | 可借：" + view.availableQuantity());
    }

    private void showRecords(List<BorrowRecordView> records) {
        if (records.isEmpty()) {
            io.println("暂无借阅记录。");
            return;
        }
        for (BorrowRecordView view : records) {
            BorrowRecord record = view.record();
            io.println("借阅编号：" + record.id() + " | 读者：" + visible(view.username())
                       + " | 图书：" + record.bookId() + " / " + visible(view.bookTitle())
                       + (view.bookDeleted() ? "（已删除）" : "")
                       + " | 借阅：" + TIME.format(record.borrowedAt())
                       + " | 归还：" + (record.isReturned() ? TIME.format(record.returnedAt()) : "未归还"));
        }
        io.println("共 " + records.size() + " 条借阅记录。");
    }

    private void pressEnterToContinue() {
        io.line("按回车键继续……");
    }

    private static String visible(String text) {
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int character = text.codePointAt(offset);
            offset += Character.charCount(character);
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
