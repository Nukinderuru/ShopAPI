# Shop API & Nginx Infrastructure

A learning project focused on backend development and web server configuration.

The project consists of three parts:

- **RESTful Shop API** implemented with Kotlin, Ktor and PostgreSQL.
- **Authorization service** implemented as a separate gRPC application with its own PostgreSQL database.
- **Nginx infrastructure** providing reverse proxying, routing, load balancing, caching, compression and HTTPS.

The goal of the project was not only to implement a REST API, but also to understand how a production-like web infrastructure is built around a backend application.

---

## Features

### REST API

- RESTful API design
- CRUD operations for:
    - Clients
    - Products
    - Suppliers
    - Product images
- PostgreSQL database
- Repository pattern
- DAO → DTO mapping
- Request validation
- Proper HTTP status codes
- Pagination
- Binary image download
- OpenAPI specification generation
- Swagger UI
- Integration with the authorization service through gRPC
- Public registration, authentication and password reset endpoints
- JWT-based authorization for protected endpoints
- Custom route authorization marker with middleware-based token validation

### Authorization service

- Separate `auth-service` application
- Separate PostgreSQL database for authorization data
- gRPC-only API described in `auth-contract/src/main/proto/auth.proto`
- User registration with signed JWT token response
- Username/password authentication with signed JWT token response
- JWT token validation for the shop API middleware
- Password change by valid token and old password
- Password reset with a temporary password printed to the auth-service console
- Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes, not plain text
- Dependency injection through Koin interfaces

### Shop API authorization

- Public endpoints:
    - `POST /api/v1/register`
    - `POST /api/v1/auth`
    - `POST /api/v1/reset`
- Public catalog endpoints:
    - product read endpoints
    - product image read endpoint
- Protected endpoints require `Authorization: Bearer <jwt>`
- Client and supplier endpoints are protected, including `GET` requests
- Product and image mutation endpoints are protected
- Invalid, missing or malformed tokens return `401 Unauthorized`

---

### Nginx configuration

- Reverse proxy to the backend application
- URL routing
- API version redirection (`/api → /api/v1`)
- Swagger publishing
- Static content serving
- pgAdmin reverse proxy
- Nginx status page
- Load balancing for GET requests (2:1:1)
- Proxy caching
- Gzip compression
- Local HTTPS with a self-signed certificate

---

## Technologies

### Backend

- Kotlin
- Ktor
- PostgreSQL
- Exposed
- Liquibase
- gRPC / Protocol Buffers
- JWT
- Koin
- OpenAPI / Swagger

### Infrastructure

- Nginx
- Docker / Podman
- pgAdmin
- OpenSSL

---

## What I learned

During this project I explored:

- RESTful API design principles
- gRPC service contracts and generated Kotlin stubs
- JWT-based authentication and authorization
- Password hashing with salt
- Dependency injection with interfaces and IOC container
- HTTP methods and status codes
- OpenAPI documentation generation
- Reverse proxying
- Request routing in Nginx
- Load balancing strategies
- Proxy caching
- Gzip compression
- HTTPS and TLS
- Self-signed certificate generation
- Basic production-like web server configuration

---

## Implemented Nginx features

### Reverse proxy

Nginx acts as the public entry point for the application and forwards incoming requests to the backend service.

### Routing

Configured routes include:

- `/api` → `/api/v1`
- `/api/v1` → Swagger UI
- `/admin` → pgAdmin
- `/status` → Nginx status page
- `/` → static website

### Load balancing

Three backend instances are configured.

GET requests are distributed with a weighted **2:1:1** ratio:

- Primary backend (weight = 2)
- Read replica #1 (weight = 1)
- Read replica #2 (weight = 1)

Write operations are always sent to the primary backend.

### Proxy cache

Proxy caching is configured for cacheable GET requests according to the project requirements.

### Compression

Gzip compression is enabled for text-based content while excluding media files.

### HTTPS

A local HTTPS environment is configured using:

- custom local domain
- self-signed TLS certificate
- OpenSSL
- Nginx SSL termination

---

## Local Run

### 1. Start PostgreSQL databases

The shop API and auth service use different PostgreSQL databases.

If you use Docker Compose from this repository:

```bash
AUTH_JWT_SECRET=local-secret-local-secret-local-secret docker compose up -d postgres auth-postgres
```

If you use your own PostgreSQL or Podman containers, pass matching JDBC URLs through environment variables when starting the applications.

### 2. Start auth-service

```bash
./gradlew :auth-service:run
```

### 3. Start shop-api

```bash
./gradlew :shop-api:run
```

### 4. Open Swagger

When running `shop-api` directly:

```text
http://localhost:8080/swagger
```

When running through nginx from Docker Compose:

```text
http://localhost:8081/swagger
```

### 5. Try authentication

Register a user:

```bash
curl -X POST http://localhost:8080/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{"email":"example@gmail.com","firstName":"John","lastName":"Doe","phone":"+79991234567","password":"secretPassword123!"}'
```

Authenticate:

```bash
curl -X POST http://localhost:8080/api/v1/auth \
  -H "Content-Type: application/json" \
  -d '{"email":"example@gmail.com","password":"secretPassword123!"}'
```

Call a protected endpoint:

```bash
curl http://localhost:8080/api/v1/clients \
  -H "Authorization: Bearer <TOKEN>"
```

Call a public catalog endpoint:

```bash
curl http://localhost:8080/api/v1/products/available
```

---

## Project purpose

This project was created as part of a backend development curriculum to gain practical experience with both REST API development and deployment infrastructure.

Rather than focusing only on application code, the project demonstrates how backend services are exposed to clients through a properly configured web server.
