CREATE DATABASE IF NOT EXISTS library_jdbc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_bin;

USE library_jdbc;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE CHECK (CHAR_LENGTH(username) > 0),
    role VARCHAR(6) NOT NULL CHECK (role IN ('ADMIN', 'READER'))
) ENGINE=InnoDB;

CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL CHECK (CHAR_LENGTH(title) > 0),
    author VARCHAR(200) NOT NULL CHECK (CHAR_LENGTH(author) > 0),
    price DECIMAL(7, 2) NOT NULL CHECK (price >= 0),
    total_quantity INT NOT NULL CHECK (total_quantity >= 0),
    deleted TINYINT NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
) ENGINE=InnoDB;

CREATE TABLE borrow_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrowed_at DATETIME NOT NULL,
    returned_at DATETIME,

    INDEX idx_borrow_records_user (user_id),
    INDEX idx_borrow_records_book_returned (book_id, returned_at),

    CONSTRAINT fk_borrow_records_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_borrow_records_book
        FOREIGN KEY (book_id) REFERENCES books(id),

    CHECK (returned_at IS NULL OR returned_at >= borrowed_at)
) ENGINE=InnoDB;
















