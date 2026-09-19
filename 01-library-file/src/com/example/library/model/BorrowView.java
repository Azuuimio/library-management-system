package com.example.library.model;

public record BorrowView(BorrowRecord record, String username, String bookTitle, boolean bookDeleted) {
}
