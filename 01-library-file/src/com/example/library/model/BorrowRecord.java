package com.example.library.model;

import java.time.LocalDateTime;

public record BorrowRecord(long id, long bookId, long readerId,
                           LocalDateTime borrowedAt, LocalDateTime returnedAt) {
    public boolean isreturned() {
        return returnedAt != null;
    }
}
