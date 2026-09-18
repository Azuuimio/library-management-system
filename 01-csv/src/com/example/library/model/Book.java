package com.example.library.model;

import java.math.BigDecimal;

public record Book(long id, String title, String author, BigDecimal price,
                   int totalQuantity, boolean deleted) {
}
