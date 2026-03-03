package ru.yandex.practicum.blog.model;

public record Image(
        Long postId,
        byte[] data
) {}
