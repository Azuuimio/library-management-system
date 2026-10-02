# 01-library-java

## 技术栈

Java SE

## 运行示例

<img width="867" height="464" alt="屏幕截图 2026-10-02 114534" src="https://github.com/user-attachments/assets/8ff11bf8-8a43-4001-8af7-bdcdf05cb4e2" />

## 功能与边界

### 角色功能

- 管理员：添加、修改、删除图书；按书名查询、查看全部图书；查看全部借阅记录。
- 读者：按书名查询、查看全部图书；借阅、归还图书；查看本人借阅记录。

### 当前限制

- 仅支持无密码预制账号，不提供注册和账号管理。
- 不支持并发读写。

## 快速开始

### 环境准备

- 开发使用 JDK 25，未验证最低兼容版本。

### 方式一：命令行启动

- 执行命令前确保 `java`、`javac` 已加入 `PATH`。
- 以下命令在 `01-library-java` 目录下使用 PowerShell 执行。

1. 设置当前 PowerShell 会话的编码（每次打开新终端后都需要执行）：

   ```powershell
   $utf8 = [System.Text.UTF8Encoding]::new($false)
   [Console]::InputEncoding = $utf8
   [Console]::OutputEncoding = $utf8
   ```

2. 编译：

   ```powershell
   $sources = (Get-ChildItem .\src -Recurse -Filter *.java).FullName
   javac -encoding UTF-8 -d out $sources
   ```

3. 启动：

   ```powershell
   java -cp out com.example.library.LibraryApplication
   ```

### 方式二：IntelliJ IDEA 启动

1. 使用 IDEA 打开 `01-library-java` 目录。
2. 打开 `src/com/example/library/LibraryApplication.java`，构建并运行。

### 登录预制账号

输入以下预制账号名称登录：

| 账号 | 角色 |
| :--- | :--- |
| admin | 管理员 |
| reader1 | 读者 |
| reader2 | 读者 |

## 项目架构

```text
com.example.library
├── LibraryApplication  # 程序入口、数据初始化与对象组装
├── model               # 领域模型
│   └── view            # 展示所需的组合数据
├── dao                 # 数据访问接口
│   └── csv             # CSV 存储实现
├── service             # 业务逻辑
├── ui                  # 控制台交互
├── validation          # 图书字段校验
└── exception           # 业务与存储异常
```
