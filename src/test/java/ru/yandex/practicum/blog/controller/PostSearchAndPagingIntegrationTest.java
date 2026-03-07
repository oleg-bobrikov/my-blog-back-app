package ru.yandex.practicum.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.blog.configuration.AppDataSourceConfiguration;
import ru.yandex.practicum.blog.configuration.WebConfiguration;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {
        AppDataSourceConfiguration.class,
        WebConfiguration.class,
})
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class PostSearchAndPagingIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TITLE_SPORT = "Sport";
    private static final String TITLE_EDUCATION = "Education";
    private static final String TAG_YANDEX = "Yandex";
    private static final String TAG_SPRING = "Spring";
    private static final String TEXT_DEFAULT = "Test Text";

    @BeforeEach
    void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Post.builder()
                                .title(TITLE_SPORT)
                                .text(TEXT_DEFAULT)
                                .tags(List.of(TAG_YANDEX))
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Post.builder()
                                .title(TITLE_EDUCATION)
                                .text(TEXT_DEFAULT)
                                .tags(List.of(TAG_YANDEX, TAG_SPRING))
                                .build())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldSearchByYandexTag() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#Yandex")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2));
    }

    @Test
    void shouldSearchBySpringTag() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#Spring")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1));
    }

    @Test
    void shouldSearchByYandexSportTagAndTitle() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#Yandex Sport")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1));
    }

    @Test
    void shouldReturnEmptyForNonExistentTag() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#AnotherTag")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0));
    }
}
