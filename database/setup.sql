-- Выполните от root в MySQL Client или Workbench.
CREATE DATABASE IF NOT EXISTS fleet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'fleet_user'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON fleet.* TO 'fleet_user'@'localhost';
-- Hibernate создаёт таблицу cars при первом запуске приложения.
