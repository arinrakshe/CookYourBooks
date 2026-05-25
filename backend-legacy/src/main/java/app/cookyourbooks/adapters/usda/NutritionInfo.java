package app.cookyourbooks.adapters.usda;

/**
 * Nutritional data for a food item, per 100g.
 *
 * @param description the food description from USDA (e.g., "Chicken breast, raw")
 * @param calories energy in kcal
 * @param protein protein in grams
 * @param fat total fat in grams
 * @param carbohydrates carbohydrates in grams
 * @param fiber dietary fiber in grams
 */
public record NutritionInfo(
    String description,
    double calories,
    double protein,
    double fat,
    double carbohydrates,
    double fiber) {

  /**
   * Returns a new NutritionInfo scaled from per-100g values to the given amount in grams.
   *
   * @param grams the actual ingredient amount in grams
   * @return a new NutritionInfo with values scaled proportionally
   */
  public NutritionInfo scaleTo(double grams) {
    double factor = grams / 100.0;
    return new NutritionInfo(
        description,
        calories * factor,
        protein * factor,
        fat * factor,
        carbohydrates * factor,
        fiber * factor);
  }
}
