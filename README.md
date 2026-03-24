## User Service

User Service отвечает за управление пользователями и их платёжными картами.  
Сервис является частью микросервисной системы и предполагается, что он будет вызываться через API Gateway.

### Архитектура и слои

- **Controller**: REST‑эндпоинты для работы с пользователями и платёжными картами.
- **Service**: бизнес‑логика (валидации, проверки владельца карты, статусов и т.д.).
- **Repository (JPA)**: доступ к PostgreSQL.
- **Liquibase**: миграции схемы (`db/changelog`).

### Безопасность, JWT и заголовки

JWT не валидируется внутри самого User Service.

- **Планируемый поток аутентификации**:
  1. Клиент получает JWT от auth‑service (Keycloak).
  2. Gateway НЕ валидирует токен самостоятельно, а ходит в auth‑service за валидацией токена (remote introspection / userinfo).
  3. В ответе от auth‑service gateway получает, по сути, доверенный набор атрибутов пользователя: `email`, роль и т.п.
  4. Gateway по `email` вызывает User Service (эндпоинт `GET /users/id-by-email?email=...`), чтобы получить `userId` доменной модели User Service.
  5. Уже после этого gateway кладёт в заголовки:
     - `X-User-Id` — `id` пользователя из User Service,
     - `X-User-Email` — email из ответа auth‑service,
     - `X-User-Role` — роль из ответа auth‑service,
     и с этими заголовками ходит в User Service.

Внутри User Service эти заголовки используются в `HeaderAuthenticationFilter` и `SecurityUtils` для авторизации и проверки прав.

#### Внутренние эндпоинты (без защиты на уровне сервиса)

Часть эндпоинтов сейчас **не защищена на уровне самого микросервиса**, потому что они предполагаются **внутренними** и доступ к ним будет ограничен в конфигурации Gateway:

- `GET /users/id-by-email?email=...` — получить `userId` по `email`. Сейчас этот эндпоинт нужен именно как “маппер” из `email` (который приходит из авторизации) в `userId` доменной модели User Service.
- `POST /users` — создание пользователя.

Важно: в текущем варианте **ID в auth‑service и ID в User Service — разные**:

- в auth‑service (Keycloak) будет свой идентификатор пользователя, который попадает в JWT как `sub`; там же хранятся роль и email;
- в User Service генерируется свой `id` (primary key таблицы `users`), который используется как доменный идентификатор внутри этого сервиса; связка между ними строится через `email` и внутренний эндпоинт `GET /users/id-by-email`.

### Кэширование

Для снижения нагрузки на базу и ускорения типичных запросов используется **Spring Cache + Redis**:

- конфигурация кэша и redis‑подключения — через `spring-boot-starter-cache` и `spring-boot-starter-data-redis`;
- в сервисах (например, в `UserServiceImpl`) кэшируются часто используемые операции поиска пользователей (например, по email/ID);
- Redis выступает общим внешним хранилищем кэша, что позволяет разделять кэш между инстансами сервиса.

### Тестирование

- **Unit + интеграционные тесты**:
  - контроллеры тестируются через `WebTestClient` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`),
  - покрывают полный путь: **Controller → Service → Repository → DB**.
- **Testcontainers + PostgreSQL**:
  - используется профиль `integrationtest`,
  - в `application-integrationtest.yaml` настроен JDBC‑URL:
    - `spring.datasource.url=jdbc:tc:postgresql:15:///user_service_test`,
  - Testcontainers автоматически поднимает контейнер `postgres:15` в Docker, Liquibase накатывает схему.

### CI/CD (GitHub Actions)

Workflow `.github/workflows/ci.yml` выполняет полный пайплайн:

- **Build & Test**:
  - `mvn clean verify` (юнит + интеграционные тесты с Testcontainers).
- **Code analysis**:
  - анализ SonarCloud (используются `SONAR_*` secrets в GitHub).
- **Docker image**:
  - сборка образа из `Dockerfile`;
  - публикация в Docker Hub:
    - `zhenya465/user-service:<branch>`
    - `zhenya465/user-service:<commit-sha>`
    - `zhenya465/user-service:latest` (для ветки `main`).

