package ru.yandex.practicum.blog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Image;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);

    Post update(Post post);

    void deleteById(Long id);

    Post findById(Long id);

    Post like(Long postId);

    Page<Post> findPosts(String search, Pageable pageable);

    void uploadImage(Image image);

    Optional<Image> findImageByPostId(Long postId);

    Optional<Comment> comment(Long postId, Comment comment);

    Optional<Comment> findComment(Comment comment);

    Optional<Comment> updateComment(Comment comment);

    void deleteComment(Comment comment);

    List<Comment> findCommentsByPostId(Long postId);
}
