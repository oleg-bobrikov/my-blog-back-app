package ru.yandex.practicum.blog.repository.impl;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.repository.PostRepository;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Objects;

@Repository
public class PostRepositoryJdbc implements PostRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> Post.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .text(rs.getString("text"))
            .likesCount(rs.getInt("likes_count"))
            .commentsCount(rs.getInt("comments_count"))
            .build();

    public PostRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    @Override
    public Post save(Post post) {
        String sql = "INSERT INTO posts (title, text) VALUES (:title, :text)";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", post.title())
                .addValue("text", post.text());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        if (post.tags() != null && !post.tags().isEmpty()) {
            saveTags(generatedId, post.tags());
        }

        return findById(generatedId);
    }

    private void saveTags(Long postId, List<String> tags) {
        String insertTagSql = "INSERT INTO tags (name) VALUES (:name) ON CONFLICT (name) DO NOTHING";
        String insertPostTagSql = """
                INSERT INTO post_tags (post_id, tag_id)
                SELECT :postId, id FROM tags WHERE name = :name
                """;

        for (String tag : tags) {
            jdbc.update(insertTagSql, new MapSqlParameterSource("name", tag));
            jdbc.update(insertPostTagSql, new MapSqlParameterSource()
                    .addValue("postId", postId)
                    .addValue("name", tag));
        }
    }

    @Override
    public void update(Post post) {
        String sql = "UPDATE posts SET title = :title, text = :text WHERE id = :id";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", post.title())
                .addValue("text", post.text())
                .addValue("id", post.id());
        jdbc.update(sql, params);

        String deleteTagsSql = "DELETE FROM post_tags WHERE post_id = :postId";
        jdbc.update(deleteTagsSql, new MapSqlParameterSource("postId", post.id()));

        if (post.tags() != null && !post.tags().isEmpty()) {
            saveTags(post.id(), post.tags());
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM posts WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    @Override
    public Post findById(Long id) {
        String sql = "SELECT * FROM posts WHERE id = :id";
        List<Post> posts = jdbc.query(sql, new MapSqlParameterSource("id", id), postRowMapper);

        if (posts.isEmpty()) {
            return null;
        }

        Post post = posts.getFirst();
        return Post.builder()
                .id(post.id())
                .title(post.title())
                .text(post.text())
                .tags(getTagsForPost(post.id()))
                .likesCount(post.likesCount())
                .commentsCount(post.commentsCount())
                .build();
    }

    @Override
    public List<Post> findPosts(String search, int pageNumber, int pageSize) {
        String sql = """
                SELECT *
                FROM posts
                WHERE title LIKE :search OR text LIKE :search
                LIMIT :limit
                OFFSET :offset
                """;
        String searchPattern = "%" + search + "%";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("search", searchPattern)
                .addValue("limit", pageSize)
                .addValue("offset", pageNumber * pageSize);
        List<Post> posts = jdbc.query(sql, params, postRowMapper);

        return posts.stream()
                .map(post -> Post.builder()
                        .id(post.id())
                        .title(post.title())
                        .text(post.text())
                        .tags(getTagsForPost(post.id()))
                        .likesCount(post.likesCount())
                        .commentsCount(post.commentsCount())
                        .build())
                .toList();
    }

    private List<String> getTagsForPost(Long postId) {
        String sql = """
                SELECT t.name FROM tags t
                JOIN post_tags pt ON t.id = pt.tag_id
                WHERE pt.post_id = :postId
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("postId", postId), String.class);
    }
}
