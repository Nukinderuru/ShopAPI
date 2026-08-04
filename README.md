# Shop API & Nginx Infrastructure

A learning project focused on backend development and web server configuration.

The project consists of two parts:

- **RESTful Shop API** implemented with Kotlin, Ktor and PostgreSQL.
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

## Project purpose

This project was created as part of a backend development curriculum to gain practical experience with both REST API development and deployment infrastructure.

Rather than focusing only on application code, the project demonstrates how backend services are exposed to clients through a properly configured web server.