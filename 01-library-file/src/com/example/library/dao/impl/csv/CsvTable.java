package com.example.library.dao.impl.csv;

import java.util.List;

enum CsvTable {
    USERS("users.csv", List.of("id", "username", "role")),
    BOOKS("books.csv", List.of("id", "title", "author", "price", "totalQuantity", "deleted")),
    RECORDS("borrow_records.csv", List.of("id", "userId", "bookId", "borrowedAt", "returnedAt"));

    final String filename;
    final List<String> header;

    CsvTable(String filename, List<String> header) {
        this.filename = filename;
        this.header = header;
    };
}
