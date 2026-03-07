package ru.yandex.practicum.blog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.repository.PostRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository repository;

    @InjectMocks
    private PostService service;

    @Test
    void shouldTruncateTextWhenSavingLongPost() {
        // given
        String longText = "a".repeat(150);
        Post post = Post.builder().title("Title").text(longText).build();
        String expectedText = "a".repeat(128) + "...";
        
        when(repository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Post saved = service.save(post);

        // then
        assertEquals(expectedText, saved.text());
        verify(repository).save(argThat(p -> p.text().equals(expectedText)));
    }

    @Test
    void shouldNotTruncateTextWhenSavingShortPost() {
        // given
        String shortText = "Short text";
        Post post = Post.builder().title("Title").text(shortText).build();
        
        when(repository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Post saved = service.save(post);

        // then
        assertEquals(shortText, saved.text());
        verify(repository).save(post);
    }

    @Test
    void shouldCallRepositoryFindById() {
        // given
        Long id = 1L;
        Post post = Post.builder().id(id).title("Title").build();
        when(repository.findById(id)).thenReturn(Optional.of(post));

        // when
        Optional<Post> found = service.findById(id);

        // then
        assertEquals(Optional.of(post), found);
        verify(repository).findById(id);
    }

    @Test
    void shouldCallRepositoryDeleteById() {
        // given
        Long id = 1L;

        // when
        service.deleteById(id);

        // then
        verify(repository).deleteById(id);
    }
}
