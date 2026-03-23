package ru.yandex.practicum.blog.model;

import lombok.Builder;

@Builder
public record Comment(
        Long id,
        Long postId,
        String text
) {}
