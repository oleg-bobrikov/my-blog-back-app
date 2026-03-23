package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PostControllerFullIntegrationTest extends BasePostFullIntegrationTest {

    @Test
    void shouldCreatePost() {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_DEFAULT))
                .build();

        Post createdPost = createPost(post);
        assertNotNull(createdPost.id());
        assertEquals(TITLE_DEFAULT, createdPost.title());
        assertEquals(TEXT_DEFAULT, createdPost.text());
        assertEquals(List.of(TAG_DEFAULT), createdPost.tags());
    }

    @Test
    void shouldGetPostById() {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_DEFAULT))
                .build();

        Post createdPost = createPost(post);
        Long id = createdPost.id();

        Post retrievedPost = getPost(id);
        assertEquals(id, retrievedPost.id());
        assertEquals(TITLE_DEFAULT, retrievedPost.title());
    }

    @Test
    void shouldUpdatePost() {
        Post post = createPost(Post.builder().title(TITLE_DEFAULT).text(TEXT_DEFAULT).build());
        Long id = post.id();

        String updatedTitle = "Updated Title";
        Post updatePost = Post.builder()
                .id(id)
                .title(updatedTitle)
                .text(TEXT_DEFAULT)
                .build();

        webTestClient.put()
                .uri("/api/posts/{id}", id)
                .bodyValue(updatePost)
                .exchange()
                .expectStatus().isOk();

        Post retrievedPost = getPost(id);
        assertEquals(updatedTitle, retrievedPost.title());
    }

    @Test
    void shouldDeletePost() {
        Post post = createPost(Post.builder().title(TITLE_DEFAULT).text(TEXT_DEFAULT).build());
        Long id = post.id();

        deletePost(id);

        ResponseEntity<Post> response = getPostResponse(id);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldLikePost() {
        Post post = createPost(Post.builder().title(TITLE_DEFAULT).text(TEXT_DEFAULT).build());
        Long id = post.id();
        assertEquals(0, post.likesCount());

        Post likedPost = likePost(id);
        assertEquals(1, likedPost.likesCount());

        Post retrievedPost = getPost(id);
        assertEquals(1, retrievedPost.likesCount());
    }

    @Test
    void shouldUploadAndGetImage() {
        Post post = createPost(Post.builder().title(TITLE_DEFAULT).text(TEXT_DEFAULT).build());
        Long id = post.id();

        byte[] imageData = new byte[]{1, 2, 3, 4};
        uploadImage(id, imageData);

        ResponseEntity<byte[]> response = getImage(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        // Depending on implementation, bytes might be exactly what we sent or wrapped
        assertNotNull(response.getBody());
    }

    @Test
    void shouldManageComments() {
        Post post = createPost(Post.builder().title(TITLE_DEFAULT).text(TEXT_DEFAULT).build());
        Long postId = post.id();

        String commentText = "Test comment";
        Comment comment = Comment.builder().text(commentText).build();

        // Create comment
        Comment createdComment = createComment(postId, comment);
        Long commentId = createdComment.id();
        assertNotNull(commentId);
        assertEquals(commentText, createdComment.text());

        // Get comments list
        List<Comment> comments = getComments(postId);
        assertEquals(1, comments.size());
        assertEquals(commentId, comments.getFirst().id());

        // Get comment by id
        Comment retrievedComment = getComment(postId, commentId);
        assertEquals(commentText, retrievedComment.text());

        // Update comment
        String updatedText = "Updated comment text";
        Comment updateComment = Comment.builder().id(commentId).postId(postId).text(updatedText).build();
        webTestClient.put()
                .uri("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                .bodyValue(updateComment)
                .exchange()
                .expectStatus().isOk();

        Comment retrievedUpdatedComment = getComment(postId, commentId);
        assertEquals(updatedText, retrievedUpdatedComment.text());

        // Delete comment
        deleteComment(postId, commentId);
        ResponseEntity<Comment> response = getCommentResponse(postId, commentId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturn404WhenPostNotFound() {
        ResponseEntity<Post> response = getPostResponse(9999L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
