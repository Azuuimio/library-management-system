package com.example.library.model;

import java.math.BigDecimal;

/**
 * 图书信息，包含总数量和删除标记。
 *
 * <p>可借数量由总数量减去未归还数量得到，不单独保存在图书中。
 * 此记录的构造器不校验字段，新增和修改时由业务层校验。
 *
 * @param id 图书编号
 * @param title 书名
 * @param author 作者
 * @param price 价格
 * @param totalQuantity 图书总数量，包括已借出但未归还的数量
 * @param deleted 是否已删除；为 {@code true} 时仍保留数据供历史借阅查询
 */
public record Book(long id, String title, String author, BigDecimal price,
                   int totalQuantity, boolean deleted) {
}
