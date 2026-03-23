package ru.yandex.practicum.blog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.repository.PostRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(repository.save(any(Post.class))).then(returnsFirstArg());

        // when
        Post saved = service.save(post);

        // then
        assertThat(saved.text()).isEqualTo(expectedText);
        verify(repository).save(argThat(p -> p.text().equals(expectedText)));
    }

    @Test
    void shouldNotTruncateTextWhenSavingShortPost() {
        // given
        String shortText = "Short text";
        Post post = Post.builder().title("Title").text(shortText).build();

        when(repository.save(any(Post.class))).then(returnsFirstArg());

        // when
        Post saved = service.save(post);

        // then
        assertThat(saved.text()).isEqualTo(shortText);
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
        assertThat(found).contains(post);
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

    @Test
    void shouldTruncateTextWhenUpdatingLongPost() {
        // given
        String longText = "a".repeat(150);
        Post post = Post.builder().id(1L).title("Title").text(longText).build();
        String expectedText = "a".repeat(128) + "...";

        when(repository.update(any(Post.class))).then(returnsFirstArg());

        // when
        Post updated = service.update(post);

        // then
        assertThat(updated.text()).isEqualTo(expectedText);
        verify(repository).update(argThat(p -> p.text().equals(expectedText)));
    }

    @Test
    void shouldCallRepositoryLike() {
        // given
        Long id = 1L;
        Post post = Post.builder().id(id).likesCount(1).build();
        when(repository.like(id)).thenReturn(post);

        // when
        Post liked = service.like(id);

        // then
        assertThat(liked).isEqualTo(post);
        verify(repository).like(id);
    }

    @Test
    void shouldCallRepositoryUploadImage() {
        // given
        ru.yandex.practicum.blog.model.Image image = new ru.yandex.practicum.blog.model.Image(1L, new byte[]{1, 2, 3});

        // when
        service.uploadImage(image);

        // then
        verify(repository).uploadImage(image);
    }

    @Test
    void shouldCallRepositoryFindImageByPostId() {
        // given
        Long id = 1L;
        ru.yandex.practicum.blog.model.Image image = new ru.yandex.practicum.blog.model.Image(id, new byte[]{1, 2, 3});
        when(repository.findImageByPostId(id)).thenReturn(Optional.of(image));

        // when
        Optional<ru.yandex.practicum.blog.model.Image> found = service.findImageByPostId(id);

        // then
        assertThat(found).contains(image);
        verify(repository).findImageByPostId(id);
    }

    @Test
    void shouldCallRepositoryComment() {
        // given
        Long postId = 1L;
        ru.yandex.practicum.blog.model.Comment comment = ru.yandex.practicum.blog.model.Comment.builder().text("text").build();
        ru.yandex.practicum.blog.model.Comment savedComment = ru.yandex.practicum.blog.model.Comment.builder().id(1L).postId(postId).text("text").build();
        when(repository.comment(postId, comment)).thenReturn(Optional.of(savedComment));

        // when
        Optional<ru.yandex.practicum.blog.model.Comment> result = service.comment(postId, comment);

        // then
        assertThat(result).contains(savedComment);
        verify(repository).comment(postId, comment);
    }

    @Test
    void shouldCallRepositoryFindCommentsByPostId() {
        // given
        Long postId = 1L;
        ru.yandex.practicum.blog.model.Comment comment = ru.yandex.practicum.blog.model.Comment.builder().id(1L).postId(postId).build();
        when(repository.findCommentsByPostId(postId)).thenReturn(List.of(comment));

        // when
        java.util.List<ru.yandex.practicum.blog.model.Comment> results = service.findCommentsByPostId(postId);

        // then
        assertThat(results).containsExactly(comment);
        verify(repository).findCommentsByPostId(postId);
    }

    @Test
    void shouldCallRepositoryFindComment() {
        // given
        ru.yandex.practicum.blog.model.Comment comment = ru.yandex.practicum.blog.model.Comment.builder().id(1L).postId(1L).build();
        when(repository.findComment(comment)).thenReturn(Optional.of(comment));

        // when
        Optional<ru.yandex.practicum.blog.model.Comment> result = service.findComment(comment);

        // then
        assertThat(result).contains(comment);
        verify(repository).findComment(comment);
    }

    @Test
    void shouldCallRepositoryUpdateComment() {
        // given
        ru.yandex.practicum.blog.model.Comment comment = ru.yandex.practicum.blog.model.Comment.builder().id(1L).postId(1L).text("new").build();
        when(repository.updateComment(comment)).thenReturn(Optional.of(comment));

        // when
        Optional<ru.yandex.practicum.blog.model.Comment> result = service.updateComment(comment);

        // then
        assertThat(result).contains(comment);
        verify(repository).updateComment(comment);
    }

    @Test
    void shouldCallRepositoryDeleteComment() {
        // given
        ru.yandex.practicum.blog.model.Comment comment = ru.yandex.practicum.blog.model.Comment.builder().id(1L).postId(1L).build();

        // when
        service.deleteComment(comment);

        // then
        verify(repository).deleteComment(comment);
    }
}
