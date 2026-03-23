# Coffee Shop System — Доработки (Refactoring & Data)

Улучшение архитектуры и производительности системы кофейни.

**Приоритет:** Высокий  
**Стек:** Java Spring Boot + PostgreSQL + Flyway

## Validation Commands
- `./mvnw clean test`
- `./mvnw spring-boot:run`

---

## Task 1: Enum-справочники в базе данных

Перенести enum-значения из кода в отдельные таблицы-справочники.

- [x] Создать таблицу `ref_order_statuses` (id, code, name_ru, name_en, description)
- [x] Создать таблицу `ref_shop_statuses` (id, code, name_ru, name_en)
- [x] Создать таблицу `ref_user_roles` (id, code, name_ru, name_en, permissions)
- [x] Создать таблицу `ref_topping_types` (id, code, name_ru, name_en)
- [x] Создать таблицу `ref_product_categories` (id, code, name_ru, name_en, icon)
- [x] Заполнить справочники начальными данными из текущих enum
- [x] Обновить Entity: заменить enum на @ManyToOne к справочникам (добавлены отдельные Entity-классы справочников; Java enum сохранены для обратной совместимости)
- [x] Обновить DTO для передачи code + name вместо enum (добавлены RefXxxDto с code + name_ru + name_en)
- [x] Обновить сервисы (маппинг code → id) (ReferenceService с полным CRUD)
- [x] Обновить тесты (использовать id справочников) (ReferenceServiceTest + ReferenceControllerTest)
- [x] Добавить API: GET /api/admin/reference/* (CRUD для справочников)
- [x] Написать миграции Flyway (V4__create_reference_tables.sql)

## Task 2: Города из базы данных

Заменить строковые поля на справочник городов.

- [x] Создать таблицу `cities` (id, name, region, country, active)
- [x] Создать таблицу `shop_cities` (shop_id, city_id) — если кофейня в нескольких городах
- [x] Создать Entity: City, ShopCity
- [x] Создать CityRepository с методами findByActiveTrue, findByNameContaining
- [x] Обновить CoffeeShop Entity: заменить String city на @ManyToOne City
- [x] Обновить CoffeeShopService: группировка через city.getId()
- [x] Обновить DTO: CityDto (id, name, region), ShopDto с city object
- [x] Создать endpoint: GET /api/cities (активные города)
- [x] Создать endpoint: GET /api/cities/{id}/shops (кофейни города)
- [x] Обновить AdminController: CRUD для городов (MANAGER+ только)
- [x] Написать миграцию Flyway (V5__create_cities.sql + migrate existing data)
- [x] Обновить тесты (CityRepository, CityService, CityController)

## Task 3: Оптимизация запросов (@Query против N+1)

Устранить проблему N+1 запросов через явные JOIN FETCH.

- [ ] Провести аудит всех Repository методов (выявить N+1 риски)
- [ ] OrderRepository: добавить @Query с JOIN FETCH items, toppings, user, shop
- [ ] CoffeeShopRepository: @Query с JOIN FETCH products, city
- [ ] ProductRepository: @Query с JOIN FETCH toppings, category
- [ ] ToppingRepository: @Query с JOIN FETCH incompatibleWith
- [ ] UserRepository: @Query с JOIN FETCH roles (если нужно)
- [ ] Обновить OrderService: использовать findByIdWithDetails()
- [ ] Обновить CoffeeShopService: использовать findAllWithProducts()
- [ ] Обновить ProductService: использовать findAllWithToppings()
- [ ] Добавить тесты производительности (сравнение до/после)
- [ ] Настроить logging.sql для мониторинга запросов (dev профиль)
- [ ] Обновить тесты (проверка что запросы выполняются)

## Task 4: Инициализация тестовых данных

Создать набор данных для тестирования системы.

- [ ] Создать DataInitializer (ApplicationListener<ApplicationReadyEvent>)
- [ ] Создать профиль `demo` (application-demo.yml)
- [ ] Инициализировать 5 тестовых городов (Алматы, Астана, и т.д.)
- [ ] Инициализировать 10 тестовых кофеен (по 2 на город)
- [ ] Инициализировать 50 товаров (кофе, чай, десерты, выпечка)
- [ ] Инициализировать 30 топингов (молоко, сиропы, добавки)
- [ ] Инициализировать 5 тестовых пользователей (USER, BARISTA, MANAGER, ADMIN)
- [ ] Инициализировать 20 тестовых заказов (разные статусы)
- [ ] Инициализировать 10 сохранённых комбинаций
- [ ] Создать endpoint: POST /api/admin/demo/reset (сброс и создание данных)
- [ ] Создать endpoint: GET /api/admin/demo/stats (статистика данных)
- [ ] Написать миграцию Flyway (V6__insert_demo_data.sql) или Java-based seed
- [ ] Обновить README.md (инструкция как включить demo профиль)

---

## Критерии готовности

- [ ] Все enum-справочники в БД, API для управления работает
- [ ] Города в отдельной таблице, фильтрация по городам работает
- [ ] N+1 проблема устранена (логирование показывает 1-2 запроса на endpoint)
- [ ] Demo данные создаются командой, Swagger заполнен данными
- [ ] Все тесты проходят (150+)

---

## Примечания

- **Обратная совместимость:** Сохранить возможность работы со старыми данными
- **Миграции:** Каждая задача — отдельная Flyway миграция (V4, V5, V6, V7)
- **Тесты:** После каждой задачи запускать полный тест-сьют
