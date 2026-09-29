# 01-library-java

纯 Java SE 控制台应用，使用 CSV 文件作为数据存储的图书管理系统。

## 运行示例



## 功能

### 管理员（ADMIN）

- 添加图书 / 删除图书 / 修改图书
- 查询图书 / 查看全部图书
- 查看全部借阅

### 读者（READER）

- 查询图书 / 查看全部图书
- 借阅图书 / 归还图书 / 查看我的借阅

## 运行方式

用 IntelliJ IDEA 打开本目录，构建并运行。也可以使用其他构建和运行方式。

## 预制账号

本阶段使用无密码预制账号，登录时只需输入账号名称。

| 账号名称 | 角色            |
| :------- | :-------------- |
| admin    | 管理员（ADMIN） |
| reader1  | 读者（READER）  |
| reader2  | 读者（READER）  |

## 项目架构

```
com.example.library
├── LibraryApplication                    # 程序入口
├── model                                 # 领域模型
│   ├── Book
│   ├── BorrowRecord
│   ├── Role
│   ├── User
│   └── view
│       ├── BookView
│       └── BorrowRecordView
├── dao                                   # 数据访问接口
│   ├── BookDao
│   ├── BorrowRecordDao
│   ├── UserDao
│   └── csv                               # CSV 实现
│       ├── CsvBookDao
│       ├── CsvBorrowRecordDao
│       ├── CsvUserDao
│       ├── CsvCodec
│       ├── CsvDataValidator
│       ├── CsvFileIo
│       └── CsvInitializer
├── service                               # 业务逻辑
│   ├── BookService
│   ├── BorrowRecordService
│   └── UserService
├── ui                                    # 控制台交互
│   ├── ConsoleIo
│   └── ConsoleUi
├── validation                            # 数据校验
│   └── BookValidator
└── exception                             # 异常
    ├── BusinessException
    └── StorageException
```

## 开发环境

| 项目 | 版本 |
| :--- | :--- |
| 操作系统 | Windows 11 |
| JDK | 25（LTS） |
| IDE | IntelliJ IDEA |
