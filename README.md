# My Blog Backend Application

Бэкенд веб-приложения блога, реализованный на Java с использованием Spring Framework 6 без Spring Boot.  
Приложение предоставляет REST API для управления постами, комментариями, лайками и изображениями и интегрируется с React-фронтендом.

## 📌 Описание проекта

Приложение состоит из трёх компонентов:

- **Frontend** — React-приложение, работающее через Nginx (Docker)
- **Backend** — Spring Framework приложение, работающее в servlet-container (Tomcat / Jetty)
- **Database** — база данных для хранения постов и комментариев (PostgreSQL / H2)

Архитектура:

Browser
↓
Frontend (React + Nginx) http://localhost:80

↓ REST API
Backend (Spring Framework) http://localhost:8080

↓ JDBC
Database (PostgreSQL / H2)

---

## 🛠 Используемые технологии

- Java 21
- Spring Framework 6 (Core, Web, JDBC, Test)
- Spring Test Framework
- Maven / Gradle
- Servlet API
- Tomcat / Jetty
- PostgreSQL или H2
- JUnit 5
- Docker / Docker Compose
- Nginx
- Lombok (опционально)

---

## 📁 Структура проекта

```
my-blog-back-app/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/blog/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── dao/
│   │   │       ├── model/
│   │   │       └── config/
│   │   │
│   │   ├── resources/
│   │   │   ├── schema.sql
│   │   │   └── application.properties
│   │   │
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           └── web.xml
│   │
│   └── test/
│       └── java/
│           └── com/example/blog/
│               ├── service/
│               ├── dao/
│               └── controller/
│
├── pom.xml / build.gradle
└── README.md
```


---

## 🧱 Архитектура backend

Приложение реализовано согласно многоуровневой архитектуре:

### Controller Layer
Обрабатывает HTTP запросы и возвращает JSON ответы.

Примеры:
GET /api/posts
POST /api/posts
PUT /api/posts/{id}
DELETE /api/posts/{id}


---

### Service Layer
Содержит бизнес-логику:

- создание поста
- редактирование
- фильтрация
- управление лайками
- управление комментариями

---

### DAO Layer
Работает с базой данных через JDBC.

Отвечает за:

- CRUD операции
- SQL запросы
- маппинг ResultSet → Model

---

### Model Layer

Основные сущности:

Post
id
title
text
tags
likesCount
Comment
id
text
postId


---

## 🗄 Структура базы данных

Пример:

```sql
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    likes_count INT DEFAULT 0
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
    tag_id BIGINT REFERENCES tags(id) ON DELETE CASCADE
);

▶️ Запуск frontend

Перейдите в директорию frontend:
cd frontend

Запустите:
docker compose up -d

Проверьте:
docker ps

Frontend будет доступен:
http://localhost

▶️ Сборка backend
mvn clean package

После сборки будет создан:
target/my-blog-back-app.war

▶️ Запуск backend (Tomcat)

Скопируйте war файл в:
TOMCAT_HOME/webapps/

Запустите Tomcat:
TOMCAT_HOME/bin/startup.sh

Backend будет доступен:
http://localhost:8080

🧪 Запуск тестов
mvn test

📡 REST API
Получение списка постов
GET /api/posts?search=text&pageNumber=1&pageSize=5
Ответ:
{
  "posts": [],
  "hasPrev": false,
  "hasNext": true,
  "lastPage": 5
}

Создание поста
POST /api/posts
{
  "title": "Post title",
  "text": "Post text",
  "tags": ["spring", "java"]
}

Лайк поста
POST /api/posts/{id}/likes

Комментарии
GET    /api/posts/{id}/comments
POST   /api/posts/{id}/comments
PUT    /api/posts/{id}/comments/{commentId}
DELETE /api/posts/{id}/comments/{commentId}

🧪 Тестирование

Проект содержит:
Unit tests

Тестируют:
Service layer
Business logic

Используют:
JUnit 5
Spring Test
Mockito (опционально)

Integration tests

Тестируют:
Controller
DAO
Database integration

Используют:
Spring TestContext Framework
Embedded H2 Database
MockMvc

🧩 Особенности реализации

Без использования Spring Boot
WAR deployment
Java-based Spring configuration
REST API
Pagination
Filtering by tags and title
Image upload support
Integration tests
Context caching

🧑‍💻 Git workflow
Используется GitFlow:

main
 └── feature

 Рабочий процесс:
 feature → commit → push → pull request → review → merge