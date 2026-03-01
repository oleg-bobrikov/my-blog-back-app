package ru.yandex.practicum.blog.repository;

import ru.yandex.practicum.blog.model.Post;

import java.util.List;

public interface PostRepository {
    Post save(Post post);

    void update(Post post);

    void deleteById(Long id);

    Post findById(Long id);

    List<Post> findPosts(String search, int pageNumber, int pageSize);
}
