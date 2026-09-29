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

/**
 * 以 UTF-8 读写 CSV 文件，并为读写错误补充文件名称。
 *
 * <p>读取时校验表头和列数；写入时先写同目录临时文件，再原子替换目标文件。
 */
final class CsvFileIO {
    private CsvFileIO() {
    }

    /**
     * 读取 CSV 文件，校验表头和每条数据记录的列数后返回数据记录。
     *
     * @param file 要读取的文件
     * @param header 预期表头，字段名称和顺序必须与文件第一条记录完全一致
     * @return 不包含表头的不可修改记录列表；文件只有表头时返回空列表
     * @throws StorageException 读取失败、CSV 引号格式错误、表头不匹配或数据记录列数不匹配
     */
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

    /**
     * 将全部记录写入同目录临时文件，强制写入存储设备后原子替换目标文件。
     *
     * <p>文件系统不支持原子移动时抛出异常，不改用普通覆盖写入。
     * 此操作只替换一个文件，不保证多个数据文件一起写入成功。
     *
     * @param target 目标文件路径，必须带有已存在的父目录
     * @param rows 包含表头的全部记录；列表、记录和字段均不能为 {@code null}
     * @throws StorageException 创建或写入临时文件失败、替换失败，或文件系统不支持原子移动
     */
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

    /**
     * 为数据记录转换失败的异常补充文件名和 CSV 记录编号。
     *
     * @param file 数据文件
     * @param index 去掉表头后从 0 开始的数据记录下标
     * @param cause 字段转换或编号检查时抛出的异常
     * @return 包含原始异常的数据异常；显示编号为下标加 2，以计入表头并从 1 开始编号
     */
    static StorageException invalidRowException(Path file, int index, RuntimeException cause) {
        return new StorageException(file.getFileName() + " 第 " + (index + 2)
                + " 条记录格式错误：" + cause.getMessage(), cause);
    }
}
