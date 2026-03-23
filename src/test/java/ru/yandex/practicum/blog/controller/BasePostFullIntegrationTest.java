package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.jdbc.JdbcTestUtils;
import ru.yandex.practicum.blog.dto.PageResponse;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BasePostFullIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "comments", "post_tags", "images", "posts");
    }

    protected static final String TITLE_DEFAULT = "Test Title";
    protected static final String TEXT_DEFAULT = "Test Text";
    protected static final String TAG_DEFAULT = "Tag";

    protected Post createPost(Post post) {
        Post created = webTestClient.post()
                .uri("/api/posts")
                .bodyValue(post)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Post.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(created);
        return created;
    }

    protected Post getPost(Long id) {
        return webTestClient.get()
                .uri("/api/posts/" + id)
                .exchange()
                .expectBody(Post.class)
                .returnResult()
                .getResponseBody();
    }

    protected ResponseEntity<Post> getPostResponse(Long id) {
        var result = webTestClient.get()
                .uri("/api/posts/" + id)
                .exchange()
                .expectBody(Post.class)
                .returnResult();
        return new ResponseEntity<>(result.getResponseBody(), result.getStatus());
    }

    protected void deletePost(Long id) {
        webTestClient.delete()
                .uri("/api/posts/" + id)
                .exchange();
    }

    protected Post likePost(Long id) {
        return webTestClient.post()
                .uri("/api/posts/" + id + "/likes")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Post.class)
                .returnResult()
                .getResponseBody();
    }

    protected Comment createComment(Long postId, Comment comment) {
        return webTestClient.post()
                .uri("/api/posts/" + postId + "/comments")
                .bodyValue(comment)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Comment.class)
                .returnResult()
                .getResponseBody();
    }

    protected List<Comment> getComments(Long postId) {
        return webTestClient.get()
                .uri("/api/posts/" + postId + "/comments")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Comment>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    protected Comment getComment(Long postId, Long commentId) {
        return webTestClient.get()
                .uri("/api/posts/" + postId + "/comments/" + commentId)
                .exchange()
                .expectBody(Comment.class)
                .returnResult()
                .getResponseBody();
    }

    protected ResponseEntity<Comment> getCommentResponse(Long postId, Long commentId) {
        var result = webTestClient.get()
                .uri("/api/posts/" + postId + "/comments/" + commentId)
                .exchange()
                .expectBody(Comment.class)
                .returnResult();
        return new ResponseEntity<>(result.getResponseBody(), result.getStatus());
    }

    protected void deleteComment(Long postId, Long commentId) {
        webTestClient.delete()
                .uri("/api/posts/" + postId + "/comments/" + commentId)
                .exchange();
    }

    protected PageResponse<Post> searchPosts(String search, int pageNumber, int pageSize) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/posts")
                        .queryParam("search", search)
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<Post>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    protected void uploadImage(Long id, byte[] imageData) {
        webTestClient.put()
                .uri("/api/posts/" + id + "/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(createMultipartBody(imageData))
                .exchange()
                .expectStatus().isOk();
    }

    private MultiValueMap<String, Object> createMultipartBody(byte[] imageData) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(imageData, createImageHeaders());
        body.add("image", fileEntity);
        return body;
    }

    private HttpHeaders createImageHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentDisposition(ContentDisposition.formData().name("image").filename("image.jpg").build());
        return headers;
    }

    protected ResponseEntity<byte[]> getImage(Long id) {
        var result = webTestClient.get()
                .uri("/api/posts/" + id + "/image")
                .exchange()
                .expectBody(byte[].class)
                .returnResult();
        return new ResponseEntity<>(result.getResponseBody(), result.getResponseHeaders(), result.getStatus());
    }
}
