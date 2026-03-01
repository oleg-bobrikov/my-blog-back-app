package ru.yandex.practicum.blog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.repository.PostRepository;

import java.util.List;

@Service
public class PostService {
    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    public Post save(Post post) {
        return repository.save(post);
    }

    public void update(Post post) {
        repository.update(post);
    }

    public Post findById(Long id) {
        return repository.findById(id);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public Page<Post> findPosts(String search, Pageable pageable) {
        List<Post> posts = repository.findPosts(search, pageable.getPageNumber(), pageable.getPageSize());
        return new org.springframework.data.domain.PageImpl<>(posts, pageable, posts.size());
    }
}
