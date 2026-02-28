package ru.yandex.practicum.blog.model;

import java.util.List;

public record Post(
        Long id,
        String title,
        String text,
        List<String> tags,
        Integer likesCount
) {}