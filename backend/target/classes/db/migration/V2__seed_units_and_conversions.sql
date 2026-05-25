-- Seed cooking units (VOLUME base = ml, MASS base = g, COUNT base = piece)
-- and 50+ unit conversions. Density-based conversions are tied to specific
-- ingredients so volume↔mass is only used when chemistry makes sense.

INSERT INTO units (code, name, plural, kind, system, base_factor, is_base) VALUES
    -- VOLUME (METRIC)
    ('ml',      'milliliter', 'milliliters', 'VOLUME', 'METRIC', 1.0,            TRUE),
    ('l',       'liter',      'liters',      'VOLUME', 'METRIC', 1000.0,         FALSE),
    -- VOLUME (US)
    ('tsp',     'teaspoon',   'teaspoons',   'VOLUME', 'US',     4.928921875,    FALSE),
    ('tbsp',    'tablespoon', 'tablespoons', 'VOLUME', 'US',     14.78676563,    FALSE),
    ('fl_oz',   'fluid ounce','fluid ounces','VOLUME', 'US',     29.57353125,    FALSE),
    ('cup',     'cup',        'cups',        'VOLUME', 'US',     236.5882500,    FALSE),
    ('pint',    'pint',       'pints',       'VOLUME', 'US',     473.1765000,    FALSE),
    ('quart',   'quart',      'quarts',      'VOLUME', 'US',     946.3530000,    FALSE),
    ('gallon',  'gallon',     'gallons',     'VOLUME', 'US',     3785.4120000,   FALSE),
    -- VOLUME (ANY)
    ('pinch',   'pinch',      'pinches',     'VOLUME', 'ANY',    0.30805762,     FALSE),
    ('dash',    'dash',       'dashes',      'VOLUME', 'ANY',    0.61611523,     FALSE),
    ('drop',    'drop',       'drops',       'VOLUME', 'ANY',    0.05,           FALSE),

    -- MASS (METRIC)
    ('g',       'gram',      'grams',      'MASS', 'METRIC', 1.0,         TRUE),
    ('kg',      'kilogram',  'kilograms',  'MASS', 'METRIC', 1000.0,      FALSE),
    ('mg',      'milligram', 'milligrams', 'MASS', 'METRIC', 0.001,       FALSE),
    -- MASS (US)
    ('oz',      'ounce',     'ounces',     'MASS', 'US',     28.34952,    FALSE),
    ('lb',      'pound',     'pounds',     'MASS', 'US',     453.59237,   FALSE),
    -- Butter "stick" is conventionally 113g; treated as MASS so it converts cleanly.
    ('stick',   'stick',     'sticks',     'MASS', 'US',     113.39809,   FALSE),

    -- COUNT (ANY)
    ('piece',   'piece',     'pieces',     'COUNT', 'ANY', 1.0, TRUE),
    ('clove',   'clove',     'cloves',     'COUNT', 'ANY', 1.0, FALSE),
    ('slice',   'slice',     'slices',     'COUNT', 'ANY', 1.0, FALSE),
    ('can',     'can',       'cans',       'COUNT', 'ANY', 1.0, FALSE),
    ('package', 'package',   'packages',   'COUNT', 'ANY', 1.0, FALSE),
    ('bunch',   'bunch',     'bunches',    'COUNT', 'ANY', 1.0, FALSE),
    ('head',    'head',      'heads',      'COUNT', 'ANY', 1.0, FALSE),

    -- OTHER
    ('to_taste','to taste',  'to taste',   'OTHER', 'ANY', 1.0, FALSE);


-- Generic same-kind conversions. ingredient_id is NULL, meaning the conversion
-- works regardless of ingredient.
INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor, ingredient_id, note)
SELECT f.id, t.id, c.factor, NULL, c.note FROM (VALUES
    -- VOLUME ↔ VOLUME (US system)
    ('cup',    'tbsp',  16.0,         'US cup to tablespoons'),
    ('cup',    'tsp',   48.0,         'US cup to teaspoons'),
    ('cup',    'fl_oz', 8.0,          'US cup to fluid ounces'),
    ('cup',    'ml',    236.5882500,  'US cup to milliliters'),
    ('tbsp',   'tsp',   3.0,          'tablespoon to teaspoons'),
    ('tbsp',   'fl_oz', 0.5,          'tablespoon to fluid ounce'),
    ('tbsp',   'ml',    14.78676563,  'tablespoon to milliliters'),
    ('tsp',    'ml',    4.928921875,  'teaspoon to milliliters'),
    ('fl_oz',  'ml',    29.57353125,  'fluid ounce to milliliters'),
    ('pint',   'cup',   2.0,          'pint to cups'),
    ('pint',   'fl_oz', 16.0,         'pint to fluid ounces'),
    ('pint',   'ml',    473.1765000,  'pint to milliliters'),
    ('quart',  'cup',   4.0,          'quart to cups'),
    ('quart',  'pint',  2.0,          'quart to pints'),
    ('quart',  'ml',    946.3530000,  'quart to milliliters'),
    ('gallon', 'quart', 4.0,          'gallon to quarts'),
    ('gallon', 'cup',   16.0,         'gallon to cups'),
    ('gallon', 'ml',    3785.4120000, 'gallon to milliliters'),
    -- VOLUME ↔ VOLUME (METRIC)
    ('l',      'ml',    1000.0,       'liter to milliliters'),
    ('l',      'cup',   4.22675284,   'liter to US cups'),
    -- Small VOLUMES
    ('pinch',  'tsp',   0.0625,       'pinch = 1/16 teaspoon'),
    ('dash',   'tsp',   0.125,        'dash = 1/8 teaspoon'),
    ('drop',   'ml',    0.05,         'drop ≈ 0.05 mL'),

    -- MASS ↔ MASS (US ↔ Metric)
    ('lb',     'oz',    16.0,         'pound to ounces'),
    ('lb',     'g',     453.59237,    'pound to grams'),
    ('lb',     'kg',    0.45359237,   'pound to kilograms'),
    ('oz',     'g',     28.34952,     'ounce to grams'),
    ('kg',     'g',     1000.0,       'kilogram to grams'),
    ('kg',     'oz',    35.2739619,   'kilogram to ounces'),
    ('kg',     'lb',    2.20462262,   'kilogram to pounds'),
    ('mg',     'g',     0.001,        'milligram to gram'),
    ('stick',  'g',     113.39809,    'stick of butter to grams'),
    ('stick',  'oz',    4.0,          'stick of butter to ounces')
) AS c(from_code, to_code, factor, note)
JOIN units f ON f.code = c.from_code
JOIN units t ON t.code = c.to_code;


-- Common ingredients (with densities in g/ml). Inserted now so we can attach
-- ingredient-specific density conversions to them.
INSERT INTO ingredients (name, default_unit_id, density_g_per_ml) VALUES
    ('all-purpose flour', (SELECT id FROM units WHERE code = 'cup'), 0.529),
    ('granulated sugar',  (SELECT id FROM units WHERE code = 'cup'), 0.845),
    ('brown sugar',       (SELECT id FROM units WHERE code = 'cup'), 0.930),
    ('powdered sugar',    (SELECT id FROM units WHERE code = 'cup'), 0.510),
    ('butter',            (SELECT id FROM units WHERE code = 'cup'), 0.960),
    ('water',             (SELECT id FROM units WHERE code = 'cup'), 1.000),
    ('whole milk',        (SELECT id FROM units WHERE code = 'cup'), 1.030),
    ('vegetable oil',     (SELECT id FROM units WHERE code = 'cup'), 0.920),
    ('olive oil',         (SELECT id FROM units WHERE code = 'cup'), 0.913),
    ('honey',             (SELECT id FROM units WHERE code = 'tbsp'), 1.420),
    ('table salt',        (SELECT id FROM units WHERE code = 'tsp'), 1.200),
    ('kosher salt',       (SELECT id FROM units WHERE code = 'tsp'), 0.850),
    ('baking powder',     (SELECT id FROM units WHERE code = 'tsp'), 0.900),
    ('baking soda',       (SELECT id FROM units WHERE code = 'tsp'), 0.933),
    ('cocoa powder',      (SELECT id FROM units WHERE code = 'cup'), 0.420);


-- Density-based VOLUME → MASS conversions (cup → g and tbsp → g per ingredient).
-- cup→g = density * 236.5882500;  tbsp→g = density * 14.78676563.
INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor, ingredient_id, note)
SELECT
    (SELECT id FROM units WHERE code = 'cup'),
    (SELECT id FROM units WHERE code = 'g'),
    ROUND((i.density_g_per_ml * 236.5882500)::numeric, 4),
    i.id,
    'density-based: 1 cup of ' || i.name || ' in grams'
FROM ingredients i
WHERE i.density_g_per_ml IS NOT NULL;

INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor, ingredient_id, note)
SELECT
    (SELECT id FROM units WHERE code = 'tbsp'),
    (SELECT id FROM units WHERE code = 'g'),
    ROUND((i.density_g_per_ml * 14.78676563)::numeric, 4),
    i.id,
    'density-based: 1 tbsp of ' || i.name || ' in grams'
FROM ingredients i
WHERE i.density_g_per_ml IS NOT NULL;
