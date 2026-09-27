package com.example.library.dao.impl.csv;

import com.example.library.model.Book;
import com.example.library.model.BorrowRecord;
import com.example.library.model.User;

import java.util.NavigableMap;
import java.util.TreeMap;

final class CsvState {
    final NavigableMap<Long, User> users = new TreeMap<>();
    final NavigableMap<Long, Book> books = new TreeMap<>();
    final NavigableMap<Long, BorrowRecord> records = new TreeMap<>();

    CsvState copy() {
        CsvState copy = new CsvState();
        copy.users.putAll(users);
        copy.books.putAll(books);
        copy.records.putAll(records);
        return copy;
    }
}
