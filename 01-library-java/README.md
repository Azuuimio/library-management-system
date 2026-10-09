# 01-library-java

## 技术栈

Java SE

## 功能说明

**已完成功能**

- 实现基础的图书管理业务功能。
- 使用 CSV 文件实现数据持久化。

**本阶段限制**

- 仅提供控制台交互界面。
- 使用无密码预制账号，暂未提供注册及面向用户的账号管理功能。
- 不支持并发读写。

预制账号如下：

| 账号名称 | 角色   |
| -------- | ------ |
| admin    | 管理员 |
| reader1  | 读者   |
| reader2  | 读者   |

## 运行示例

<img width="867" height="464" alt="屏幕截图 2026-10-02 114534" src="https://github.com/user-attachments/assets/8ff11bf8-8a43-4001-8af7-bdcdf05cb4e2" />

## 开发环境

- JDK 25

## 快速开始

**方式一：IDEA 启动**

1. 使用 IDEA 打开 `01-library-java` 目录。
2. 打开 `src/com/example/library/LibraryApplication.java`，运行 `main` 方法。

**方式二：命令行启动**

在 `01-library-java` 目录下使用 PowerShell 执行以下命令。

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

## 项目结构

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
