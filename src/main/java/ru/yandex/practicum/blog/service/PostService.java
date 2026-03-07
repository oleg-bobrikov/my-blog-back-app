package ru.yandex.practicum.blog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Image;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.repository.PostRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    public Post save(Post post) {
        Post truncatedPost = truncatePostText(post);
        return repository.save(truncatedPost);
    }

    public Post update(Post post) {
        Post truncatedPost = truncatePostText(post);
        return repository.update(truncatedPost);
    }

    private Post truncatePostText(Post post) {
        if (post.text() == null || post.text().length() <= 128) {
            return post;
        }

        String truncatedText = post.text().substring(0, 128) + "...";
        return Post.builder()
                .id(post.id())
                .title(post.title())
                .text(truncatedText)
                .tags(post.tags())
                .likesCount(post.likesCount())
                .commentsCount(post.commentsCount())
                .build();
    }

    public Post findById(Long id) {
        return repository.findById(id);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public Page<Post> findPosts(String search, Pageable pageable) {
        return repository.findPosts(search, pageable);
    }

    public Post like(Long postId) {
        return repository.like(postId);
    }

    public void uploadImage(Image image) {
        repository.uploadImage(image);
    }

    public Optional<Image> findImageByPostId(Long postId) {
        return repository.findImageByPostId(postId);
    }

    public Optional<Comment> comment(Long postId, Comment comment) {
        return repository.comment(postId, comment);
    }

    public List<Comment> findCommentsByPostId(Long postId) {
        return repository.findCommentsByPostId(postId);
    }

    public Optional<Comment> findComment(Comment comment) {
        return repository.findComment(comment);
    }

    public Optional<Comment> updateComment(Comment comment) {
        return repository.updateComment(comment);
    }

    public void deleteComment(Comment comment) {
        repository.deleteComment(comment);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
