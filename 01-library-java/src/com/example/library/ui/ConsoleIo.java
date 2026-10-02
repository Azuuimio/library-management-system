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
 * <p>取消输入时返回 {@link InteractionStatus#CANCELLED}，由控制台界面返回对应菜单。
 * 输入流结束时返回 {@link InteractionStatus#END_OF_INPUT}，由控制台界面统一结束交互。
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
     * @return 完成时包含读取到的文字，允许空输入时可以是空字符串；
     *         启用空输入取消且输入为空时返回取消状态，输入流结束时返回结束状态
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public InputResult<String> readLine(String prompt, boolean cancelOnEmpty) {
        output.print(prompt);
        output.flush();
        try {
            String line = input.readLine();
            if (line == null) {
                return InputResult.stopped(InteractionStatus.END_OF_INPUT);
            }
            if (cancelOnEmpty && line.isBlank()) {
                return InputResult.stopped(InteractionStatus.CANCELLED);
            }
            return InputResult.completed(line);
        } catch (IOException exception) {
            throw new UncheckedIOException("读取控制台输入失败", exception);
        }
    }

    /**
     * 读取正整数编号。
     *
     * @param prompt 读取输入前显示的提示文字
     * @return 完成时包含去除首尾空白后解析得到的编号，范围为 1 到 {@link Long#MAX_VALUE}；
     *         空输入时返回取消状态，输入流结束时返回结束状态
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public InputResult<Long> readId(String prompt) {
        while (true) {
            InputResult<String> line = readLine(prompt, true);
            if (line.status() != InteractionStatus.COMPLETED) {
                return InputResult.stopped(line.status());
            }
            try {
                long id = Long.parseLong(line.value().strip());
                if (id > 0) {
                    return InputResult.completed(id);
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
     * @return 完成时包含 0 到 {@link Integer#MAX_VALUE} 之间的整数，或提供的默认值；
     *         空输入且未提供默认值时返回取消状态，输入流结束时返回结束状态
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public InputResult<Integer> readQuantity(String prompt, Integer defaultValue) {
        while (true) {
            InputResult<String> line = readLine(prompt, defaultValue == null);
            if (line.status() != InteractionStatus.COMPLETED) {
                return InputResult.stopped(line.status());
            }
            String text = line.value().strip();
            if (text.isEmpty() && defaultValue != null) {
                return InputResult.completed(defaultValue);
            }
            try {
                return InputResult.completed(BookValidator.validateQuantity(Integer.parseInt(text)));
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
     * @return 完成时包含校验后保留两位小数的价格，或提供的默认值；
     *         空输入且未提供默认值时返回取消状态，输入流结束时返回结束状态
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public InputResult<BigDecimal> readPrice(String prompt, BigDecimal defaultValue) {
        while (true) {
            InputResult<String> line = readLine(prompt, defaultValue == null);
            if (line.status() != InteractionStatus.COMPLETED) {
                return InputResult.stopped(line.status());
            }
            String text = line.value().strip();
            if (text.isEmpty() && defaultValue != null) {
                return InputResult.completed(defaultValue);
            }
            try {
                return InputResult.completed(BookValidator.validateAndNormalizePrice(new BigDecimal(text)));
            } catch (NumberFormatException exception) {
                println("请输入有效金额。");
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }

    /**
     * 读取书名或作者，去除首尾空白后按 {@link BookValidator#validateAndNormalizeText(String, String)} 校验。
     *
     * @param prompt 读取输入前显示的提示文字
     * @param label 错误提示中使用的字段名称，例如“书名”
     * @param defaultValue 空输入时返回的默认值，可为 {@code null}
     * @return 完成时包含去除首尾空白并通过校验的文本，或提供的默认值；
     *         空输入且未提供默认值时返回取消状态，输入流结束时返回结束状态
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public InputResult<String> readText(String prompt, String label, String defaultValue) {
        while (true) {
            InputResult<String> line = readLine(prompt, defaultValue == null);
            if (line.status() != InteractionStatus.COMPLETED) {
                return InputResult.stopped(line.status());
            }
            String text = line.value().strip();
            if (text.isEmpty() && defaultValue != null) {
                return InputResult.completed(defaultValue);
            }
            try {
                return InputResult.completed(BookValidator.validateAndNormalizeText(text, label));
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }

    /**
     * 读取一次确认输入，只有去除首尾空白后为 y 或 Y 才表示确认。
     *
     * @param prompt 确认提示，方法会在其后补充输入说明
     * @return 完成时包含确认结果：确认时为 {@code true}，其他输入（包括空输入）为 {@code false}；
     *         输入流结束时返回结束状态，不返回取消状态
     * @throws UncheckedIOException 读取控制台输入失败
     */
    public InputResult<Boolean> confirm(String prompt) {
        InputResult<String> line = readLine(prompt + "（输入 y 确认，其他输入取消）：", false);
        if (line.status() != InteractionStatus.COMPLETED) {
            return InputResult.stopped(line.status());
        }
        return InputResult.completed(line.value().strip().equalsIgnoreCase("y"));
    }

    /**
     * 表示一次输入或界面操作的处理状态。
     */
    public enum InteractionStatus {
        /** 输入已读取，或界面操作已处理完毕；界面操作可以包含已显示的业务失败提示。 */
        COMPLETED,
        /** 用户在允许取消的位置输入空白，本次输入已取消。 */
        CANCELLED,
        /** 输入流已结束，无法继续读取。 */
        END_OF_INPUT
    }

    /**
     * 保存输入状态和读取到的值。
     *
     * @param <T> 输入值的类型
     * @param status 输入状态，不能为 {@code null}
     * @param value 完成时为非 {@code null} 的输入值；取消或输入结束时为 {@code null}
     */
    public record InputResult<T>(InteractionStatus status, T value) {
        /**
         * 检查输入状态及其对应值。
         *
         * @throws IllegalArgumentException 状态为 {@code null}、完成时值为 {@code null}，
         *                                  或取消、输入结束时携带非 {@code null} 的值
         */
        public InputResult {
            if (status == null) {
                throw new IllegalArgumentException("输入状态不能为空");
            }
            if (status == InteractionStatus.COMPLETED && value == null) {
                throw new IllegalArgumentException("输入完成时必须提供值");
            }
            if (status != InteractionStatus.COMPLETED && value != null) {
                throw new IllegalArgumentException("取消或输入结束时不能提供值");
            }
        }

        /**
         * 创建包含输入值的完成结果。
         *
         * @param value 输入值，不能为 {@code null}
         * @param <T> 输入值的类型
         * @return 状态为 {@link InteractionStatus#COMPLETED} 的结果
         * @throws IllegalArgumentException 输入值为 {@code null}
         */
        public static <T> InputResult<T> completed(T value) {
            return new InputResult<>(InteractionStatus.COMPLETED, value);
        }

        /**
         * 创建不携带值的取消或输入结束结果。
         *
         * @param status 只能为 {@link InteractionStatus#CANCELLED} 或 {@link InteractionStatus#END_OF_INPUT}
         * @param <T> 输入值的类型
         * @return 指定状态且值为 {@code null} 的结果
         * @throws IllegalArgumentException 状态为 {@code null} 或 {@link InteractionStatus#COMPLETED}
         */
        public static <T> InputResult<T> stopped(InteractionStatus status) {
            return new InputResult<>(status, null);
        }
    }
}
