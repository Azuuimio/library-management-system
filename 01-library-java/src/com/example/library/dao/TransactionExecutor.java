package com.example.library.dao;

import java.util.function.Supplier;

public interface TransactionExecutor {
    <T> T executor(Supplier<T> action);
    <T> T executorForBook(long bookId, Supplier<T> action);
}
