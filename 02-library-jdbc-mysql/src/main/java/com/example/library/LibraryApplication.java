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
import java.nio.file.Path;

/**
 * 程序入口，依次初始化数据文件、加载并检查数据、组装业务对象，然后启动控制台界面。
 */
public final class LibraryApplication {
    private LibraryApplication() {
    }

    /**
     * 使用当前工作目录下的 data 目录启动图书管理系统。
     *
     * <p>捕获 {@link StorageException} 或 {@link UncheckedIOException} 时，
     * 显示原因并输出异常堆栈，以状态码 1 退出。
     *
     * @param args 命令行参数，本程序不使用
     */
    public static void main(String[] args) {
        ConsoleIo io = new ConsoleIo();
        try {
            Path directory = Path.of("data");
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
