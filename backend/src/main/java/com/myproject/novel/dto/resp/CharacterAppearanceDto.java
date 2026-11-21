package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 角色出场统计 响应 DTO
 */
@Data
public class CharacterAppearanceDto {

    /** 章节号 */
    private Integer chapterNum;

    /** 章节名 */
    private String chapterName;

    /** 该角色在本章出现的次数 */
    private Long appearCount;
}
