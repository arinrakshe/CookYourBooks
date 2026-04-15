# Plan: Add Nutrition Info Popup for Ingredients via USDA API

## Context
The user wants to add a "Nutrition Info" button to each ingredient in the Recipe Editor. Clicking it queries the USDA FoodData Central API by ingredient name and shows a small popup with key nutritional data (calories, protein, fat, carbs, fiber per 100g). This follows the existing hexagonal adapter pattern used for the Gemini OCR integration.

## Architecture

```
Button click (IngredientCell)
  -> BackgroundTaskRunner.run(...)
    -> NutritionLookupAdapter (implements NutritionLookupService)
      -> RealUsdaClient (HttpClient + Jackson)
        -> POST https://api.nal.usda.gov/fdc/v1/foods/search
  -> onSuccess: show Dialog with NutrientInfo
  -> onFailure: show Alert with error message
```

## New Files

### Layer 1: USDA client (`src/main/java/app/cookyourbooks/adapters/usda/`)
1. **`NutrientInfo.java`** -- Record: `description`, `calories`, `protein`, `fat`, `carbs`, `fiber` (all doubles, per 100g)
2. **`UsdaClient.java`** -- Interface: `NutrientInfo lookupNutrients(String query)`
3. **`RealUsdaClient.java`** -- Implementation using `HttpClient` + Jackson. POSTs to `/foods/search` with `pageSize: 1`, `dataType: ["SR Legacy", "Foundation"]`. Extracts nutrients by ID (1008=calories, 1003=protein, 1004=fat, 1005=carbs, 1079=fiber). Mirrors `RealGeminiClient` patterns.
4. **Exception hierarchy** (mirrors Gemini): `UsdaException`, `UsdaAuthException`, `UsdaRateLimitException`, `UsdaServerException`, `UsdaTimeoutException`, `UsdaNetworkException`, `UsdaParseException`

### Layer 2: Core port (`src/main/java/app/cookyourbooks/services/nutrition/`)
5. **`NutritionLookupService.java`** -- Interface: `NutrientInfo lookup(String ingredientName)`
6. **`NutritionLookupException.java`** -- Checked exception

### Layer 3: Adapter (`src/main/java/app/cookyourbooks/adapters/`)
7. **`NutritionLookupAdapter.java`** -- Implements `NutritionLookupService`, wraps `UsdaClient`, translates exceptions to user-friendly messages

## Modified Files

8. **`RecipeEditorViewController.java`** -- Add `NutritionLookupService` to constructor. Add "Nutrition" button to `IngredientCell` rows. Button triggers async lookup via `BackgroundTaskRunner`, shows Dialog on success or Alert on error.
9. **`CookYourBooksGuiApp.java`** -- Wire `RealUsdaClient` (reads `USDA_API_KEY` env var, falls back to `DEMO_KEY`) -> `NutritionLookupAdapter` -> pass to `RecipeEditorViewController`.

## Implementation Order

| Step | What | Why |
|------|------|-----|
| 1 | `NutrientInfo` record | Zero dependencies, foundation for everything |
| 2 | Exception hierarchy (7 classes) | Copy pattern from Gemini exceptions |
| 3 | `UsdaClient` interface | Define the contract |
| 4 | `RealUsdaClient` implementation | Core API logic -- HTTP, JSON parsing, error mapping |
| 5 | `NutritionLookupException` + `NutritionLookupService` interface | Core port |
| 6 | `NutritionLookupAdapter` | Glue layer |
| 7 | Modify `RecipeEditorViewController` -- add button + popup | UI integration |
| 8 | Modify `CookYourBooksGuiApp` -- wire everything | Final wiring |
| 9 | Unit tests for JSON parsing + exception mapping | Testable without network |

## Edge Cases

- **No results**: Show info alert "No nutrition data found for 'xyz'"
- **Loading**: Disable button, change text to "Loading..."
- **API errors**: Adapter maps each exception to a friendly message in an error Alert
- **Blank name**: Button disabled/hidden for empty ingredient rows
- **Vague ingredients**: Still allow lookup ("salt" is a valid USDA query)

## Popup Design

```
+------------------------------------+
|  Nutrition Info                  [X]|
|------------------------------------|
|  Chicken breast, raw               |
|  Per 100g:                         |
|  Calories    120 kcal              |
|  Protein     22.5 g               |
|  Fat          2.6 g               |
|  Carbs        0.0 g               |
|  Fiber        0.0 g               |
|  Source: USDA FoodData Central     |
|                          [ Close ] |
+------------------------------------+
```

## Key Reference Files (templates to follow)
- `src/main/java/app/cookyourbooks/adapters/gemini/RealGeminiClient.java` -- HTTP client pattern
- `src/main/java/app/cookyourbooks/adapters/GeminiOcrAdapter.java` -- adapter/exception translation pattern
- `src/main/java/app/cookyourbooks/gui/BackgroundTaskRunner.java` -- async utility
- `src/main/java/app/cookyourbooks/gui/view/RecipeEditorViewController.java` -- where button is added

## Verification
1. `./gradlew build` -- compiles and passes checkstyle/spotless
2. Unit tests pass for JSON parsing and exception mapping
3. Run the app, open a recipe, click "Nutrition" on an ingredient, see the popup with real data
4. Test error case: unset API key, verify friendly error message
