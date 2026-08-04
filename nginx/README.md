# Nginx Chapter II — пошаговое выполнение заданий

Этот README — практический гайд по выполнению заданий из Chapter II проекта по Nginx. Идея не просто получить рабочий конфиг, а понимать, зачем нужна каждая часть: reverse proxy, routing, балансировка, кэширование, gzip и локальный HTTPS.

## 0. Общая схема

В проекте Nginx выступает как входная точка для пользователя.

```text
browser / curl
    ↓
Nginx
    ├── отдаёт статические файлы
    ├── проксирует API в backend
    ├── проксирует /admin в pgAdmin
    ├── показывает /status
    ├── балансирует GET-запросы между backend-инстансами
    ├── кэширует безопасные GET-запросы
    ├── сжимает текстовые ответы gzip
    └── принимает HTTPS-запросы
```

Пример имён сервисов в Docker/Compose:

```text
nginx       — reverse proxy
app         — основной backend
app-read-1  — read-only backend №1
app-read-2  — read-only backend №2
pgadmin     — pgAdmin
```

---

## 1. Reverse proxy к приложению

### Что нужно сделать

Настроить Nginx так, чтобы он принимал HTTP-запросы на своём порту и пересылал их в backend-приложение.

### Почему так

Клиент не ходит напрямую в приложение. Он ходит в Nginx, а Nginx уже решает, куда отправить запрос дальше.

```text
browser → Nginx → backend
```

Это и есть reverse proxy.

### Минимальный пример

```nginx
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://app:8080;

        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Что здесь происходит

`listen 80;` — Nginx слушает обычный HTTP-порт.

`server_name _;` — условное имя для дефолтного локального сервера. В учебном проекте это нормально.

`location / { ... }` — блок для всех путей, если нет более точного `location`.

`proxy_pass http://app:8080;` — Nginx пересылает запрос в backend-сервис `app` на порт `8080`.

Заголовки `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` нужны, чтобы backend понимал, кто был исходным клиентом и по какому протоколу он пришёл.

### Как проверить

```bash
curl -i http://localhost/
```

Если Nginx в контейнере и проброшен на другой порт:

```bash
curl -i http://localhost:8080/
```

---

## 2. Routing для web application

По заданию нужно настроить несколько маршрутов:

```text
/api        → редирект на /api/v1
/api/v1     → Swagger
/api/v1/... → backend API
/           → статическая страница
/image.png  → статическая картинка
/admin      → pgAdmin
/status     → Nginx status
```

---

## 2.1. Редирект `/api` → `/api/v1`

### Конфиг

```nginx
location = /api {
    return 301 $scheme://$http_host/api/v1;
}

location = /api/ {
    return 301 $scheme://$http_host/api/v1;
}
```

### Что здесь происходит

`location = /api` — точное совпадение. Этот блок сработает только для `/api`, но не для `/api/users`.

`return 301 ...` — постоянный редирект.

`$scheme` — встроенная переменная Nginx: `http` или `https`.

`$http_host` — значение заголовка `Host`, например `localhost`, `shop.local` или `localhost:8080`.

### Как проверить

```bash
curl -i http://localhost/api
```

Ожидаемо:

```text
HTTP/1.1 301 Moved Permanently
Location: http://localhost/api/v1
```

---

## 2.2. Swagger на `/api/v1`

### Конфиг

```nginx
location = /api/v1 {
    proxy_pass http://app:8080/swagger;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location = /api/v1/ {
    proxy_pass http://app:8080/swagger/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location = /api/v1/openapi {
    proxy_pass http://app:8080/swagger/documentation.yaml;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### Почему отдельно `/api/v1` и `/api/v1/`

Для Nginx это разные URI: `/api/v1` и `/api/v1/`.

Если Swagger внутри backend доступен как `/swagger` и `/swagger/`, удобно явно проксировать оба варианта.

### Как проверить

Открыть в браузере:

```text
http://localhost/api/v1
```

Или:

```bash
curl -i http://localhost/api/v1/openapi
```

---

## 2.3. Проксирование `/swagger/`

### Конфиг

```nginx
location ^~ /swagger/ {
    proxy_pass http://app:8080/swagger/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### Что значит `^~`

`^~` означает prefix-location с приоритетом: если URI начинается с `/swagger/`, используй этот location и не ищи regex-location дальше.

---

## 2.4. API-запросы `/api/v1/...`

### Конфиг без балансировки

```nginx
location ^~ /api/v1/ {
    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### Что важно

Здесь `proxy_pass` без URI на конце:

```nginx
proxy_pass http://app:8080;
```

Это значит, что исходный путь сохранится:

```text
/api/v1/items → http://app:8080/api/v1/items
```

---

## 2.5. Статика на `/`

### Что нужно сделать

Положить в директорию статической раздачи два файла:

```text
index.html
image.png
```

Например, если используется официальный nginx image:

```text
/usr/share/nginx/html/index.html
/usr/share/nginx/html/image.png
```

### Конфиг

```nginx
server {
    root /usr/share/nginx/html;

    location = / {
        rewrite ^ /index.html break;
    }

    location = /index.html {
    }

    location = /image.png {
    }
}
```

### Что здесь происходит

`root /usr/share/nginx/html;` — Nginx будет искать статические файлы в этой директории.

`location = / { rewrite ^ /index.html break; }` — при запросе `/` Nginx отдаст `/index.html`.

Пустые блоки `location = /index.html {}` и `location = /image.png {}` означают: “этот путь разрешён, отдай файл из `root`”.

### Как проверить

```bash
curl -i http://localhost/
curl -i http://localhost/image.png
```

---

## 2.6. pgAdmin на `/admin`

### Конфиг

```nginx
location = /admin {
    return 301 /admin/;
}

location /admin/ {
    proxy_set_header X-Script-Name /admin;
    proxy_set_header X-Scheme $scheme;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_pass http://pgadmin:80/;
    proxy_redirect off;
}
```

### Почему есть редирект `/admin` → `/admin/`

Для приложений, которые живут не в корне сайта, а под префиксом, слэш часто важен. `/admin/` проще корректно использовать как базовый путь для внутренних ресурсов.

### Что делает `X-Script-Name`

Он сообщает приложению, что оно опубликовано не в корне `/`, а под префиксом `/admin`. Иначе pgAdmin может пытаться загрузить ресурсы с неправильных путей.

### Как проверить

Открыть:

```text
http://localhost/admin/
```

---

## 2.7. Nginx status на `/status`

### Конфиг

```nginx
location = /status {
    stub_status;
    access_log off;
}
```

### Что делает `stub_status`

Показывает простую страницу состояния Nginx: active connections, accepted/handled/requests, reading/writing/waiting.

### Как проверить

```bash
curl http://localhost/status
```

---

## 3. Балансировка GET-запросов для `/api/v1`

### Что нужно сделать

Запустить 3 backend-инстанса:

```text
app         — основной backend
app-read-1  — read-only backend
app-read-2  — read-only backend
```

И настроить Nginx так, чтобы GET-запросы на `/api/v1/...` распределялись в пропорции `2 : 1 : 1`. Изменяющие запросы должны идти только в основной backend.

---

## 3.1. Upstream

### Конфиг

```nginx
upstream api_v1_get_backends {
    server app:8080 weight=2;
    server app-read-1:8081 weight=1;
    server app-read-2:8082 weight=1;
}
```

### Что здесь происходит

`upstream api_v1_get_backends` — группа backend-серверов.

`weight=2` — первый backend получает примерно в два раза больше запросов, чем каждый read-only backend.

```text
app        → 2/4 запросов
app-read-1 → 1/4 запросов
app-read-2 → 1/4 запросов
```

---

## 3.2. Разделяем GET и не-GET

### Конфиг

```nginx
location ^~ /api/v1/ {
    error_page 418 = @api_v1_get_backends;

    if ($request_method = GET) {
        return 418;
    }

    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location @api_v1_get_backends {
    proxy_pass http://api_v1_get_backends;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### Что происходит

Для GET:

```text
GET /api/v1/items
    ↓
location ^~ /api/v1/
    ↓
return 418
    ↓
error_page 418 = @api_v1_get_backends
    ↓
балансировка между app, app-read-1, app-read-2
```

Для POST/PUT/PATCH/DELETE:

```text
POST /api/v1/items
    ↓
location ^~ /api/v1/
    ↓
proxy_pass http://app:8080
```

### Почему 418

`418` здесь не настоящая ошибка для клиента, а внутренний технический маркер. Nginx получает `return 418`, но из-за `error_page 418 = @api_v1_get_backends;` не отдаёт клиенту 418, а внутренне переводит запрос в named location.

### Что значит `@api_v1_get_backends`

`@...` — это named location. В него нельзя попасть напрямую по URL. Он нужен только для внутренней маршрутизации внутри Nginx.

---

## 4. Кэширование всех GET-запросов, кроме `/api`

### Что нужно сделать

Включить кэширование для GET/HEAD-запросов, но не кэшировать API.

Практический смысл:

```text
статические и публичные GET-ответы → можно кэшировать
/api → по заданию не кэшируем
запросы с Authorization/Cookie → не кэшируем, чтобы не сохранить персональные данные
```

---

## 4.1. Зона кэша

### Конфиг в `http`-контексте

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=shop_cache:10m max_size=256m inactive=60m use_temp_path=off;
```

### Что это значит

- `/var/cache/nginx` — директория, где будут лежать файлы кэша.
- `levels=1:2` — Nginx раскладывает кэш по вложенным директориям, чтобы в одной папке не было слишком много файлов.
- `keys_zone=shop_cache:10m` — shared memory zone с именем `shop_cache` размером `10m`; там Nginx хранит метаданные кэша.
- `max_size=256m` — максимальный размер кэша на диске.
- `inactive=60m` — если кэшированный объект не запрашивали 60 минут, его можно удалить.
- `use_temp_path=off` — временные файлы пишутся сразу рядом с кэшем.

---

## 4.2. `map` для условий кэширования

### Конфиг

```nginx
map $request_method $cacheable_method {
    default 0;
    GET 1;
    HEAD 1;
}

map $request_uri $is_api_request {
    default 0;
    ~^/api(?:/|$) 1;
}

map $http_authorization $has_auth_header {
    default 1;
    "" 0;
}

map $http_cookie $has_cookie {
    default 1;
    "" 0;
}

map "$cacheable_method:$is_api_request:$has_auth_header:$has_cookie" $skip_proxy_cache {
    default 1;
    "1:0:0:0" 0;
}
```

### Идея

`map` работает как маленький `when`/`switch`: посмотреть на входную переменную и положить результат в новую переменную.

Пример:

```text
GET  → cacheable_method = 1
HEAD → cacheable_method = 1
всё остальное → cacheable_method = 0
```

Финальный `map` собирает несколько признаков в одну строку:

```text
$cacheable_method:$is_api_request:$has_auth_header:$has_cookie
```

Пример:

```text
GET /index.html без Authorization и Cookie
→ 1:0:0:0
→ skip_proxy_cache = 0
→ кэш можно использовать
```

А вот API:

```text
GET /api/v1/items без Authorization и Cookie
→ 1:1:0:0
→ default 1
→ skip_proxy_cache = 1
→ кэш пропускаем
```

---

## 4.3. Включить кэш в `server`

### Конфиг

```nginx
server {
    proxy_cache shop_cache;
    proxy_cache_methods GET HEAD;
    proxy_cache_bypass $skip_proxy_cache;
    proxy_no_cache $skip_proxy_cache;
    proxy_cache_valid 200 301 302 10m;
    proxy_cache_valid 404 1m;
    add_header X-Cache-Status $upstream_cache_status always;

    # locations...
}
```

### Что здесь происходит

- `proxy_cache shop_cache;` — включаем кэш-зону.
- `proxy_cache_methods GET HEAD;` — кэшировать можно только GET и HEAD.
- `proxy_cache_bypass $skip_proxy_cache;` — если `$skip_proxy_cache = 1`, не читать ответ из кэша.
- `proxy_no_cache $skip_proxy_cache;` — если `$skip_proxy_cache = 1`, не сохранять ответ в кэш.
- `proxy_cache_valid 200 301 302 10m;` — ответы 200, 301, 302 хранить 10 минут.
- `proxy_cache_valid 404 1m;` — 404 хранить 1 минуту.
- `add_header X-Cache-Status $upstream_cache_status always;` — добавить диагностический заголовок.

Возможные значения `X-Cache-Status`:

```text
MISS   — в кэше не было, сходили в backend
HIT    — отдали из кэша
BYPASS — кэш специально пропущен
```

### Как проверить

```bash
curl -I http://localhost/
curl -I http://localhost/
```

Первый раз часто будет `X-Cache-Status: MISS`, второй раз — `X-Cache-Status: HIT`.

Для API:

```bash
curl -I http://localhost/api/v1/items
```

Ожидаемо кэш должен быть пропущен.

---

## 5. Gzip compression

### Что нужно сделать

Включить gzip-сжатие для текстовых типов данных, но не применять его к медиа-файлам вроде jpeg/png.

### Почему так

Текст хорошо сжимается: html, css, js, json, xml, svg.

Картинки обычно уже сжаты: jpeg, png, webp. Сжимать их gzip-ом почти бесполезно и иногда даже вредно.

### Конфиг

```nginx
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_buffers 16 8k;
gzip_http_version 1.1;
gzip_min_length 1100;
gzip_types
    text/plain
    text/css
    text/xml
    application/json
    application/javascript
    application/xml
    application/xml+rss
    image/svg+xml;
```

### Что здесь происходит

- `gzip on;` — включает gzip.
- `gzip_vary on;` — добавляет `Vary: Accept-Encoding`, чтобы кэши понимали, что gzip- и non-gzip-версии ответа отличаются.
- `gzip_comp_level 6;` — уровень сжатия. `1` быстрее, `9` сильнее, `6` — нормальный компромисс.
- `gzip_min_length 1100;` — не сжимать слишком маленькие ответы.
- `gzip_types ...;` — список MIME-типов, которые можно сжимать.

### Почему тут нет `image/png` и `image/jpeg`

Потому что задание говорит не сжимать media types. PNG/JPEG уже являются сжатыми форматами.

### Как проверить

```bash
curl -H "Accept-Encoding: gzip" -I http://localhost/index.html
```

Ожидаемо:

```text
Content-Encoding: gzip
```

Для картинки:

```bash
curl -H "Accept-Encoding: gzip" -I http://localhost/image.png
```

`Content-Encoding: gzip` быть не должно.

---

## 6. HTTPS на локальной машине

### Что нужно сделать

1. Создать локальное доменное имя.
2. Сгенерировать self-signed сертификат для этого имени.
3. Подключить сертификат в Nginx.
4. Настроить reverse proxy через HTTPS.

Возьмём пример домена:

```text
shop.local
```

---

## 6.1. Локальный DNS через `/etc/hosts`

### Команда

```bash
sudo vim /etc/hosts
```

Добавить строку:

```text
127.0.0.1 shop.local
```

### Что это значит

Когда браузер или `curl` попытается открыть `shop.local`, операционная система сначала посмотрит в `/etc/hosts` и поймёт:

```text
shop.local → 127.0.0.1
```

### Проверка

```bash
ping shop.local
```

Ожидаемо:

```text
PING shop.local (127.0.0.1)
```

---

## 6.2. Self-signed certificate

### Создать директорию

```bash
mkdir -p nginx/certs
```

### Создать openssl config

```bash
cat > nginx/certs/shop.local.conf <<'OPENSSL_CONF'
[req]
default_bits = 2048
prompt = no
default_md = sha256
x509_extensions = v3_req
distinguished_name = dn

[dn]
CN = shop.local

[v3_req]
subjectAltName = @alt_names

[alt_names]
DNS.1 = shop.local
OPENSSL_CONF
```

### Почему нужен `subjectAltName`

Современные браузеры проверяют доменное имя сертификата через SAN — Subject Alternative Name. Одного `CN = shop.local` часто уже недостаточно.

### Сгенерировать сертификат и ключ

```bash
openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout nginx/certs/shop.local.key \
  -out nginx/certs/shop.local.crt \
  -config nginx/certs/shop.local.conf
```

После этого появятся:

```text
nginx/certs/shop.local.crt — публичный сертификат
nginx/certs/shop.local.key — приватный ключ
```

Приватный ключ нельзя коммитить в публичный репозиторий.

---

## 6.3. HTTPS server block

### Конфиг

```nginx
server {
    listen 80;
    server_name shop.local;

    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name shop.local;

    ssl_certificate /etc/nginx/certs/shop.local.crt;
    ssl_certificate_key /etc/nginx/certs/shop.local.key;

    location / {
        proxy_pass http://app:8080;

        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

### Что происходит

Первый `server` перенаправляет HTTP на HTTPS.

Второй `server` слушает HTTPS-порт, подключает сертификат и проксирует запросы в backend.

До backend-а Nginx всё ещё ходит по обычному HTTP внутри локальной сети/контейнерной сети. Это нормально для учебного локального проекта.

---

## 6.4. Если Nginx в Docker/Podman Compose

### Пример volumes и ports

```yaml
services:
  nginx:
    image: nginx:1.27-alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro
      - ./nginx/certs:/etc/nginx/certs:ro
    depends_on:
      - app
```

Если rootless Podman не разрешает порты ниже 1024, можно использовать:

```yaml
ports:
  - "8080:80"
  - "8443:443"
```

Тогда открывать:

```text
https://shop.local:8443
```

---

## 6.5. Проверка HTTPS

### Проверить конфиг Nginx

Если Nginx установлен локально:

```bash
sudo nginx -t
```

Если Nginx в контейнере:

```bash
podman exec -it <nginx-container-name> nginx -t
```

### Перезапустить

```bash
podman compose up -d --build
```

### Проверить curl

```bash
curl -k -i https://shop.local
```

Если используется порт `8443`:

```bash
curl -k -i https://shop.local:8443
```

`-k` нужен, потому что сертификат self-signed и система ему по умолчанию не доверяет.

---

## 7. Пример итогового `default.conf`

Ниже пример, который объединяет основные части.

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=shop_cache:10m max_size=256m inactive=60m use_temp_path=off;

map $request_method $cacheable_method {
    default 0;
    GET 1;
    HEAD 1;
}

map $request_uri $is_api_request {
    default 0;
    ~^/api(?:/|$) 1;
}

map $http_authorization $has_auth_header {
    default 1;
    "" 0;
}

map $http_cookie $has_cookie {
    default 1;
    "" 0;
}

map "$cacheable_method:$is_api_request:$has_auth_header:$has_cookie" $skip_proxy_cache {
    default 1;
    "1:0:0:0" 0;
}

upstream api_v1_get_backends {
    server app:8080 weight=2;
    server app-read-1:8081 weight=1;
    server app-read-2:8082 weight=1;
}

server {
    listen 80;
    server_name shop.local;

    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name shop.local;
    root /usr/share/nginx/html;

    ssl_certificate /etc/nginx/certs/shop.local.crt;
    ssl_certificate_key /etc/nginx/certs/shop.local.key;

    proxy_cache shop_cache;
    proxy_cache_methods GET HEAD;
    proxy_cache_bypass $skip_proxy_cache;
    proxy_no_cache $skip_proxy_cache;
    proxy_cache_valid 200 301 302 10m;
    proxy_cache_valid 404 1m;
    add_header X-Cache-Status $upstream_cache_status always;

    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_buffers 16 8k;
    gzip_http_version 1.1;
    gzip_min_length 1100;
    gzip_types
        text/plain
        text/css
        text/xml
        application/json
        application/javascript
        application/xml
        application/xml+rss
        image/svg+xml;

    location = /api {
        return 301 https://$http_host/api/v1;
    }

    location = /api/ {
        return 301 https://$http_host/api/v1;
    }

    location = /api/v1 {
        proxy_pass http://app:8080/swagger;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location = /api/v1/ {
        proxy_pass http://app:8080/swagger/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location = /api/v1/openapi {
        proxy_pass http://app:8080/swagger/documentation.yaml;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location ^~ /swagger/ {
        proxy_pass http://app:8080/swagger/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location ^~ /api/v1/ {
        error_page 418 = @api_v1_get_backends;

        if ($request_method = GET) {
            return 418;
        }

        proxy_pass http://app:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location @api_v1_get_backends {
        proxy_pass http://api_v1_get_backends;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location = / {
        rewrite ^ /index.html break;
    }

    location = /index.html {
    }

    location = /image.png {
    }

    location = /admin {
        return 301 /admin/;
    }

    location /admin/ {
        proxy_set_header X-Script-Name /admin;
        proxy_set_header X-Scheme https;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_pass http://pgadmin:80/;
        proxy_redirect off;
    }

    location = /status {
        stub_status;
        access_log off;
    }

    location / {
        return 404;
    }
}
```

---

## 8. Финальный чек-лист

### Reverse proxy

```bash
curl -k -i https://shop.local:8443
```

### `/api` redirect

```bash
curl -k -i https://shop.local:8443/api
```

Ожидаемо: `301` на `/api/v1`.

### Swagger

```bash
curl -k -i https://shop.local:8443/api/v1
curl -k -i https://shop.local:8443/api/v1/openapi
```

### Static files

```bash
curl -k -i https://shop.local:8443/
curl -k -i https://shop.local:8443/image.png
```

### pgAdmin

```bash
curl -k -i https://shop.local:8443/admin/
```

### Nginx status

```bash
curl https://shop.local:8443/status
```

### Balancing

Сделать несколько GET-запросов:

```bash
for i in {1..20}; do curl -k -s https://shop.local:8443/api/v1/clients > /dev/null; done
```

Потом посмотреть логи backend-инстансов. Запросы должны распределяться примерно 2:1:1.

### Cache

```bash
curl -k -I https://shop.local:8443/demo
curl -k -I https://shop.local:8443/demo
```

Ожидаемо увидеть `X-Cache-Status: MISS`, потом `HIT`.

Для `/api` кэш должен пропускаться.

### Gzip

```bash
curl -k -H "Accept-Encoding: gzip" -I https://shop.local:8443/index.html
```

Ожидаемо:

```text
Content-Encoding: gzip
```

Для png:

```bash
curl -k -H "Accept-Encoding: gzip" -I https://shop.local:8443/image.png
```

`Content-Encoding: gzip` быть не должно.

### HTTPS

```bash
curl -k -i https://shop.local
```

Если порт 8443:

```bash
curl -k -i https://shop.local:8443
```

---

## 9. Частые проблемы

### `host not found in upstream "app"`

Nginx не видит сервис `app`.

Проверить:

- имя сервиса в Compose;
- что Nginx и backend в одной Docker/Podman-сети;
- что backend-контейнер запущен.

### `permission denied` для сертификата

Nginx не может прочитать `.key` или `.crt`.

Проверить права:

```bash
ls -l nginx/certs
```

Для локального учебного проекта обычно достаточно, чтобы файл был читаем контейнером.

### Браузер ругается на сертификат

Это нормально для self-signed certificate.

Для проверки можно использовать:

```bash
curl -k https://shop.local
```

### `/admin` открывается, но ресурсы ломаются

Проверить:

```nginx
proxy_set_header X-Script-Name /admin;
```

и редирект:

```nginx
location = /admin {
    return 301 /admin/;
}
```

### Кэш не даёт `HIT`

Проверить:

- запрос точно GET или HEAD;
- путь не начинается с `/api`;
- нет `Authorization`;
- нет `Cookie`;
- ответ имеет статус 200, 301, 302 или 404;
- есть ли заголовок `X-Cache-Status`.
