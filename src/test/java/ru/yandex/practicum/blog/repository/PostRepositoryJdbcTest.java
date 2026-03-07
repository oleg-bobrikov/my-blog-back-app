package ru.yandex.practicum.blog.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.configuration.AppDataSourceConfiguration;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("classpath:application-test.properties")
@SpringJUnitConfig(classes = {AppDataSourceConfiguration.class, ru.yandex.practicum.blog.repository.impl.PostRepositoryJdbc.class})
@Transactional
public class PostRepositoryJdbcTest {

    @Autowired
    private PostRepository repository;

    @Test
    void shouldSaveAndFindPost() {
        Post post = Post.builder()
                .title("Title")
                .text("Text")
                .tags(List.of("tag1", "tag2"))
                .build();

        Post saved = repository.save(post);
        assertNotNull(saved.id());
        assertEquals("Title", saved.title());
        assertEquals(List.of("tag1", "tag2"), saved.tags());

        Optional<Post> found = repository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(saved.id(), found.get().id());
        assertEquals("Title", found.get().title());
    }

    @Test
    void shouldUpdatePost() {
        Post post = repository.save(Post.builder().title("Old").text("Old").build());
        Post updated = Post.builder()
                .id(post.id())
                .title("New")
                .text("New")
                .tags(List.of("newTag"))
                .build();

        repository.update(updated);
        Optional<Post> found = repository.findById(post.id());
        assertTrue(found.isPresent());
        assertEquals("New", found.get().title());
        assertEquals(List.of("newTag"), found.get().tags());
    }

    @Test
    void shouldDeletePost() {
        Post post = repository.save(Post.builder().title("To delete").text("Text").build());
        assertTrue(repository.findById(post.id()).isPresent());

        repository.deleteById(post.id());
        assertFalse(repository.findById(post.id()).isPresent());
    }

    @Test
    void shouldFindPostsWithPagingAndSearch() {
        repository.save(Post.builder().title("Sport").text("Text").tags(List.of("Yandex")).build());
        repository.save(Post.builder().title("Education").text("Text").tags(List.of("Yandex", "Spring")).build());

        Page<Post> yandexPosts = repository.findPosts("#Yandex", PageRequest.of(0, 10));
        assertEquals(2, yandexPosts.getTotalElements());

        Page<Post> springPosts = repository.findPosts("#Spring", PageRequest.of(0, 10));
        assertEquals(1, springPosts.getTotalElements());

        Page<Post> yandexSportPosts = repository.findPosts("#Yandex Sport", PageRequest.of(0, 10));
        assertEquals(1, yandexSportPosts.getTotalElements());

        Page<Post> anotherTagPosts = repository.findPosts("#AnotherTag", PageRequest.of(0, 10));
        assertEquals(0, anotherTagPosts.getTotalElements());
    }

    @Test
    void shouldManageComments() {
        Post post = repository.save(Post.builder().title("Title").text("Text").build());
        Long postId = post.id();

        String commentText = "Test comment";
        Comment comment = Comment.builder().text(commentText).build();

        // Create comment
        Optional<Comment> saved = repository.comment(postId, comment);
        assertTrue(saved.isPresent());
        Long commentId = saved.get().id();
        assertNotNull(commentId);
        assertEquals(commentText, saved.get().text());

        // Get comments list
        List<Comment> comments = repository.findCommentsByPostId(postId);
        assertEquals(1, comments.size());
        assertEquals(commentId, comments.getFirst().id());

        // Get comment by id
        Comment searchComment = Comment.builder().id(commentId).postId(postId).build();
        Optional<Comment> foundResult = repository.findComment(searchComment);
        assertTrue(foundResult.isPresent());
        assertEquals(commentText, foundResult.get().text());

        // Update comment
        String updatedText = "Updated comment text";
        Comment updateComment = Comment.builder().id(commentId).postId(postId).text(updatedText).build();
        Optional<Comment> updateResult = repository.updateComment(updateComment);
        assertTrue(updateResult.isPresent());
        assertEquals(updatedText, updateResult.get().text());

        // Delete comment
        repository.deleteComment(updateComment);
        Optional<Comment> afterDelete = repository.findComment(updateComment);
        assertFalse(afterDelete.isPresent());
    }

    @Test
    void shouldHandleLikes() {
        Post post = repository.save(Post.builder().title("Title").text("Text").build());
        assertEquals(0, post.likesCount());

        Post liked = repository.like(post.id());
        assertEquals(1, liked.likesCount());
    }
}
