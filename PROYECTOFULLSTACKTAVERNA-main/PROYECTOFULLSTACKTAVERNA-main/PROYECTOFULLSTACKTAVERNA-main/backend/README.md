# Backend — Spring Boot

La API usa Spring Web MVC, Spring Data JPA, Bean Validation, Spring Security y JWT.
La base de datos de la aplicación es MySQL; Swagger está disponible en
`http://localhost:8082/swagger-ui.html`.

Instala e inicia MySQL Community Server, crea la base `la_taverna` y el usuario
`taverna` siguiendo la sección “Preparar MySQL” del
[README principal](../README.md). Antes de iniciar la aplicación, configura
`MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` y
`ADMIN_REGISTRATION_KEY` en el entorno. La última debe ser una clave aleatoria de
al menos 32 caracteres y permite usar el registro de administradores protegido
desde el Blog. Después ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

Al iniciar, la aplicación carga de manera idempotente los 121 productos del
catálogo original desde `src/main/resources/data/catalogo-inicial.json`. Los
productos existentes y personalizados se conservan; solo se eliminan registros
de ejemplo antiguos que coincidan exactamente con los datos iniciales y cuyas
rutas de imagen ya no existían. Las fotos están en `frontend/public/images`.

Los tests de integración emplean H2 con compatibilidad MySQL y no sustituyen la
validación de persistencia contra un servidor MySQL real:

```powershell
.\mvnw.cmd clean verify
```
