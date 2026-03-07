# My Blog Backend Application

Бэкенд веб-приложения блога, реализованный на **Java 21** с использованием **Spring Framework 6 (без Spring Boot)**.
Приложение предоставляет **REST API** для управления постами, комментариями, лайками и изображениями и интегрируется с React-frontend.

---

# 📌 Описание проекта

Приложение состоит из трёх компонентов:

* **Frontend** — React + Nginx (Docker)
* **Backend** — Spring Framework (WAR deployment, Tomcat / Jetty)
* **Database** — PostgreSQL или H2

---

# 🏗 Архитектура системы

```
Browser
   │
   ▼
Frontend (React + Nginx)
http://localhost
   │
   │ REST API
   ▼
Backend (Spring Framework)
http://localhost:8080
   │
   │ JDBC
   ▼
Database (PostgreSQL / H2)
```

---

# 🛠 Используемые технологии

## Core

* Java 21
* Spring Framework 6

  * Spring Core
  * Spring Web (Spring MVC)
  * Spring JDBC
  * Spring Test

## Build tools

* Maven или Gradle

## Server

* Tomcat или Jetty
* Servlet API

## Database

* PostgreSQL
* H2 (для тестов)

## Testing

* JUnit 5
* Spring Test
* Mockito
* MockMvc

## DevOps

* Docker
* Docker Compose
* Nginx

## Optional

* Lombok

---

# 📁 Структура проекта

```
my-blog-back-app/
│
├── src/
│   ├── main/
│   │   ├── java/ru/yandex/practicum/blog/
│   │   │   ├── configuration/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   │
│   │   ├── resources/
│   │   │   ├── schema.sql
│   │   │   └── application.properties
│   │   │
│   │   └── webapp/WEB-INF/
│   │       └── web.xml
│   │
│   └── test/java/ru/yandex/practicum/blog/
│       ├── controller/
│       ├── service/
│       └── repository/
│
├── pom.xml
└── README.md
```

---

# 🧱 Архитектура backend

Используется классическая **многоуровневая архитектура**.

---

## Controller Layer

Обрабатывает HTTP-запросы и возвращает JSON.

Примеры:

```
GET    /api/posts
POST   /api/posts
PUT    /api/posts/{id}
DELETE /api/posts/{id}
```

Ответственность:

* обработка HTTP запросов
* валидация входных данных
* возврат JSON

---

## Service Layer

Содержит бизнес-логику:

* создание постов
* редактирование
* фильтрация
* управление лайками
* управление комментариями

---

## DAO Layer

Работает с базой данных через JDBC.

Отвечает за:

* CRUD операции
* SQL запросы
* mapping ResultSet → Model

---

## Model Layer

Основные сущности:

### Post

```
id
title
text
tags
likesCount
commentsCount
```

### Comment

```
id
text
postId
```

---

# 🗄 Структура базы данных

```sql
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0
);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    post_id BIGINT REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE post_tags (
    post_id BIGINT REFERENCES posts(id) ON DELETE CASCADE,
    tag_id BIGINT REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE TABLE images (
    post_id BIGINT PRIMARY KEY REFERENCES posts(id) ON DELETE CASCADE,
    data BYTEA NOT NULL
);
```

---

# ▶️ Запуск frontend

Перейдите в директорию frontend:

```bash
cd frontend
```

Запустите:

```bash
docker compose up -d
```

Проверьте:

```bash
docker ps
```

Frontend будет доступен:

```
http://localhost
```

---

# ▶️ Сборка backend

```bash
mvn clean package
```

После сборки появится:

```
target/my-blog-back-app.war
```

---

# ▶️ Запуск backend (Tomcat)

Скопируйте WAR файл:

```
TOMCAT_HOME/webapps/
```

Запустите Tomcat:

```bash
TOMCAT_HOME/bin/startup.sh
```

Backend будет доступен:

```
http://localhost:8080
```

---

# 🧪 Запуск тестов

```bash
mvn test
```

---

# 📡 REST API

## Получение списка постов

```
GET /api/posts?search=text&pageNumber=1&pageSize=5
```

Ответ:

```json
{
  "posts": [],
  "hasPrev": false,
  "hasNext": true,
  "lastPage": 5
}
```

---

## Получение поста по ID

```
GET /api/posts/{id}
```

---

## Создание поста

```
POST /api/posts
```

```json
{
  "title": "Post title",
  "text": "Post text",
  "tags": ["spring", "java"]
}
```

---

## Обновление поста

```
PUT /api/posts/{id}
```

---

## Удаление поста

```
DELETE /api/posts/{id}
```

---

## Лайк поста

```
POST /api/posts/{id}/likes
```

---

## Изображения

```
GET /api/posts/{id}/image
PUT /api/posts/{id}/image (multipart/form-data)
```

---

## Комментарии

```
GET     /api/posts/{id}/comments
POST    /api/posts/{id}/comments
GET     /api/posts/{id}/comments/{commentId}
PUT     /api/posts/{id}/comments/{commentId}
DELETE  /api/posts/{id}/comments/{commentId}
```

---

# 🧪 Тестирование

## Unit tests

Тестируют:

* Service layer
* business logic

Используют:

* JUnit 5
* Spring Test
* Mockito

---

## Integration tests

Тестируют:

* Controller
* DAO
* database integration

Используют:

* Spring TestContext Framework
* H2 Database
* MockMvc

---

# 🧩 Особенности реализации

* Без Spring Boot
* WAR deployment
* Java-based configuration
* REST API
* Pagination
* Filtering
* Tag support
* Image upload
* Integration tests
* Context caching

---

# 🧑‍💻 Git workflow

Используется GitFlow:

```
main
 └── feature/*
```

Workflow:

```
feature → commit → push → pull request → review → merge
```