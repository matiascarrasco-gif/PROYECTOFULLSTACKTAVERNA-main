CREATE DATABASE IF NOT EXISTS la_taverna
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'taverna'@'localhost'
    IDENTIFIED BY 'taverna_dev_only';

ALTER USER 'taverna'@'localhost'
    IDENTIFIED BY 'taverna_dev_only';

GRANT ALL PRIVILEGES ON la_taverna.* TO 'taverna'@'localhost';
FLUSH PRIVILEGES;
