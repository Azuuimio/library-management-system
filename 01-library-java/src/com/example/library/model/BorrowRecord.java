package com.example.library.model;

import java.time.LocalDateTime;

public record BorrowRecord(long id, long userId, long bookId,
                           LocalDateTime borrowedAt, LocalDateTime returnedAt) {
    public boolean isReturned() {
        return returnedAt != null;
    }
}
