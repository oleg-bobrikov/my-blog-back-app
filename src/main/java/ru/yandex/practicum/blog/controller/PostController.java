package ru.yandex.practicum.blog.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.service.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.validation.constraints.Min;


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

    @GetMapping("/api/posts")
    public Page<Post> getPosts(
            @RequestParam String search,
            @RequestParam @Min(1) int pageNumber,
            @RequestParam @Min(1) int pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return service.findPosts(search, pageable);
    }

    @GetMapping("/{id}")
    public Post get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable @SuppressWarnings("unused") Long id, @RequestBody Post post) {
        service.update(post);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }
}