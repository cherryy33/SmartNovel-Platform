package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 小说-标签关联
 */
@Data
@TableName("book_tag")
public class BookTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;
    private Long tagId;
}
