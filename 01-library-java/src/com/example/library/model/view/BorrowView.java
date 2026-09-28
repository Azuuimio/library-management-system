package com.example.library.model.view;

import com.example.library.model.Borrow;

public record BorrowView(Borrow record, String username, String bookTitle, boolean bookDeleted) {
}
