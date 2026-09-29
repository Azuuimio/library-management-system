package com.example.library.dao.csv;

import com.example.library.exception.StorageException;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

final class CsvFileIO {
    private CsvFileIO() {
    }

    static List<List<String>> read(Path file, List<String> header) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<List<String>> rows = CsvCodec.decode(reader);
            if (rows.isEmpty() || !rows.getFirst().equals(header)) {
                throw new StorageException("表头错误，应为：" + String.join(",", header));
            }
            for (int index = 1; index < rows.size(); index++) {
                if (rows.get(index).size() != header.size()) {
                    throw new StorageException("第 " + (index + 1) + " 条记录列数不匹配");
                }
            }
            return rows.subList(1, rows.size());
        } catch (IOException | StorageException exception) {
            throw new StorageException("读取 " + file.getFileName() + " 失败：" + exception.getMessage(), exception);
        }
    }

    static void write(Path target, List<List<String>> rows) {
        Path temporary = null;
        try {
            byte[] bytes = CsvCodec.encode(rows).getBytes(StandardCharsets.UTF_8);
            temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new StorageException("当前文件系统不支持原子替换，请将数据目录放到本地支持原子移动的磁盘", exception);
        } catch (IOException exception) {
            throw new StorageException("写入 " + target.getFileName() + " 失败", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    static StorageException invalidRow(Path file, int index, RuntimeException cause) {
        return new StorageException(file.getFileName() + " 第 " + (index + 2)
                + " 条记录格式错误：" + cause.getMessage(), cause);
    }
}
