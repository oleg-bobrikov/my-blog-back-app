package ru.yandex.practicum.blog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PostServiceIntegrationTest {

    @Autowired
    private PostService service;

    @Test
    void shouldTruncateLongTextOnSave() {
        String longText = "a".repeat(150);
        Post post = Post.builder().title("Title").text(longText).build();

        Post saved = service.save(post);

        String expectedText = "a".repeat(128) + "...";
        assertEquals(expectedText, saved.text());

        Optional<Post> found = service.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(expectedText, found.get().text());
    }

    @Test
    void shouldNotTruncateShortTextOnSave() {
        String shortText = "Short text";
        Post post = Post.builder().title("Title").text(shortText).build();

        Post saved = service.save(post);

        assertEquals(shortText, saved.text());
    }

    @Test
    void shouldUpdatePost() {
        Post initial = service.save(Post.builder().title("Initial").text("Initial Text").tags(List.of("tag1")).build());

        Post updateRequest = Post.builder()
                .id(initial.id())
                .title("Updated Title")
                .text("Updated Text")
                .tags(List.of("tag1", "tag2"))
                .build();

        Post updated = service.update(updateRequest);

        assertEquals("Updated Title", updated.title());
        assertEquals("Updated Text", updated.text());
        assertEquals(List.of("tag1", "tag2"), updated.tags());

        Optional<Post> found = service.findById(initial.id());
        assertTrue(found.isPresent());
        assertEquals("Updated Title", found.get().title());
        assertEquals("Updated Text", found.get().text());
        assertEquals(List.of("tag1", "tag2"), found.get().tags());
    }

    @Test
    void shouldTruncateLongTextOnUpdate() {
        Post initial = service.save(Post.builder().title("Title").text("Short").build());

        String longText = "b".repeat(200);
        Post update = Post.builder().id(initial.id()).title("Title").text(longText).build();

        Post updated = service.update(update);

        String expectedText = "b".repeat(128) + "...";
        assertEquals(expectedText, updated.text());
    }

    @Test
    void shouldManagePostLifeCycle() {
        Post post = service.save(Post.builder().title("Title").text("Text").build());
        assertTrue(service.findById(post.id()).isPresent());

        service.deleteById(post.id());
        assertFalse(service.findById(post.id()).isPresent());
    }

    @Test
    void shouldFindPostsWithPagingAndSearch() {
        service.save(Post.builder().title("Sport").text("Text").tags(List.of("Yandex")).build());
        service.save(Post.builder().title("Education").text("Text").tags(List.of("Yandex", "Spring")).build());

        Page<Post> yandexPosts = service.findPosts("#Yandex", PageRequest.of(0, 10));
        assertEquals(2, yandexPosts.getTotalElements());

        Page<Post> springPosts = service.findPosts("#Spring", PageRequest.of(0, 10));
        assertEquals(1, springPosts.getTotalElements());

        Page<Post> yandexSportPosts = service.findPosts("#Yandex Sport", PageRequest.of(0, 10));
        assertEquals(1, yandexSportPosts.getTotalElements());

        Page<Post> anotherTagPosts = service.findPosts("#AnotherTag", PageRequest.of(0, 10));
        assertEquals(0, anotherTagPosts.getTotalElements());
    }

    @Test
    void shouldManageComments() {
        Post post = service.save(Post.builder().title("Title").text("Text").build());
        Long postId = post.id();

        String commentText = "Test comment";
        ru.yandex.practicum.blog.model.Comment comment = ru.yandex.practicum.blog.model.Comment.builder().text(commentText).build();

        // Create comment
        Optional<ru.yandex.practicum.blog.model.Comment> commentResult = service.comment(postId, comment);
        org.junit.jupiter.api.Assertions.assertTrue(commentResult.isPresent());
        Long commentId = commentResult.get().id();
        org.junit.jupiter.api.Assertions.assertNotNull(commentId);
        assertEquals(commentText, commentResult.get().text());

        // Get comments list
        List<ru.yandex.practicum.blog.model.Comment> comments = service.findCommentsByPostId(postId);
        assertEquals(1, comments.size());
        assertEquals(commentId, comments.getFirst().id());

        // Get comment by id
        ru.yandex.practicum.blog.model.Comment searchComment = ru.yandex.practicum.blog.model.Comment.builder().id(commentId).postId(postId).build();
        Optional<ru.yandex.practicum.blog.model.Comment> foundResult = service.findComment(searchComment);
        org.junit.jupiter.api.Assertions.assertTrue(foundResult.isPresent());
        assertEquals(commentText, foundResult.get().text());

        // Update comment
        String updatedText = "Updated comment text";
        ru.yandex.practicum.blog.model.Comment updateComment = ru.yandex.practicum.blog.model.Comment.builder().id(commentId).postId(postId).text(updatedText).build();
        Optional<ru.yandex.practicum.blog.model.Comment> updateResult = service.updateComment(updateComment);
        org.junit.jupiter.api.Assertions.assertTrue(updateResult.isPresent());
        assertEquals(updatedText, updateResult.get().text());

        // Delete comment
        service.deleteComment(updateComment);
        Optional<ru.yandex.practicum.blog.model.Comment> afterDelete = service.findComment(updateComment);
        org.junit.jupiter.api.Assertions.assertFalse(afterDelete.isPresent());
    }

    @Test
    void shouldHandleLikes() {
        Post post = service.save(Post.builder().title("Title").text("Text").build());
        assertEquals(0, post.likesCount());

        Post liked = service.like(post.id());
        assertEquals(1, liked.likesCount());

        Optional<Post> found = service.findById(post.id());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().likesCount());
    }

    @Test
    void shouldUploadAndGetImage() {
        Post post = service.save(Post.builder().title("Title").text("Text").build());
        byte[] imageData = new byte[]{1, 2, 3, 4};
        ru.yandex.practicum.blog.model.Image image = new ru.yandex.practicum.blog.model.Image(post.id(), imageData);

        service.uploadImage(image);

        Optional<ru.yandex.practicum.blog.model.Image> foundImage = service.findImageByPostId(post.id());
        assertTrue(foundImage.isPresent());
        assertArrayEquals(imageData, foundImage.get().data());
    }
}
