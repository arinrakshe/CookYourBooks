

# CookYourBooks — Claude Instructions

## Stack
- Backend: Java 21, Spring Boot 3.3.5, Maven, PostgreSQL 16, Flyway, JWT (JJWT 0.12.6), Lombok
- Frontend: Next.js 14 (App Router), Tailwind CSS, TypeScript
- Deploy: Fly.io (backend), Vercel (frontend)

## Rules
- Never touch backend-legacy/ — it is read-only reference only
- Always use Lombok (@Getter, @Setter, @Builder, @RequiredArgsConstructor) on entities/DTOs
- All DB schema changes go through Flyway migrations in db/migration/
- ddl-auto is set to validate — never change this
- Follow existing domain model in domain/ and repositories in repository/ exactly
- DTOs go in dto/, adapters go in adapter/, exceptions go in exception/
- Frontend lives in web/ — use Next.js App Router, Tailwind, TypeScript
- API base URL for frontend: http://localhost:8080

## Priority Order
1. Fix CI workflow (maven)
2. Build service + controller layers (backend)
3. Build Next.js frontend (web/)