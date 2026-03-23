package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.blog.dto.PageResponse;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostSearchAndPagingFullIntegrationTest extends BasePostFullIntegrationTest {

    private static final String TITLE_SPORT = "Sport";
    private static final String TITLE_EDUCATION = "Education";
    private static final String TAG_YANDEX = "Yandex";
    private static final String TAG_SPRING = "Spring";

    @BeforeEach
    void setup() {
        createPost(Post.builder()
                .title(TITLE_SPORT)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_YANDEX))
                .build());

        createPost(Post.builder()
                .title(TITLE_EDUCATION)
                .text(TEXT_DEFAULT)
                .tags(List.of(TAG_YANDEX, TAG_SPRING))
                .build());
    }

    @Test
    void shouldSearchByYandexTag() {
        PageResponse<Post> response = searchPosts("#Yandex", 1, 10);
        assertEquals(2, response.posts().size());
    }

    @Test
    void shouldSearchBySpringTag() {
        PageResponse<Post> response = searchPosts("#Spring", 1, 10);
        assertEquals(1, response.posts().size());
    }

    @Test
    void shouldSearchByYandexSportTagAndTitle() {
        PageResponse<Post> response = searchPosts("#Yandex Sport", 1, 10);
        assertEquals(1, response.posts().size());
    }

    @Test
    void shouldReturnEmptyForNonExistentTag() {
        PageResponse<Post> response = searchPosts("#AnotherTag", 1, 10);
        assertEquals(0, response.posts().size());
    }

    @Test
    void shouldReturnCorrectPage() {
        PageResponse<Post> response = searchPosts("#Yandex", 2, 1);
        assertEquals(1, response.posts().size());
        assertEquals(2, response.lastPage());
    }
}
