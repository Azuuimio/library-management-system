package com.example.library.model.view;

import com.example.library.model.BorrowRecord;

public record BorrowRecordView(BorrowRecord record, String username, String bookTitle, boolean bookDeleted) {
}
