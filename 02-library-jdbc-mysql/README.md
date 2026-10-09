# 02-library-jdbc-mysql

## 技术栈

- Java SE + JDBC + MySQL
- 使用 Maven 管理依赖与构建

## 功能说明

**已完成功能**

- 实现基础的图书管理业务功能。
- 使用 JDBC 访问 MySQL 实现数据持久化。
- 支持并发读写。

**本阶段限制**

- 仅提供控制台交互界面。
- 使用无密码预制账号，暂未提供注册及面向用户的账号管理功能。

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
- Maven 3.10.0
- MySQL 8.4

## 快速开始

### 初始化数据库

启动 MySQL 服务，在 `02-library-jdbc-mysql` 目录下使用 PowerShell 连接数据库：

```powershell
mysql -u root -p
```

按提示输入 MySQL 密码，然后在同一个 MySQL 会话中依次执行：

```sql
SOURCE sql/schema.sql;
SOURCE sql/data.sql;
```

`schema.sql` 创建并选中 `library_jdbc` 数据库，建立账号、图书和借阅记录表；`data.sql` 初始化三个预制账号。

### 配置数据库连接

| 环境变量 | 值 |
| -------- | --- |
| LIBRARY_DB_URL | jdbc:mysql://localhost:3306/library_jdbc |
| LIBRARY_DB_USER | MySQL 账号 |
| LIBRARY_DB_PASSWORD | MySQL 密码 |

### 编译与启动

**方式一：IDEA 启动**

1. 使用 IDEA 打开 `02-library-jdbc-mysql` 目录，将 `pom.xml` 导入为 Maven 工程并加载依赖。
2. 为 `com.example.library.LibraryApplication` 创建 Application 运行配置，在该配置的环境变量中填写上述三个变量。
3. 打开 `src/main/java/com/example/library/LibraryApplication.java`，运行 `main` 方法。

**方式二：命令行启动**

在 `02-library-jdbc-mysql` 目录下使用 PowerShell 执行以下命令。

1. 设置当前 PowerShell 会话的编码与数据库连接参数（每次打开新终端后都需要执行，账号和密码替换为实际值）：

   ```powershell
   $utf8 = [System.Text.UTF8Encoding]::new($false)
   [Console]::InputEncoding = $utf8
   [Console]::OutputEncoding = $utf8
   $env:LIBRARY_DB_URL = 'jdbc:mysql://localhost:3306/library_jdbc'
   $env:LIBRARY_DB_USER = '你的 MySQL 账号'
   $env:LIBRARY_DB_PASSWORD = '你的 MySQL 密码'
   ```
   
2. 编译：

   ```powershell
   mvn compile dependency:copy-dependencies
   ```

3. 启动：

   ```powershell
   java -cp "target/classes;target/dependency/*" com.example.library.LibraryApplication
   ```

## 项目结构

```text
02-library-jdbc-mysql/
├── README.md                           # 项目说明与运行指南
├── pom.xml                             # Maven 依赖与构建配置
├── sql/
│   ├── schema.sql                      # 创建数据库与表结构
│   └── data.sql                        # 初始化预制账号
└── src/main/java/com/example/library/
    ├── LibraryApplication.java         # 程序入口、读取连接配置与对象组装
    ├── model/                          # 领域模型
    │   └── view/                       # 展示所需的组合数据
    ├── dao/                            # 数据访问接口
    │   └── jdbc/                       # JDBC 存储实现
    ├── db/                             # 数据库连接创建与事务管理
    ├── service/                        # 业务逻辑
    ├── ui/                             # 控制台交互
    ├── validation/                     # 图书字段校验
    └── exception/                      # 业务与存储异常
```
