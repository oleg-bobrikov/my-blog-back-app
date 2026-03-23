package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.service.PostService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PostController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostSearchAndPagingIntegrationMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService service;

    private final List<Post> posts = new ArrayList<>();

    private static final String TITLE_SPORT = "Sport";
    private static final String TITLE_EDUCATION = "Education";
    private static final String TAG_YANDEX = "Yandex";
    private static final String TAG_SPRING = "Spring";
    private static final String TEXT_DEFAULT = "Test Text";

    @BeforeEach
    void setup() {
        posts.clear();
        posts.add(Post.builder()
                .id(1L)
                .title(TITLE_SPORT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_YANDEX))
                .build());

        posts.add(Post.builder()
                .id(2L)
                .title(TITLE_EDUCATION)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_YANDEX, TAG_SPRING))
                .build());

        when(service.findPosts(anyString(), any(Pageable.class))).thenAnswer(invocation -> {
            String search = invocation.getArgument(0);
            Pageable pageable = invocation.getArgument(1);

            List<Post> filteredPosts = posts.stream()
                    .filter(post -> {
                        if (search == null || search.isBlank()) return true;
                        String[] searchTerms = search.split("\\s+");
                        for (String term : searchTerms) {
                            boolean termMatched = false;
                            if (term.startsWith("#")) {
                                String tag = term.substring(1);
                                if (post.tags() != null && post.tags().contains(tag)) termMatched = true;
                            } else {
                                if (post.title() != null && post.title().contains(term)) termMatched = true;
                                if (post.text() != null && post.text().contains(term)) termMatched = true;
                            }
                            if (!termMatched) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredPosts.size());
            List<Post> pagedPosts = (start < filteredPosts.size())
                    ? filteredPosts.subList(start, end)
                    : List.of();

            return new PageImpl<>(pagedPosts, pageable, filteredPosts.size());
        });
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

    @Test
    void shouldReturnCorrectPage() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#Yandex")
                        .param("pageNumber", "2")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.lastPage").value(2));
    }
}
