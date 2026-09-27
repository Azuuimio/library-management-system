package com.example.library.model;

public record BorrowView(Borrow record, String username, String bookTitle, boolean bookDeleted) {
}
