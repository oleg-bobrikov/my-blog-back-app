package ru.yandex.practicum.blog.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record Comment(
        Long id,
        Long postId,
        String text
) {}
