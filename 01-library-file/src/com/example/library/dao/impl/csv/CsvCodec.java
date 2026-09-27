package com.example.library.dao.impl.csv;

import com.example.library.exception.StorageException;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class CsvCodec {
    private CsvCodec() {
    }

    public static List<List<String>> read(Reader source) throws IOException {
        PushbackReader reader = new PushbackReader(source, 1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        boolean started = false;
        int character;
        while ((character = reader.read()) != -1) {
            char current = (char) character;
            if (quoted) {
                if (current == '"') {
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        quoted = false;
                        closedQuote = true;
                        if (next != -1) {
                            reader.unread(next);
                        }
                    }
                } else {
                    field.append(current);
                }
                continue;
            }
            if (current == ',' || current == '\r' || current == '\n') {
                row.add(field.toString());
                field.setLength(0);
                closedQuote = false;
                started = false;
                if (current != ',') {
                    if (current == '\r') {
                        int next = reader.read();
                        if (next != '\n' && next != -1) {
                            reader.unread(next);
                        }
                    }
                    rows.add(List.copyOf(row));
                    row.clear();
                }
            } else if (closedQuote) {
                throw new StorageException("CSV 第 " + (rows.size() + 1) + " 条记录：闭合引号后有非法字符");
            } else if (current == '"') {
                if (started) {
                    throw new StorageException("CSV 第 " + (rows.size() + 1) + " 条记录：未转义的双引号");
                }
                quoted = true;
                started = true;
            } else {
                field.append(current);
                started = true;
            }
        }
        if (quoted) {
            throw new StorageException("CSV 最后一条记录的引号没有闭合");
        }
        if (started || closedQuote || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

    public static String encode(List<List<String>> rows) {
        StringWriter writer = new StringWriter();
        try {
            write(writer, rows);
        } catch (IOException exception) {
            throw new IllegalStateException("内存 CSV 编码失败", exception);
        }
        return writer.toString();
    }

    public static void write(Writer writer, List<List<String>> rows) throws IOException {
        for (List<String> row : rows) {
            for (int index = 0; index < row.size(); index++) {
                if (index > 0) {
                    writer.write(',');
                }
                String value = row.get(index);
                boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                        || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
                if (quote) {
                    writer.write('"');
                    writer.write(value.replace("\"", "\"\""));
                    writer.write('"');
                } else {
                    writer.write(value);
                }
            }
            writer.write("\r\n");
        }
    }
}