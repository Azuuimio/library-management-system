package com.example.library.model.view;

import com.example.library.model.Book;

/**
 * 供界面展示的图书信息，将图书数据与未归还数量组合在一起。
 *
 * @param book 图书数据
 * @param unreturnedQuantity 该图书当前未归还的数量
 */
public record BookView(Book book, long unreturnedQuantity) {
    /**
     * 用总数量减去未归还数量，计算可借数量。
     *
     * @return 两者的差值；此方法不校验数量，也不将负数改为零
     */
    public long availableQuantity() {
        return book.totalQuantity() - unreturnedQuantity;
    }
}
