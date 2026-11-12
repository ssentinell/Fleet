# Автопарк

Вариант **Н–Т**

## Настройки для установленной MySQL

Оба проекта подготовлены для обычного MySQL-подключения **`localhost:3307`**. Порт **33061** относится к X Protocol и в JDBC-настройках не используется.

| Параметр | Значение |
|---|---|
| База данных | `fleet` |
| Пользователь приложения | `fleet_user` |
| MySQL Classic Protocol | `3307` |
| Адрес веб-интерфейса | **http://localhost:8082** |
| Главный класс | `ru.student.fleet.FleetApplication` |


## Запуск готового приложения

1. Распакуйте **обновлённый** архив целиком в отдельную папку.
2. Запустите **`start-mysql-with-examples.cmd`**. 
3. После строки `Started FleetApplication` откройте **http://localhost:8082**.
4. Для остановки нажмите **Ctrl+C** в окне запуска.

Не задавайте `--spring.profiles.active=demo`, если хотите работать с MySQL: этот профиль включает отдельную встроенную H2.

## Структура

```text
src/main/java/ru/student/fleet/
  FleetApplication.java      запуск Spring Boot и часы Москвы
  Car.java                   JPA-сущность, поле марка кириллицей
  CarRequest.java            входные поля и Bean Validation
  CarResponse.java           JSON-ответ
  CarRepository.java         ORM и агрегация постановок на учёт
  CarService.java            CRUD, поиск, сортировка, правила дат
  CarController.java         REST API
  ApiExceptionHandler.java   ошибки HTTP 400 и 404
  DemoData.java              примеры по явному включению
src/main/resources/static/   HTML, CSS и JavaScript
src/test/java/               интеграционные тесты
database/setup.sql           создание MySQL-базы и пользователя
dist/fleet-1.0.0.jar          готовое приложение
docs/report.docx             отчёт Word со скриншотами
docs/screenshots/            отдельные скриншоты
docs/verification.md         результаты проверки
```

## API

| Метод | Адрес | Действие |
|---|---|---|
| GET | `/api/cars` | Найденные записи `cars` и общее количество `total` |
| GET | `/api/cars/{id}` | Один автомобиль |
| POST | `/api/cars` | Создание, ответ 201 с Location |
| PUT | `/api/cars/{id}` | Редактирование, ответ 200 |
| DELETE | `/api/cars/{id}` | Удаление, ответ 204 |
| GET | `/api/cars/histogram` | 30 дней и сумма постановок на учёт |
| GET | `/api/cars/today` | Сегодняшняя дата сервера по Москве |

Параметры поиска: **`марка`**, `owner`, `id`, `yearFrom`, `yearTo`, `from`, `to`, `sorted=true`. Условия объединяются через И, текст ищется по подстроке без учёта регистра. `%` и `_` считаются обычными символами. SQL-условия формируются через JPA Criteria, значения ячеек вставляются через `textContent`.

```json
{
  "марка": "Toyota",
  "productionYear": 2020,
  "registeredOn": "2026-11-12",
  "ownerName": "Петров Павел Андреевич"
}
```