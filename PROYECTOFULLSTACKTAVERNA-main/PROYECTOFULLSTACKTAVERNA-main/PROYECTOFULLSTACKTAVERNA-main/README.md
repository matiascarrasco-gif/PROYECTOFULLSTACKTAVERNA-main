# Cómo iniciar La Taverna

## La primera vez

1. Instala **Java JDK 21**, **Node.js LTS** y **XAMPP**.
2. Abre en VS Code la carpeta del proyecto, donde aparecen `backend`, `frontend` y `database`.
3. Abre XAMPP y presiona **Start** en **MySQL** y **Apache**.
4. Entra a `http://localhost/phpmyadmin`.
5. Abre **Importar**, selecciona `database/crear_bd.sql` y presiona **Importar**.

Importa ese archivo solo si todavía no tienes la base de datos preparada. Si ya existe y tiene tus datos, no repitas este paso.

## Cada vez que quieras usar el proyecto

### 1. Enciende la base de datos

Abre XAMPP y presiona **Start** en **MySQL**. Apache solo hace falta para preparar la base de datos por primera vez.

### 2. Inicia el servidor

En VS Code, haz clic derecho en la carpeta `backend` y elige **Open in Integrated Terminal**. Copia estos comandos:

```powershell
$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/la_taverna?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:MYSQL_USER = 'taverna'
$env:MYSQL_PASSWORD = 'taverna_dev_only'
$env:ADMIN_EMAIL = 'admin@example.com'
$env:ADMIN_PASSWORD = '<escribe una clave de al menos 12 caracteres>'
.\mvnw.cmd spring-boot:run
```

Reemplaza el correo y la clave por los datos que quieras usar para iniciar sesión como administrador. Si esa cuenta ya existe, escribe los datos actuales: estos comandos no cambian la clave de una cuenta existente.

Deja esta ventana abierta mientras uses el proyecto.

### 3. Inicia la página

En VS Code, haz clic derecho en la carpeta `frontend` y elige **Open in Integrated Terminal**.

La primera vez, copia:

```powershell
npm.cmd install
```

Después, cada vez que quieras iniciar la página, copia:

```powershell
npm.cmd run dev
```

Deja también esta ventana abierta.

### 4. Abre el proyecto

Entra a `http://localhost:5173`.

### 5. Cierra el proyecto

Presiona **Ctrl + C** en las dos ventanas de VS Code. Cuando termines, puedes presionar **Stop** en MySQL en XAMPP.

## Para registrar otro administrador

Si vas a registrar otro administrador, agrega este comando a los del paso 2, antes de `.\mvnw.cmd spring-boot:run`. Escribe una clave inventada por ti que tenga al menos 32 caracteres:

```powershell
$env:ADMIN_REGISTRATION_KEY = '<tu clave de invitación>'
```

Usa esa misma clave en el formulario para registrar al nuevo administrador.
