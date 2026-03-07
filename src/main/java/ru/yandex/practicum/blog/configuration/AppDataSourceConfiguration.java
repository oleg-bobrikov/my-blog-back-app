package ru.yandex.practicum.blog.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@PropertySource("classpath:application.properties")
public class AppDataSourceConfiguration {

    // Настройка PlatformTransactionManager для управления транзакциями
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // Настройка DataSource — компонент, отвечающий за соединение с базой данных
    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName
    ) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    // NamedParameterJdbcTemplate — компонент для выполнения именованных запросов
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @EventListener
    public void populate(ContextRefreshedEvent event) {
        DataSource dataSource = event.getApplicationContext().getBean(DataSource.class);

        try (Connection connection = dataSource.getConnection()) {
            String dbName = connection.getMetaData().getDatabaseProductName();
            if ("H2".equalsIgnoreCase(dbName)) {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.setSeparator(";;");
                populator.execute(dataSource);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check database type", e);
        }
    }
}
