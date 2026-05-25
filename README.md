# CookYourBooks

> A full-stack recipe manager: save recipes, scale them by servings, group them
> into collections, and auto-generate shopping lists. Recipes can be imported
> from a photo via Google Gemini OCR.

A solo-built monorepo that pairs a Spring Boot REST API with a TanStack Start
(Vite + SSR) frontend. Built end-to-end — schema, auth, business logic, OCR
integration, type-safe client, and UI.

---

## Highlights

- **Type-safe end-to-end** — every backend DTO has a mirrored TypeScript
  interface; the frontend `api.ts` is a thin, typed namespace per resource.
- **JWT auth from scratch** — JJWT-signed tokens, bcrypt hashes, custom
  `OncePerRequestFilter`, stateless `SecurityFilterChain`, route-level guards
  on the frontend via TanStack Router `beforeLoad`.
- **Cookbook-grade unit conversions** — a seeded units table (mL, g, cup, tbsp,
  pinch, stick, …) plus an ingredient-aware conversion table for
  density-based crosses (1 cup flour → grams).
- **OCR recipe import** — a multipart endpoint that asks Gemini 2.5 Flash for
  a strict JSON shape with `responseMimeType: application/json`, then
  fuzzy-matches the extracted names back into the seeded ingredient/unit
  catalog and returns unmatched terms so the UI can prompt for cleanup.
- **Real database migrations** — Flyway, with `ddl-auto: validate` so the JPA
  entities stay locked to the SQL schema in source control.
- **Tests run hermetically** — H2 with PostgreSQL mode + `create-drop`, no
  external services touched.

---

## Tech stack

| Layer    | Choices                                                                                        | Why                                                                                                                  |
| -------- | ---------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Backend  | **Java 21**, Spring Boot 3.3, Maven, Spring Security, Spring Data JPA, Flyway, JJWT, Lombok    | Stable, productive, hireable. Records + pattern matching make the DTO layer compact without sacrificing type safety. |
| Database | **PostgreSQL 16** (Supabase in prod), H2 for tests                                             | Stored decimals, FK constraints, and Flyway-friendly. Supabase pooler for serverless-style connections.              |
| Frontend | **TanStack Start** (Vite + React 19 SSR), TanStack Router (file-based), TanStack Query         | Built-in SSR & data router, but escapes Next.js lock-in. Query handles cache invalidation across mutations cleanly.  |
| UI       | **Tailwind v4** (PostCSS plugin), shadcn/ui, Radix primitives, Lucide icons, Sonner for toasts | Design system primitives + utility CSS = fast iteration without hand-rolling components.                             |
| AI       | **Google Gemini 2.5 Flash** (`generateContent` with `responseSchema`)                          | Cheap, JSON-mode-native, and good at structured OCR from photos.                                                     |
| Deploy   | Fly.io (backend) + Cloudflare Workers via Wrangler (frontend SSR)                              | Edge SSR for the UI, regional containers for the DB-bound API.                                                       |

---

## Features

| Feature              | Endpoint(s)                                                | Notes                                                                                                |
| -------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Email/password auth  | `POST /api/auth/register`, `POST /api/auth/login`          | bcrypt + JWT, 8-char min, optional `displayName`.                                                    |
| Recipes              | full CRUD under `/api/recipes`                             | Nested `ingredients[]` with raw text + optional ingredient/unit foreign keys. Steps as JSON array.   |
| Ingredient catalog   | `GET /api/ingredients?q=`                                  | Paginated case-insensitive search over a seeded 15-ingredient catalog with densities for scaling.    |
| Collections          | full CRUD + `POST/DELETE /collections/{id}/recipes/{rid}`  | One recipe can live in many collections (M:N join table with `position`).                            |
| Shopping lists       | full CRUD + per-item CRUD + `POST .../generate-from-recipes` | Generate copies every ingredient from selected recipes into one list. Toggles persist server-side. |
| OCR recipe import    | `POST /api/ocr/import` (multipart `image`)                 | Returns the saved recipe plus `unmatchedIngredients` / `unmatchedUnits` so the UI can offer fixes.   |

---

## Architecture

```
┌──────────────────────────┐         ┌──────────────────────────┐
│  TanStack Start (Vite)   │  HTTPS  │   Spring Boot API        │
│  React 19 + TS + Tailwind│ ◀─────▶ │   Java 21 + Spring 3.3   │
│  TanStack Router/Query   │   JWT   │   Spring Security + JJWT │
└──────────────────────────┘         └────────────┬─────────────┘
                                                  │ JDBC
                                                  ▼
                                     ┌──────────────────────────┐
                                     │   PostgreSQL 16          │
                                     │   (Supabase / Fly Postgres)│
                                     │   Flyway migrations      │
                                     └──────────────────────────┘
                                                  ▲
                                                  │ (OCR import only)
                                     ┌────────────┴─────────────┐
                                     │   Google Gemini API      │
                                     │   2.5 Flash, JSON mode   │
                                     └──────────────────────────┘
```

### Backend layout

```
backend/src/main/java/app/cookyourbooks/
├── CookYourBooksApplication.java
├── domain/         JPA entities (User, Recipe, Ingredient, Unit, Collection, …)
├── repository/     Spring Data JPA repositories
├── dto/            Request/response records grouped by feature
├── service/        Business logic, @Transactional boundaries
├── controller/     @RestController endpoints, validation, security helpers
├── adapter/        External integrations (Gemini OCR)
├── security/       JwtService, JwtAuthenticationFilter, AppUserDetailsService
├── config/         SecurityConfig, CORS, @ConfigurationProperties
└── exception/      Typed exceptions + @RestControllerAdvice handler
```

### Frontend layout

```
web/src/
├── routes/         File-based routes (index, login, register, recipes.$id, …)
├── components/     Navbar, RecipeCard + shadcn/ui primitives
├── lib/
│   ├── api.ts      Typed fetch client + all backend DTOs in one file
│   └── auth.ts     Token persistence + reactive useAuth hook
└── styles.css      Tailwind v4 + design tokens
```

---

## Notable design decisions

- **`ddl-auto: validate`, not `update`.** Schema drift between Hibernate and
  source-controlled SQL is the #1 reason "it worked on my machine" production
  surprises happen. Flyway owns the schema; Hibernate validates against it on
  boot.
- **Ingredients have `rawText` + optional FK.** The OCR pipeline may extract
  ingredients we don't recognize; instead of failing the import, we store the
  raw line and surface a list of unmatched names to the client so the user can
  reconcile later.
- **Density-aware conversions.** A `unit_conversions` row can be ingredient-
  scoped — "1 cup flour = 125 g" lives alongside the generic
  "1 cup = 236.59 mL". This lets the API answer scaling questions correctly
  for both mass and volume ingredients.
- **One typed namespace per resource.** The frontend `api.ts` exposes
  `authApi`, `recipesApi`, `collectionsApi`, `shoppingListsApi`, `ocrApi`.
  Route components stay small because they don't repeat URL strings or guess
  at response shapes.
- **Server-side toggle for shopping items.** The shopping list page
  optimistically renders, but every checkbox click `PUT`s the item; nothing
  is "phantom-checked" if the network drops. React Query handles invalidation.

---

## Running locally

### Prerequisites

- Java 21 (Temurin), Maven 3.9+
- Node 20+ and npm (or bun)
- PostgreSQL 16, or a Supabase project

### 1. Backend

```bash
cd backend
cp .env.local.example .env.local        # then edit with real values
set -a; source .env.local; set +a
mvn spring-boot:run
# → http://localhost:8080
```

Required env vars (full list with comments in
[`backend/.env.local.example`](backend/.env.local.example)):

| Variable                | Purpose                                           |
| ----------------------- | ------------------------------------------------- |
| `DATABASE_URL`          | JDBC URL                                          |
| `DATABASE_USERNAME`     | DB user                                           |
| `DATABASE_PASSWORD`     | DB password                                       |
| `JWT_SECRET`            | Base64 HMAC key — `openssl rand -base64 32`       |
| `CORS_ALLOWED_ORIGINS`  | e.g. `http://localhost:8081`                      |
| `GEMINI_API_KEY`        | Optional; needed for OCR import                   |

### 2. Frontend

```bash
cd web
npm install
npm run dev
# → http://localhost:8081
```

### Tests

```bash
cd backend
mvn test    # runs against H2 — no DB / API keys required
```

---

