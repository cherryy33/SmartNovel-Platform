package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.dao.entity.Tag;
import com.myproject.novel.dao.mapper.TagMapper;
import com.myproject.novel.dto.resp.TagRespDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<TagRespDto> listAll() {
        List<Tag> tags = tagMapper.selectList(
            new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getSort));
        return tags.stream().map(t -> {
            TagRespDto dto = new TagRespDto();
            dto.setId(t.getId()); dto.setName(t.getName()); dto.setSort(t.getSort());
            return dto;
        }).collect(Collectors.toList());
    }
}
