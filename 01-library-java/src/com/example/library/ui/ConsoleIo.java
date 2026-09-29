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

public class ConsoleIo {
    private final BufferedReader input;
    private final PrintWriter output;

    public ConsoleIo() {
        input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
    }

    public void println(String text) {
        output.println(text);
    }

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

    public boolean confirm(String prompt) {
        return readLine(prompt + "（输入 y 确认，其他输入取消）：").strip().equalsIgnoreCase("y");
    }

    static final class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        EndOfInputException() {
        }
    }
}
