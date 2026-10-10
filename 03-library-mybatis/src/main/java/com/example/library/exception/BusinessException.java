package com.example.library.exception;

/**
 * 表示输入或操作不符合业务规则。
 *
 * <p>界面捕获此异常后显示原因，用户可以重新输入或继续选择其他操作。
 */
public final class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 创建包含业务失败原因的异常。
     *
     * @param message 向用户显示的失败原因
     */
    public BusinessException(String message) {
        super(message);
    }
}
