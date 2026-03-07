package ru.yandex.practicum.blog.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> posts,
        boolean hasPrev,
        boolean hasNext,
        int lastPage
) {}
