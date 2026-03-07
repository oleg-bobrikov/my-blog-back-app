package ru.yandex.practicum.blog.repository.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Image;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.repository.PostRepository;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Array;
import java.sql.SQLException;
import java.util.*;

@Repository
public class PostRepositoryJdbc implements PostRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> {
        Post.PostBuilder builder = Post.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .text(rs.getString("text"))
                .likesCount(rs.getInt("likes_count"))
                .commentsCount(rs.getInt("comments_count"));

        try {
            Array tagsArray = rs.getArray("tags");
            if (tagsArray != null) {
                builder.tags(Arrays.asList((String[]) tagsArray.getArray()));
            }
        } catch (SQLException ignored) {
        }
        return builder.build();
    };
    private final RowMapper<Image> imageRowMapper = (rs, rowNum) -> new Image(
            rs.getLong("post_id"),
            rs.getBytes("data")
    );
    private final RowMapper<Comment> commentRowMapper = (rs, rowNum) -> Comment.builder()
            .id(rs.getLong("id"))
            .postId(rs.getLong("post_id"))
            .text(rs.getString("text"))
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

        return findById(generatedId).orElse(null);
    }

    private void saveTags(Long postId, List<String> tags) {
        String selectTagSql = "SELECT id FROM tags WHERE name = :name";
        String insertTagSql = "INSERT INTO tags (name) VALUES (:name)";
        String insertPostTagSql = "INSERT INTO post_tags (post_id, tag_id) VALUES (:postId, :tagId)";

        for (String tag : tags) {
            List<Long> tagIds = jdbc.queryForList(selectTagSql, new MapSqlParameterSource("name", tag), Long.class);
            Long tagId;
            if (tagIds.isEmpty()) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbc.update(insertTagSql, new MapSqlParameterSource("name", tag), keyHolder, new String[]{"id"});
                tagId = Objects.requireNonNull(keyHolder.getKey()).longValue();
            } else {
                tagId = tagIds.getFirst();
            }

            jdbc.update(insertPostTagSql, new MapSqlParameterSource()
                    .addValue("postId", postId)
                    .addValue("tagId", tagId));
        }
    }

    @Override
    public Post update(Post post) {
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

        return findById(post.id()).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM posts WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    @Override
    public Optional<Post> findById(Long id) {
        String sql = "SELECT * FROM posts WHERE id = :id";
        List<Post> posts = jdbc.query(sql, new MapSqlParameterSource("id", id), postRowMapper);

        if (posts.isEmpty()) {
            return Optional.empty();
        }

        Post post = posts.getFirst();
        return Optional.of(Post.builder()
                .id(post.id())
                .title(post.title())
                .text(post.text())
                .tags(getTagsForPost(post.id()))
                .likesCount(post.likesCount())
                .commentsCount(post.commentsCount())
                .build());
    }

    @Override
    public Post like(Long postId) {
        String sql = "UPDATE posts SET likes_count = likes_count + 1 WHERE id = :postId";
        jdbc.update(sql, new MapSqlParameterSource("postId", postId));
        return findById(postId).orElse(null);
    }

    @Override
    public Page<Post> findPosts(String search, Pageable pageable) {

        Set<String> tags = new HashSet<>();
        List<String> titleWords = new ArrayList<>();

        for (String word : search.split("\\s+")) {
            if (word.isBlank()) continue;
            if (word.startsWith("#")) {
                for (String tag : word.split("#")) {
                    if (!tag.isBlank()) {
                        tags.add(tag);
                    }
                }
            } else {
                titleWords.add(word);
            }
        }

        String titleSearch = String.join(" ", titleWords);

        String fromSql = """
                FROM posts
                WHERE 1=1
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (!titleSearch.isBlank()) {
            fromSql += " AND LOWER(posts.title) LIKE LOWER(:title)";
            params.addValue("title", "%" + titleSearch + "%");
        }

        if (!tags.isEmpty()) {
            fromSql += """
                     AND EXISTS (
                        SELECT 1 FROM post_tags pt
                        JOIN tags t ON pt.tag_id = t.id
                        WHERE pt.post_id = posts.id AND t.name IN (:tags)
                        HAVING COUNT(DISTINCT t.name) = :tagCount
                    )
                    """;
            params.addValue("tags", tags);
            params.addValue("tagCount", tags.size());
        }

        String countSql = "SELECT COUNT(*) " + fromSql;
        params
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        Long total = jdbc.queryForObject(countSql, params, Long.class);

        String selectSql = getSelectSql(fromSql);
        List<Post> posts = jdbc.query(selectSql, params, postRowMapper);

        return new PageImpl<>(posts, pageable, total == null ? 0 : total);
    }

    // language=SQL
    private String getSelectSql(String fromSql) {
        return """
                SELECT
                    posts.id,
                    posts.title,
                    posts.text,
                    posts.likes_count,
                    posts.comments_count,
                    NULL AS tags
                """ + fromSql + """
                ORDER BY posts.id DESC
                LIMIT :limit OFFSET :offset
                """;
    }

    private List<String> getTagsForPost(Long postId) {
        String sql = """
                SELECT tags.name FROM tags
                JOIN post_tags ON tags.id = post_tags.tag_id
                WHERE post_tags.post_id = :postId
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("postId", postId), String.class);
    }

    @Override
    public void uploadImage(Image image) {
        String checkSql = "SELECT 1 FROM images WHERE post_id = :postId";
        List<Integer> exists = jdbc.queryForList(checkSql, new MapSqlParameterSource("postId", image.postId()), Integer.class);

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", image.postId())
                .addValue("data", image.data());

        if (exists.isEmpty()) {
            String insertSql = "INSERT INTO images (post_id, data) VALUES (:postId, :data)";
            jdbc.update(insertSql, params);
        } else {
            String updateSql = "UPDATE images SET data = :data WHERE post_id = :postId";
            jdbc.update(updateSql, params);
        }
    }

    @Override
    public Optional<Image> findImageByPostId(Long postId) {
        String sql = "SELECT * FROM images WHERE post_id = :postId";
        return jdbc.query(sql, new MapSqlParameterSource("postId", postId), imageRowMapper)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Comment> comment(Long postId, Comment comment) {
        if (findById(postId).isEmpty()) {
            return Optional.empty();
        }

        String sql = "INSERT INTO comments (post_id, text) VALUES (:postId, :text)";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("text", comment.text());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        return Optional.of(Comment.builder()
                .id(generatedId)
                .postId(postId)
                .text(comment.text())
                .build());
    }

    @Override
    public Optional<Comment> findComment(Comment comment) {
        String sql = "SELECT * FROM comments WHERE id = :commentId AND post_id = :postId";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("commentId", comment.id())
                .addValue("postId", comment.postId());
        return jdbc.query(sql, params, commentRowMapper)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Comment> updateComment(Comment comment) {
        String sql = "UPDATE comments SET text = :text WHERE id = :commentId AND post_id = :postId";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("text", comment.text())
                .addValue("commentId", comment.id())
                .addValue("postId", comment.postId());

        int updatedRows = jdbc.update(sql, params);
        if (updatedRows == 0) {
            return Optional.empty();
        }

        return findComment(comment);
    }

    @Override
    public void deleteComment(Comment comment) {
        String sql = "DELETE FROM comments WHERE id = :commentId AND post_id = :postId";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("commentId", comment.id())
                .addValue("postId", comment.postId());
        jdbc.update(sql, params);
    }

    @Override
    public List<Comment> findCommentsByPostId(Long postId) {
        String sql = "SELECT * FROM comments WHERE post_id = :postId";
        return jdbc.query(sql, new MapSqlParameterSource("postId", postId), commentRowMapper);
    }
}
