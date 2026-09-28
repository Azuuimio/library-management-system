package com.example.library.model.view;

import com.example.library.model.Book;

public record BookView(Book book, long unreturnedQuantity) {
    public long availableQuantity() {
        return book.totalQuantity() - unreturnedQuantity;
    }
}
