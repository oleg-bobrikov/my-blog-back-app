package ru.yandex.practicum.blog.model;

import lombok.Builder;

import java.util.List;

@Builder
public record Post(
        Long id,
        String title,
        String text,
        List<String> tags,
        int likesCount,
        int commentsCount
) {
    public Post {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}