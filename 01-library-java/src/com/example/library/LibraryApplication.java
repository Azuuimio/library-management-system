package com.example.library;

import com.example.library.dao.csv.CsvBookDao;
import com.example.library.dao.csv.CsvBorrowRecordDao;
import com.example.library.dao.csv.CsvDataValidator;
import com.example.library.dao.csv.CsvInitializer;
import com.example.library.dao.csv.CsvUserDao;
import com.example.library.exception.StorageException;
import com.example.library.service.BookService;
import com.example.library.service.BorrowRecordService;
import com.example.library.service.UserService;
import com.example.library.ui.ConsoleIo;
import com.example.library.ui.ConsoleUi;

import java.io.UncheckedIOException;
import java.nio.file.Path;

public class LibraryApplication {
    private LibraryApplication() {
    }

    public static void main(String[] args) {
        ConsoleIo io = new ConsoleIo();
        try {
            Path directory = Path.of("data");
            CsvInitializer.initialize(directory);
            CsvUserDao userDao = new CsvUserDao(directory);
            CsvBookDao bookDao = new CsvBookDao(directory);
            CsvBorrowRecordDao borrowRecordDao = new CsvBorrowRecordDao(directory);
            CsvDataValidator.validate(userDao, bookDao, borrowRecordDao);
            UserService userService = new UserService(userDao);
            BookService bookService = new BookService(bookDao, borrowRecordDao);
            BorrowRecordService borrowRecordService = new BorrowRecordService(bookDao, borrowRecordDao, userDao);
            new ConsoleUi(io, userService, bookService, borrowRecordService).run();
        } catch (StorageException | UncheckedIOException exception) {
            io.println("程序停止：" + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
