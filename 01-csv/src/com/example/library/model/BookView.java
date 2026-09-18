package com.example.library.model;

public record BookView(Book book, long borrowedQuantity) {
    public long availableQuantity() {
        return book.totalQuantity() - borrowedQuantity;
    }
}
