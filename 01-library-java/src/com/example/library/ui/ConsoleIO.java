package com.example.library.ui;

import com.example.library.exception.BusinessException;
import com.example.library.validation.BookValidator;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class ConsoleIO {
    private final BufferedReader input;
    private final PrintWriter output;

    public ConsoleIO() {
        input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
    }

    public void println(String text) {
        output.println(text);
    }

    public String line(String prompt) {
        output.print(prompt);
        output.flush();
        try {
            String line = input.readLine();
            if (line == null) {
                throw new EndOfInput();
            }
            return line;
        } catch (IOException exception) {
            throw new UncheckedIOException("读取控制台输入失败", exception);
        }
    }

    public long id(String prompt) {
        while (true) {
            try {
                long id = Long.parseLong(line(prompt).strip());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {}
            println("请输入有效的正整数编号。");
        }
    }

    public int quantity(String prompt, Integer defaultValue) {
        while (true) {
            String text = line(prompt).strip();
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return BookValidator.quantity(Integer.parseInt(text));
            } catch (NumberFormatException | BusinessException exception) {
                println("请输入 0 到 " + Integer.MAX_VALUE + " 之间的整数。");
            }
        }
    }

    public BigDecimal price(String prompt, BigDecimal defaultValue) {
        while (true) {
            String text = line(prompt).strip();
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return BookValidator.price(new BigDecimal(text));
            } catch (NumberFormatException exception) {
                println("请输入有效金额。");
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }

    public String text(String prompt, String defaultValue) {
        while (true) {
            String text = line(prompt).strip();
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return BookValidator.text(text, "内容");
            } catch (BusinessException exception) {
                println(exception.getMessage());
            }
        }
    }


    public boolean confirm(String prompt) {
        return line(prompt + "（输入 y 确认，其他输入取消）：").strip().equalsIgnoreCase("y");
    }

    static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
        EndOfInput() {}
    }
}
