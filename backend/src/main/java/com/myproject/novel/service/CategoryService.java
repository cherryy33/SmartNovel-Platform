package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.dao.entity.BookCategory;
import com.myproject.novel.dao.mapper.BookCategoryMapper;
import com.myproject.novel.dto.resp.CategoryRespDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final BookCategoryMapper bookCategoryMapper;

    public List<CategoryRespDto> listAll() {
        List<BookCategory> categories = bookCategoryMapper.selectList(
            new LambdaQueryWrapper<BookCategory>().orderByAsc(BookCategory::getSort));
        return categories.stream().map(c -> {
            CategoryRespDto dto = new CategoryRespDto();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setWorkDirection(c.getWorkDirection());
            dto.setSort(c.getSort());
            return dto;
        }).collect(Collectors.toList());
    }

    public List<CategoryRespDto> listByWorkDirection(Integer workDirection) {
        List<BookCategory> categories = bookCategoryMapper.selectList(
            new LambdaQueryWrapper<BookCategory>()
                .eq(workDirection != null, BookCategory::getWorkDirection, workDirection)
                .orderByAsc(BookCategory::getSort));
        return categories.stream().map(c -> {
            CategoryRespDto dto = new CategoryRespDto();
            dto.setId(c.getId()); dto.setName(c.getName());
            dto.setWorkDirection(c.getWorkDirection()); dto.setSort(c.getSort());
            return dto;
        }).collect(Collectors.toList());
    }
}
