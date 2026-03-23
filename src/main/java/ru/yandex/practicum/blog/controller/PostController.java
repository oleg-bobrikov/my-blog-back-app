package ru.yandex.practicum.blog.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.blog.dto.PageResponse;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Image;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.service.PostService;

import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Post save(@RequestBody Post post) {
        return service.save(post);
    }

    @GetMapping
    public PageResponse<Post> getPosts(
            @RequestParam String search,
            @RequestParam @Min(1) int pageNumber,
            @RequestParam @Min(1) int pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<Post> page = service.findPosts(search, pageable);

        return new PageResponse<>(
                page.getContent(),
                page.hasPrevious(),
                page.hasNext(),
                page.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Post update(@PathVariable @SuppressWarnings("unused") Long id, @RequestBody Post post) {
        return service.update(post);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<Post> like(@PathVariable Long postId) {
        Post post = service.like(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId}/image")
    public void uploadImage(@PathVariable Long postId, @RequestParam("image") MultipartFile file) throws IOException {
        Image image = new Image(postId, file.getBytes());
        service.uploadImage(image);
    }

    @GetMapping("/{postId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long postId) {
        return service.findImageByPostId(postId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(image.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> comment(@PathVariable Long postId, @RequestBody Comment comment) {
        return service.comment(postId, comment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{postId}/comments")
    public List<Comment> getComments(@PathVariable Long postId) {
        return service.findCommentsByPostId(postId);
    }

    @GetMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Comment> getComment(@PathVariable Long postId, @PathVariable Long commentId) {
        Comment comment = Comment.builder().id(commentId).postId(postId).build();
        return service.findComment(comment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable @SuppressWarnings("unused") Long postId,
            @PathVariable @SuppressWarnings("unused") Long commentId,
            @RequestBody Comment comment) {
        return service.updateComment(comment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public void deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        Comment comment = Comment.builder().id(commentId).postId(postId).build();
        service.deleteComment(comment);
    }
}