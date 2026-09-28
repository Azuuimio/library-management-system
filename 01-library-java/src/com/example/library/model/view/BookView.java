package com.example.library.model.view;

import com.example.library.model.Book;

public record BookView(Book book, long borrowedQuantity) {
    public long availableQuantity() {
        return book.totalQuantity() - borrowedQuantity;
    }
}
