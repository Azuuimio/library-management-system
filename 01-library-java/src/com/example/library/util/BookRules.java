package com.example.library.util;

import com.example.library.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BookRules {
    public static final int MAX_TEXT_LENGTH = 200;
    public static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");

    private BookRules() {}

    public static String text(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_TEXT_LENGTH) {
            throw new BusinessException(label + "最多 " + MAX_TEXT_LENGTH + " 个字符");
        }
        return normalized;
    }

    public static BigDecimal price(BigDecimal price) {
        if (price == null || price.signum() < 0  || price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException("价格须在 0.00 ~ " + MAX_PRICE.toPlainString() + " 之间");
        }
        if (price.scale() > 2) {
            throw new BusinessException("价格最多两位小数");
        }
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static int quantity(int quantity) {
        if (quantity < 0) {
            throw new BusinessException("总数量不能为负");
        }
        return quantity;
    }
}
