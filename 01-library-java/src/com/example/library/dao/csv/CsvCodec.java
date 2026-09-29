package com.example.library.dao.csv;

import com.example.library.exception.StorageException;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * 在 CSV 文本和字符串表格之间转换，不负责文件读写或表头校验。
 *
 * <p>支持逗号分隔字段、双引号包围字段，以及用连续两个双引号表示字段中的一个双引号。
 * 引号内的换行属于字段内容。
 */
final class CsvCodec {
    private CsvCodec() {
    }

    /**
     * 读取全部 CSV 记录，保留字段内容中的空白。
     *
     * <p>引号外的 CR、LF 和 CRLF 均作为记录分隔符。
     * 错误信息中的记录编号从 1 开始，按 CSV 记录计数。
     * 引号内的换行不增加记录编号，因此记录编号不一定与文本行号相同。
     * 此方法不关闭传入的读取器。
     *
     * @param source CSV 文本读取器
     * @return 不可修改的记录列表，其中每条记录的字段列表也不可修改；空输入返回空列表
     * @throws IOException 读取字符失败
     * @throws StorageException 引号未闭合、未加引号的字段中出现双引号，或闭合引号后出现非分隔字符
     */
    static List<List<String>> decode(Reader source) throws IOException {
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

    /**
     * 将字符串表格编码为 CSV 文本，每条记录以 CRLF 结束。
     *
     * <p>包含逗号、双引号、回车或换行的字段会用双引号包围，字段内的双引号写成两个。
     *
     * @param rows 要编码的记录；列表、记录和字段均不能为 {@code null}，字段可以是空字符串
     * @return CSV 文本；没有记录时返回空字符串
     */
    static String encode(List<List<String>> rows) {
        StringBuilder result = new StringBuilder();
        for (List<String> row : rows) {
            for (int index = 0; index < row.size(); index++) {
                if (index > 0) {
                    result.append(',');
                }
                String value = row.get(index);
                boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                        || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
                if (quote) {
                    result.append('"');
                    result.append(value.replace("\"", "\"\""));
                    result.append('"');
                } else {
                    result.append(value);
                }
            }
            result.append("\r\n");
        }
        return result.toString();
    }
}
