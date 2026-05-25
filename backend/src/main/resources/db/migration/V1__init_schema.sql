-- CookYourBooks initial schema. PostgreSQL 16.
-- Tables: users, units, unit_conversions, ingredients, recipes, recipe_ingredients,
--         collections, recipe_collections, shopping_lists, shopping_list_items.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email_lower ON users (LOWER(email));

-- Units. kind ∈ {VOLUME, MASS, COUNT, OTHER}; system ∈ {METRIC, US, ANY}.
-- base_factor converts this unit to the base unit of its kind (ml for VOLUME, g for MASS, 1 for COUNT/OTHER).
CREATE TABLE units (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    plural      VARCHAR(64),
    kind        VARCHAR(16)  NOT NULL,
    system      VARCHAR(16)  NOT NULL,
    base_factor NUMERIC(18,9) NOT NULL,
    is_base     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT  units_kind_chk   CHECK (kind   IN ('VOLUME','MASS','COUNT','OTHER')),
    CONSTRAINT  units_system_chk CHECK (system IN ('METRIC','US','ANY'))
);

CREATE TABLE ingredients (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(160) NOT NULL UNIQUE,
    default_unit_id  BIGINT REFERENCES units(id),
    density_g_per_ml NUMERIC(10,6),
    usda_fdc_id      BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingredients_name_lower ON ingredients (LOWER(name));

-- Explicit conversion pairs. If ingredient_id IS NULL the row applies generically
-- (same-kind conversion). If set, it's a density-based cross-kind conversion.
CREATE TABLE unit_conversions (
    id            BIGSERIAL PRIMARY KEY,
    from_unit_id  BIGINT NOT NULL REFERENCES units(id),
    to_unit_id    BIGINT NOT NULL REFERENCES units(id),
    factor        NUMERIC(18,9) NOT NULL,
    ingredient_id BIGINT REFERENCES ingredients(id) ON DELETE CASCADE,
    note          VARCHAR(255),
    CONSTRAINT unit_conversions_distinct CHECK (from_unit_id <> to_unit_id),
    CONSTRAINT unit_conversions_unique UNIQUE (from_unit_id, to_unit_id, ingredient_id)
);

CREATE INDEX idx_unit_conversions_from ON unit_conversions (from_unit_id);
CREATE INDEX idx_unit_conversions_pair ON unit_conversions (from_unit_id, to_unit_id);

CREATE TABLE recipes (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    servings    NUMERIC(8,2),
    source_url  VARCHAR(1024),
    image_url   VARCHAR(1024),
    notes       TEXT,
    steps       TEXT,           -- JSON-encoded array of step strings
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recipes_user ON recipes (user_id);

CREATE TABLE recipe_ingredients (
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id BIGINT REFERENCES ingredients(id),
    raw_text      VARCHAR(512) NOT NULL,
    quantity      NUMERIC(12,4),
    unit_id       BIGINT REFERENCES units(id),
    preparation   VARCHAR(255),
    notes         VARCHAR(512),
    position      INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients (recipe_id);

CREATE TABLE collections (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT collections_user_name_unique UNIQUE (user_id, name)
);

CREATE TABLE recipe_collections (
    recipe_id     BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    collection_id BIGINT NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    position      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (recipe_id, collection_id)
);

CREATE TABLE shopping_lists (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE shopping_list_items (
    id               BIGSERIAL PRIMARY KEY,
    shopping_list_id BIGINT NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    recipe_id        BIGINT REFERENCES recipes(id) ON DELETE SET NULL,
    ingredient_id    BIGINT REFERENCES ingredients(id),
    raw_text         VARCHAR(512) NOT NULL,
    quantity         NUMERIC(12,4),
    unit_id          BIGINT REFERENCES units(id),
    checked          BOOLEAN NOT NULL DEFAULT FALSE,
    position         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_shopping_list_items_list ON shopping_list_items (shopping_list_id);
