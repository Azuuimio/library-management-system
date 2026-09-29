package com.example.library;

import com.example.library.dao.csv.*;
import com.example.library.exception.StorageException;
import com.example.library.service.BookService;
import com.example.library.service.BorrowRecordService;
import com.example.library.service.UserService;
import com.example.library.ui.ConsoleIO;
import com.example.library.ui.ConsoleUi;

import java.io.UncheckedIOException;
import java.nio.file.Path;

public class LibraryApplication {
     private LibraryApplication() {
     }
     public static void main(String[] args) {
         ConsoleIO io = new ConsoleIO();
         try {
             Path directory = Path.of("data");
             CsvInitializer.initialize(directory);
             CsvUserDao userDao = new CsvUserDao(directory);
             CsvBookDao bookDao = new CsvBookDao(directory);
             CsvBorrowRecordDao recordDao = new CsvBorrowRecordDao(directory);
             CsvDataValidator.validate(userDao, bookDao, recordDao);
             UserService userService = new UserService(userDao);
             BookService bookService = new BookService(bookDao, recordDao);
             BorrowRecordService borrowService = new BorrowRecordService(bookDao, recordDao, userDao);
             new ConsoleUi(io, userService, bookService, borrowService).run();
         } catch (StorageException | UncheckedIOException exception) {
             io.println("程序停止：" + exception.getMessage());
             exception.printStackTrace(System.err);
             System.exit(1);
         }
     }
}
