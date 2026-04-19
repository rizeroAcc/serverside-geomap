# serverside-geomap

**Серверная часть геоинформационной системы** на **Kotlin + Ktor**.

Проект представляет собой backend-сервис для работы с геопространственными данными: хранение, обработка и выдача информации для карт.

## Возможности

- Асинхронный веб-сервер на **Ktor**
- Подключение к **PostgreSQL**
- ORM **Exposed** для работы с базой
- **kotlinx.serialization** + Content Negotiation (JSON)
- Валидация входящих запросов
- Dependency Injection через **Koin**

## Стек

| Технология              | Назначение                     |
|-------------------------|--------------------------------|
| Kotlin                  | Основной язык                  |
| Ktor                    | Веб-фреймворк                  |
| Koin                    | Dependency Injection           |
| Exposed                 | SQL-библиотека                 |
| PostgreSQL              | Хранение геоданных             |
| kotlinx.serialization   | Сериализация JSON              |
