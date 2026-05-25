# CookYourBooks

Your kitchen library — recipes, collections, shopping lists, and recipe OCR
import. A Java/Spring backend and a Next.js frontend.

## Stack

- **Backend**: Java 21, Spring Boot 3.3.5, Maven, PostgreSQL 16, Flyway, JWT
- **Frontend**: Next.js 14 (App Router), Tailwind CSS, TypeScript
- **Deploy**: Fly.io (backend), Vercel (frontend)

## Prerequisites

- Java 21 (Temurin recommended)
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16 running locally
- An OpenSSL binary (for generating the JWT secret)

## Backend setup

### 1. Create the database

```bash
createdb cookyourbooks
```

### 2. Configure environment variables

The backend reads its config from environment variables (see
`backend/src/main/resources/application.yml`). A template lives at
**`backend/.env.local`** with placeholder values — it is gitignored, so edit
it in place and add your real secrets.

| Variable             | Required | Purpose                                                                                                  |
| -------------------- | -------- | -------------------------------------------------------------------------------------------------------- |
| `DATABASE_URL`       | yes      | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/cookyourbooks`                                          |
| `DATABASE_USERNAME`  | yes      | PostgreSQL user                                                                                          |
| `DATABASE_PASSWORD`  | yes      | PostgreSQL password                                                                                      |
| `JWT_SECRET`         | yes      | Base64-encoded HMAC key. Must decode to ≥ 32 bytes (256 bits). Generate with `openssl rand -base64 32`.  |
| `JWT_TTL_MINUTES`    | no       | Access-token lifetime in minutes (default `120`)                                                         |
| `GEMINI_API_KEY`     | OCR only | Google Gemini key for `/api/ocr/import`. Get one at https://aistudio.google.com/apikey                   |
| `USDA_API_KEY`       | no       | USDA FoodData Central key. Defaults to the shared `DEMO_KEY` (rate-limited). https://api.data.gov/signup |

#### Generating `JWT_SECRET`

```bash
openssl rand -base64 32
```

The backend validates the decoded length on startup and refuses to boot if it
is shorter than 32 bytes.

### 3. Load the env file and run

```bash
cd backend
set -a; source .env.local; set +a   # loads DATABASE_URL, JWT_SECRET, etc.
mvn spring-boot:run
```

The API serves on `http://localhost:8080`. Flyway runs migrations
(`db/migration/V1__init_schema.sql`, `V2__seed_units_and_conversions.sql`) on
first boot.

> **Note:** Spring Boot does not read `.env.local` automatically. Source it in
> your shell, configure your IDE's run configuration with the same vars, or
> use [direnv](https://direnv.net/).

### Tests

```bash
cd backend
mvn test
```

Tests run against an in-memory H2 database with `create-drop` schema; no
PostgreSQL or env vars required.

## Frontend setup

```bash
cd web
npm install
npm run dev
```

The web app serves on `http://localhost:3000` and expects the backend at
`http://localhost:8080`. Override with `NEXT_PUBLIC_API_BASE_URL` if needed.

### Production build

```bash
cd web
npm run build
npm start
```

## Project layout

```
backend/        Spring Boot API (this is the active backend)
backend-legacy/ Read-only reference — do not modify
web/            Next.js frontend
```

## Endpoints (summary)

- `POST /api/auth/register`, `POST /api/auth/login`
- `GET/POST/PUT/DELETE /api/recipes[/{id}]`
- `GET /api/ingredients[?q=]`, `GET /api/ingredients/{id}`
- `GET/POST/PUT/DELETE /api/collections[/{id}]`,
  `POST/DELETE /api/collections/{id}/recipes/{recipeId}`
- `GET/POST/PUT/DELETE /api/shopping-lists[/{id}]`,
  item CRUD under `/items`, and `POST /{id}/generate-from-recipes`
- `POST /api/ocr/import` (multipart `image`)
- `GET /actuator/health`

All routes except `register`, `login`, and `actuator/health` require a
`Authorization: Bearer <token>` header.
