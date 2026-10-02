package com.example.library.ui;

import com.example.library.exception.BusinessException;
import com.example.library.validation.BookValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 以 UTF-8 读取控制台输入并输出提示文字。
 *
 * <p>以下“空输入”包括空字符串和仅包含空白字符的输入。
 * {@link #readLine(String, boolean)} 根据参数决定是否因空输入取消操作。
 * 编号输入为空时取消；数量、价格和文本输入为空时，有默认值则返回默认值，否则取消。
 * 默认值原样返回，不再校验；编号、数量、价格和文本的非空输入校验失败时，显示提示并重新读取。
 *
 * <p>取消输入时抛出 {@link CancelledInputException}，由控制台界面返回对应菜单。
 * 输入流结束时抛出 {@link EndOfInputException}，由控制台界面统一结束交互。
 */
public final class ConsoleIo {
    private final BufferedReader input;
    private final PrintWriter output;

    /**
     * 使用标准输入和标准输出创建控制台读写对象，字符编码均为 UTF-8。
     */
    public ConsoleIo() {
        input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
    }

    /**
     * 输出一行文字并刷新输出。
     *
     * @param text 要输出的文字
     */
    public void println(String text) {
        output.println(text);
    }

    /**
     * 显示提示后读取一行输入，保留首尾空白，不包含行结束符。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param cancelOnEmpty 是否在空输入时取消当前操作
     * @return 读取到的文字；允许空输入时，直接回车返回空字符串
     * @throws CancelledInputException 启用空输入取消，且输入为空
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public String readLine(String prompt, boolean cancelOnEmpty) {
        output.print(prompt);
        output.flush();
        try {
            String line = input.readLine();
            if (line == null) {
                throw new EndOfInputException();
            }
            if (cancelOnEmpty && line.isBlank()) {
                throw new CancelledInputException();
            }
            return line;
        } catch (IOException exception) {
            throw new UncheckedIOException("读取控制台输入失败", exception);
        }
    }

    /**
     * 读取正整数编号。
     *
     * @param prompt 读取输入前显示的提示文字
     * @return 去除首尾空白后解析得到的编号，范围为 1 到 {@link Long#MAX_VALUE}
     * @throws CancelledInputException 输入为空
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public long readId(String prompt) {
        while (true) {
            try {
                long id = Long.parseLong(readLine(prompt, true).strip());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
            }
            println("请输入有效的正整数编号。");
        }
    }

    /**
     * 读取图书总数，去除首尾空白后按非负整数校验。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param defaultValue 空输入时返回的默认值，可为 {@code null}
     * @return 0 到 {@link Integer#MAX_VALUE} 之间的整数，或提供的默认值
     * @throws CancelledInputException 输入为空且未提供默认值
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public int readQuantity(String prompt, Integer defaultValue) {
        while (true) {
            String text = readLine(prompt, defaultValue == null).strip();
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return BookValidator.validateQuantity(Integer.parseInt(text));
            } catch (NumberFormatException exception) {
                println("请输入 0 到 " + Integer.MAX_VALUE + " 之间的整数。");
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }

    /**
     * 读取价格，去除首尾空白后按 {@link BookValidator#validateAndNormalizePrice(BigDecimal)} 校验。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param defaultValue 空输入时返回的默认值，可为 {@code null}
     * @return 校验后保留两位小数的价格，或提供的默认值
     * @throws CancelledInputException 输入为空且未提供默认值
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public BigDecimal readPrice(String prompt, BigDecimal defaultValue) {
        while (true) {
            String text = readLine(prompt, defaultValue == null).strip();
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return BookValidator.validateAndNormalizePrice(new BigDecimal(text));
            } catch (NumberFormatException exception) {
                println("请输入有效金额。");
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }

    /**
     * 读取书名或作者，去除首尾空白后校验，长度须为 1 到 200 个 Unicode 码点。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param label 错误提示中使用的字段名称，例如“书名”
     * @param defaultValue 空输入时返回的默认值，可为 {@code null}
     * @return 去除首尾空白并通过校验的文本，或提供的默认值
     * @throws CancelledInputException 输入为空且未提供默认值
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public String readText(String prompt, String label, String defaultValue) {
        while (true) {
            String text = readLine(prompt, defaultValue == null).strip();
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return BookValidator.validateAndNormalizeText(text, label);
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }

    /**
     * 读取一次确认输入，只有去除首尾空白后为 y 或 Y 才表示确认。
     *
     * @param prompt 确认提示，方法会在其后补充输入说明
     * @return 确认时返回 {@code true}；其他输入均返回 {@code false}
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public boolean confirm(String prompt) {
        return readLine(prompt + "（输入 y 确认，其他输入取消）：", false).strip().equalsIgnoreCase("y");
    }

    /**
     * 表示用户通过空输入取消当前操作，由控制台界面返回对应菜单。
     */
    static final class CancelledInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CancelledInputException() {
        }
    }

    /**
     * 表示标准输入已结束，用于退出当前输入流程和外层菜单循环。
     */
    static final class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        EndOfInputException() {
        }
    }
}
