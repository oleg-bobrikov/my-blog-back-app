package ru.yandex.practicum.blog.model;

public record Image(
        Long id,
        Long postId,
        byte[] data
) {}
