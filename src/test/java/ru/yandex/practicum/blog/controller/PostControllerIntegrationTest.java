package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.blog.configuration.AppDataSourceConfiguration;
import ru.yandex.practicum.blog.configuration.WebConfiguration;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.service.PostService;

import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource("classpath:application-test.properties")
@SpringJUnitConfig(classes = {
        AppDataSourceConfiguration.class,
        WebConfiguration.class,
})

@WebAppConfiguration
public class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private PostService postService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TITLE_DEFAULT = "Test Title";
    private static final String TEXT_DEFAULT = "Test Text";
    private static final String TAG_DEFAULT = "Tag";

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        postService.deleteAll();
    }

    @Test
    void shouldCreatePost() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_DEFAULT))
                .build();

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(result.getResponse().getContentAsString(), Post.class);
        assertNotNull(createdPost.id());
        assertEquals(TITLE_DEFAULT, createdPost.title());
        assertEquals(TEXT_DEFAULT, createdPost.text());
        assertEquals(List.of(TAG_DEFAULT), createdPost.tags());
    }

    @Test
    void shouldGetPostById() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_DEFAULT))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), Post.class);
        Long id = createdPost.id();

        MvcResult getResult = mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        Post retrievedPost = objectMapper.readValue(getResult.getResponse().getContentAsString(), Post.class);
        assertEquals(id, retrievedPost.id());
        assertEquals(TITLE_DEFAULT, retrievedPost.title());
        assertEquals(TEXT_DEFAULT, retrievedPost.text());
        assertEquals(List.of(TAG_DEFAULT), retrievedPost.tags());
    }

    @Test
    void shouldUpdatePost() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), Post.class);
        Long id = createdPost.id();

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

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle));
    }

    @Test
    void shouldDeletePost() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), Post.class);
        Long id = createdPost.id();

        mockMvc.perform(delete("/api/posts/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLikePost() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), Post.class);
        Long id = createdPost.id();
        assertEquals(0, createdPost.likesCount());

        mockMvc.perform(post("/api/posts/{id}/likes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void shouldUploadAndGetImage() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), Post.class);
        Long id = createdPost.id();

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
    void shouldManageComments() throws Exception {
        Post post = Post.builder()
                .title(TITLE_DEFAULT)
                .text(TEXT_DEFAULT)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), Post.class);
        Long postId = createdPost.id();

        String commentText = "Test comment";
        Comment comment = Comment.builder().text(commentText).build();

        // Create comment
        MvcResult commentResult = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(commentText))
                .andReturn();

        Comment createdComment = objectMapper.readValue(commentResult.getResponse().getContentAsString(), Comment.class);
        Long commentId = createdComment.id();
        assertNotNull(commentId);

        // Get comments list
        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(commentId));

        // Get comment by id
        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(commentText));

        // Update comment
        String updatedText = "Updated comment text";
        Comment updateComment = Comment.builder().id(commentId).postId(postId).text(updatedText).build();

        mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(updatedText));

        // Delete comment
        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNotFound());
    }

}
