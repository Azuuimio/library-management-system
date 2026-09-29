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
 * <p>编号、数量、价格和文本输入会在校验失败后重新读取。
 * 输入流结束时抛出 {@link EndOfInputException}，由控制台界面统一结束交互。
 */
public class ConsoleIo {
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
     * @return 读取到的文字；用户直接回车时返回空字符串
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public String readLine(String prompt) {
        output.print(prompt);
        output.flush();
        try {
            String line = input.readLine();
            if (line == null) {
                throw new EndOfInputException();
            }
            return line;
        } catch (IOException exception) {
            throw new UncheckedIOException("读取控制台输入失败", exception);
        }
    }

    /**
     * 读取正整数编号，输入无效时显示提示并重新读取。
     *
     * @param prompt 读取输入前显示的提示文字
     * @return 去除首尾空白后解析得到的编号，范围为 1 到 {@link Long#MAX_VALUE}
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public long readId(String prompt) {
        while (true) {
            try {
                long id = Long.parseLong(readLine(prompt).strip());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
            }
            println("请输入有效的正整数编号。");
        }
    }

    /**
     * 提示用户输入总数量，输入无效时显示原因并重新读取。
     *
     * <p>输入会先去除首尾空白。
     * 输入为空且提供了默认值时，直接返回默认值，不再校验。
     * 其他输入必须能解析为 0 到 {@link Integer#MAX_VALUE} 之间的整数。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param defaultValue 空输入时返回的默认值；为 {@code null} 时，空输入也需要重新输入
     * @return 用户输入的非负整数，或提供的默认值
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public int readQuantity(String prompt, Integer defaultValue) {
        while (true) {
            String text = readLine(prompt).strip();
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
     * 读取价格，输入无法解析或未通过价格校验时显示原因并重新读取。
     *
     * <p>输入会先去除首尾空白。输入为空且提供了默认值时，直接返回默认值，不再校验或补齐小数位。
     * 其他输入按 {@link BookValidator#validateAndNormalizePrice(BigDecimal)} 校验并补齐两位小数。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param defaultValue 空输入时返回的默认值；为 {@code null} 时，空输入也需要重新输入
     * @return 校验后保留两位小数的价格，或提供的默认值
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public BigDecimal readPrice(String prompt, BigDecimal defaultValue) {
        while (true) {
            String text = readLine(prompt).strip();
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
     * 读取书名或作者，去除首尾空白后校验，校验失败时显示原因并重新读取。
     *
     * <p>输入为空且提供了默认值时，直接返回默认值，不再校验或去除默认值的首尾空白。
     * 其他输入须为 1 到 200 个 Unicode 码点。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param label 错误提示中使用的字段名称，例如“书名”
     * @param defaultValue 空输入时返回的默认值；为 {@code null} 时，空输入也需要重新输入
     * @return 去除首尾空白并通过校验的文本，或提供的默认值
     * @throws EndOfInputException 输入流已结束，无法继续读取
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public String readText(String prompt, String label, String defaultValue) {
        while (true) {
            String text = readLine(prompt).strip();
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
        return readLine(prompt + "（输入 y 确认，其他输入取消）：").strip().equalsIgnoreCase("y");
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
