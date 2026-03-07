package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.BeforeEach;
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
import ru.yandex.practicum.blog.model.Post;

import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource("classpath:application-test.properties")
@SpringJUnitConfig(classes = {
        AppDataSourceConfiguration.class,
        WebConfiguration.class,
})

@WebAppConfiguration
public class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TITLE_1 = "Test Title";
    private static final String TEXT_1 = "Test Text";
    private static final String TAG_1 = "Tag1";
    private static final String TAG_2 = "Tag2";

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void shouldCreatePost() throws Exception {
        Post post = Post.builder()
                .title(TITLE_1)
                .text(TEXT_1)
                .tags(List.of(TAG_1, TAG_2))
                .build();

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andReturn();

        Post createdPost = objectMapper.readValue(result.getResponse().getContentAsString(), Post.class);
        assertNotNull(createdPost.id());
        assertEquals(TITLE_1, createdPost.title());
        assertEquals(TEXT_1, createdPost.text());
        assertEquals(List.of(TAG_1, TAG_2), createdPost.tags());
    }

    @Test
    void shouldGetPostById() throws Exception {
        Post post = Post.builder()
                .title(TITLE_1)
                .text(TEXT_1)
                .tags(List.of(TAG_1, TAG_2))
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
        assertEquals(TITLE_1, retrievedPost.title());
        assertEquals(TEXT_1, retrievedPost.text());
        assertEquals(List.of(TAG_1, TAG_2), retrievedPost.tags());
    }
}
