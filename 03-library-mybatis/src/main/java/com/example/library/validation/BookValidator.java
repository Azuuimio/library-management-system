package com.example.library.validation;

import com.example.library.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 校验书名、作者、价格和总数量，供控制台输入和业务操作共用。
 *
 * <p>文本会去除首尾空白，价格会补齐到两位小数；校验失败时抛出业务异常。
 */
public final class BookValidator {
    private static final int MAX_TEXT_LENGTH = 200;
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");

    private BookValidator() {
    }

    /**
     * 去除文本首尾空白，并检查长度为 1 到 {@value #MAX_TEXT_LENGTH} 个 Unicode 码点。
     *
     * @param value 待校验的书名或作者
     * @param label 错误提示中使用的字段名称，例如“书名”
     * @return 去除首尾空白后的文本
     * @throws BusinessException 文本为 {@code null}、空字符串或仅包含空白字符，或去除首尾空白后超过长度限制
     */
    public static String validateAndNormalizeText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_TEXT_LENGTH) {
            throw new BusinessException(label + "最多 " + MAX_TEXT_LENGTH + " 个字符");
        }
        return normalized;
    }

    /**
     * 检查价格非负且不超过 {@link #MAX_PRICE}，并将小数位补齐到两位。
     *
     * <p>上下限均可取。小数位数按 {@link BigDecimal#scale()} 判断，超过两位时直接拒绝，
     * 包括 {@code 1.000} 这样的值；此方法不进行四舍五入。
     *
     * @param price 待校验的价格
     * @return 数值不变、保留两位小数的价格
     * @throws BusinessException 价格为 {@code null}、超出范围，或小数位数超过两位
     */
    public static BigDecimal validateAndNormalizePrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException("价格须在 0.00 ~ " + MAX_PRICE.toPlainString() + " 之间");
        }
        if (price.scale() > 2) {
            throw new BusinessException("价格最多两位小数");
        }
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * 检查图书总数量是否为非负数。
     *
     * @param quantity 待校验的总数量
     * @return 传入的总数量
     * @throws BusinessException 总数量小于零
     */
    public static int validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new BusinessException("总数不能为负");
        }
        return quantity;
    }
}
