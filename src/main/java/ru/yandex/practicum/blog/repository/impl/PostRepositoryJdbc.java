package ru.yandex.practicum.blog.repository.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import java.util.*;

@Repository
public class PostRepositoryJdbc implements PostRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> Post.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .text(rs.getString("text"))
            .tags(Arrays.asList((String[]) rs.getArray("tags").getArray()))
            .likesCount(rs.getInt("likes_count"))
            .commentsCount(rs.getInt("comments_count"))
            .build();
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
    public Post like(Long postId) {
        String sql = "UPDATE posts SET likes_count = likes_count + 1 WHERE id = :postId";
        jdbc.update(sql, new MapSqlParameterSource("postId", postId));
        return findById(postId);
    }

    @Override
    public Page<Post> findPosts(String search, int pageNumber, int pageSize) {

        Set<String> tags = new HashSet<>();
        List<String> titleWords = new ArrayList<>();

        for (String word : search.split("\\s+")) {
            if (word.startsWith("#")) {
                tags.add(word.substring(1));
            } else if (!word.isBlank()) {
                titleWords.add(word);
            }
        }

        String titleSearch = String.join(" ", titleWords);

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (!titleSearch.isBlank()) {
            where.append(" AND posts.title ILIKE :title");
            params.addValue("title", "%" + titleSearch + "%");
        }

        if (!tags.isEmpty()) {
            where.append(" AND tags.name IN (:tags)");
            params.addValue("tags", tags);
        }

        String fromSql = """
            FROM posts
            LEFT JOIN post_tags ON posts.id = post_tags.post_id
            LEFT JOIN tags ON post_tags.tag_id = tags.id
            """ + where + """
            GROUP BY posts.id
            """;

        if (!tags.isEmpty()) {
            fromSql += " HAVING COUNT(DISTINCT tags.name) = :tagCount";
            params.addValue("tagCount", tags.size());
        }

        String selectSql = """
            SELECT
                posts.id,
                posts.title,
                posts.text,
                posts.likes_count,
                posts.comments_count,
                COALESCE(
                    array_agg(DISTINCT tags.name)
                    FILTER (WHERE tags.name IS NOT NULL),
                    '{}'
                ) AS tags
            """ + fromSql;

        String countSql = "SELECT COUNT(*) FROM (SELECT posts.id " + fromSql + ") AS total";

        Long total = jdbc.queryForObject(countSql, params, Long.class);

        params
                .addValue("limit", pageSize)
                .addValue("offset", pageNumber * pageSize);

        selectSql += " LIMIT :limit OFFSET :offset";

        List<Post> posts = jdbc.query(selectSql, params, postRowMapper);

        return new PageImpl<>(posts, PageRequest.of(pageNumber, pageSize), total == null ? 0 : total);
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
        String sql = """
                INSERT INTO images (post_id, data) VALUES (:postId, :data)
                ON CONFLICT (post_id) DO UPDATE SET data = EXCLUDED.data
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", image.postId())
                .addValue("data", image.data());
        jdbc.update(sql, params);
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
        if (findById(postId) == null) {
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
