package com.example.library;

import com.example.library.db.ConnectionFactory;
import com.example.library.db.JdbcExecutor;
import com.example.library.exception.StorageException;
import com.example.library.service.BookService;
import com.example.library.service.BorrowRecordService;
import com.example.library.service.UserService;
import com.example.library.ui.ConsoleIo;
import com.example.library.ui.ConsoleUi;

import java.io.UncheckedIOException;

/**
 * 程序入口，读取数据库连接配置、组装业务对象，然后启动控制台界面。
 */
public final class LibraryApplication {
    private LibraryApplication() {
    }

    /**
     * 使用环境变量中的数据库连接配置启动图书管理系统。
     *
     * <p>捕获 {@link StorageException} 或 {@link UncheckedIOException} 时，
     * 显示原因并输出异常堆栈，以状态码 1 退出。
     *
     * @param args 命令行参数，本程序不使用
     */
    public static void main(String[] args) {
        ConsoleIo io = new ConsoleIo();
        try {
            JdbcExecutor executor = new JdbcExecutor(new ConnectionFactory());
            UserService userService = new UserService(executor);
            BookService bookService = new BookService(executor);
            BorrowRecordService borrowRecordService = new BorrowRecordService(executor);
            new ConsoleUi(io, userService, bookService, borrowRecordService).run();
        } catch (StorageException | UncheckedIOException exception) {
            io.println("程序停止：" + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
