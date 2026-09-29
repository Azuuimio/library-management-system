package com.example.library.exception;

/**
 * 表示数据文件读写失败、文件内容无效或数据之间的关联不成立。
 *
 * <p>此异常传到程序入口后，程序显示原因、输出异常堆栈并以状态码 1 退出。
 */
public final class StorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 创建包含数据错误说明的异常。
     *
     * @param message 数据错误或读写失败的原因
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * 创建数据异常，并保留引发失败的原始异常。
     *
     * @param message 数据错误或读写失败的原因
     * @param cause 引发失败的原始异常
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}