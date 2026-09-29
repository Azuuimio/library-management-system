package com.example.library.model.view;

import com.example.library.model.BorrowRecord;

/**
 * 供界面展示的借阅记录，补充关联用户和图书的信息。
 *
 * <p>用户名、书名和删除标记来自查询时的数据，不是借阅发生时的快照。
 *
 * @param borrowRecord 借阅记录
 * @param username 借阅读者的账号名称
 * @param bookTitle 所借图书的当前书名
 * @param bookDeleted 所借图书当前是否已标记为删除
 */
public record BorrowRecordView(BorrowRecord borrowRecord, String username, String bookTitle, boolean bookDeleted) {
}
