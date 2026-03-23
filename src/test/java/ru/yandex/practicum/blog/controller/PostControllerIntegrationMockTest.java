package ru.yandex.practicum.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Image;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.service.PostService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PostController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostControllerIntegrationMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService service;

    @BeforeEach
    void setUp() {
        when(service.save(any())).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            return Post.builder()
                    .id(p.id() == null ? 1L : p.id())
                    .title(p.title())
                    .text(p.text())
                    .tags(p.tags())
                    .likesCount(p.likesCount())
                    .commentsCount(p.commentsCount())
                    .build();
        });

        when(service.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.of(Post.builder()
                    .id(id)
                    .title(TITLE_DEFAULT)
                    .text(TEXT_DEFAULT)
                    .tags(List.of(TAG_DEFAULT))
                    .build());
        });

        when(service.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(service.like(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Post.builder()
                    .id(id)
                    .title(TITLE_DEFAULT)
                    .text(TEXT_DEFAULT)
                    .tags(List.of(TAG_DEFAULT))
                    .likesCount(1)
                    .build();
        });

        when(service.comment(any(), any()))
                .thenAnswer(invocation -> {
                    Comment c = invocation.getArgument(1);
                    return Optional.of(Comment.builder()
                            .id(1L)
                            .postId(invocation.getArgument(0))
                            .text(c.text())
                            .build());
                });

        when(service.findCommentsByPostId(any())).thenAnswer(invocation -> List.of(Comment.builder()
                .id(1L)
                .postId(invocation.getArgument(0))
                .text("Test comment")
                .build()));

        when(service.findComment(any()))
                .thenAnswer(invocation -> {
                    Comment c = invocation.getArgument(0);
                    if (c == null) return Optional.empty();
                    return Optional.of(Comment.builder()
                            .id(c.id() == null ? 1L : c.id())
                            .postId(c.postId())
                            .text("Test comment")
                            .build());
                });

        when(service.updateComment(any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        when(service.findImageByPostId(any()))
                .thenAnswer(invocation -> {
                    Long postId = invocation.getArgument(0);
                    return Optional.of(new Image(postId, new byte[]{1, 2, 3, 4}));
                });
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TITLE_DEFAULT = "Test Title";
    private static final String TEXT_DEFAULT = "Test Text";
    private static final String TAG_DEFAULT = "Tag";

    @Test
    void shouldCreatePost() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_DEFAULT))
                .build();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value(TITLE_DEFAULT))
                .andExpect(jsonPath("$.text").value(TEXT_DEFAULT))
                .andExpect(jsonPath("$.tags[0]").value(TAG_DEFAULT));
    }

    @Test
    void shouldGetPostById() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value(TITLE_DEFAULT))
                .andExpect(jsonPath("$.text").value(TEXT_DEFAULT))
                .andExpect(jsonPath("$.tags[0]").value(TAG_DEFAULT));
    }

    @Test
    void shouldUpdatePost() throws Exception {
        Long id = 1L;
        String updatedTitle = "Updated Title";
        Post updatePost = Post.builder()
                .id(id)
                .title(updatedTitle)
                .text(TEXT_DEFAULT)
                .build();

        mockMvc.perform(put("/api/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePost)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(updatedTitle));
    }

    @Test
    void shouldDeletePost() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/api/posts/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLikePost() throws Exception {
        Long id = 1L;

        mockMvc.perform(post("/api/posts/{id}/likes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void shouldUploadAndGetImage() throws Exception {
        Long id = 1L;
        byte[] imageData = new byte[]{1, 2, 3, 4};

        mockMvc.perform(multipart("/api/posts/{id}/image", id)
                        .file("image", imageData)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}/image", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(imageData));
    }

    @Test
    void shouldCreateComment() throws Exception {
        Long postId = 1L;
        String commentText = "Test comment";
        Comment comment = Comment.builder().text(commentText).build();

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(commentText))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldGetComments() throws Exception {
        Long postId = 1L;

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldGetCommentById() throws Exception {
        Long postId = 1L;
        Long commentId = 1L;

        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Test comment"));
    }

    @Test
    void shouldUpdateComment() throws Exception {
        Long postId = 1L;
        Long commentId = 1L;
        String updatedText = "Updated comment text";
        Comment updateComment = Comment.builder().id(commentId).postId(postId).text(updatedText).build();

        mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(updatedText));
    }

    @Test
    void shouldDeleteComment() throws Exception {
        Long postId = 1L;
        Long commentId = 1L;

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk());

        when(service.findComment(any())).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNotFound());
    }
}
