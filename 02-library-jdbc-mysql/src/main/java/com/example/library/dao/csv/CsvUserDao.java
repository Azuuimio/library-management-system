package com.example.library.dao.csv;

import com.example.library.dao.UserDao;
import com.example.library.model.Role;
import com.example.library.model.User;

import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 从 users.csv 加载预制账号，供登录和借阅记录展示查询。
 *
 * <p>文件只在创建对象时读取，之后的查询使用内存中的数据。
 */
public final class CsvUserDao implements UserDao {
    static final List<String> HEADER = List.of("id", "username", "role");
    final NavigableMap<Long, User> users = new TreeMap<>();

    /**
     * 从指定目录读取 users.csv，按用户编号保存到内存中。
     *
     * @param directory 数据目录，其中的 users.csv 必须已存在
     * @throws com.example.library.exception.StorageException 读取失败、CSV 格式或字段转换失败，
     *                                                       或用户编号不是正数或重复
     */
    public CsvUserDao(Path directory) {
        Path file = directory.toAbsolutePath().normalize().resolve("users.csv");
        List<List<String>> rows = CsvFileIo.read(file, HEADER);
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                long id = Long.parseLong(row.get(0));
                addLoadedUser(new User(id, row.get(1), Role.valueOf(row.get(2))));
            } catch (RuntimeException exception) {
                throw CsvFileIo.invalidRowException(file, i, exception);
            }
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.username().equals(username)).findFirst();
    }

    private void addLoadedUser(User value) {
        if (value.id() <= 0 || users.putIfAbsent(value.id(), value) != null) {
            throw new IllegalArgumentException("编号必须为不重复的正整数");
        }
    }
}
