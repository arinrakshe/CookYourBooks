package app.cookyourbooks.services.nutrition;

import app.cookyourbooks.adapters.usda.NutritionInfo;

/** Service for looking up nutrition information for ingredients. */
public interface NutritionLookupService {

  /**
   * Looks up nutrition information for the given ingredient, scaled to the specified amount.
   *
   * <p>The amount and unit are converted to grams, then used to scale the per-100g values from the
   * USDA API. If the unit cannot be converted to grams (e.g., volume units like cups), the result
   * is returned per 100g.
   *
   * @param ingredientName the name of the ingredient to look up
   * @param amount the quantity of the ingredient
   * @param unit the unit abbreviation (e.g., "g", "kg", "oz", "lb")
   * @return a NutritionInfo object with values scaled to the given amount
   * @throws NutritionLookupException if there was an error during lookup
   */
  NutritionInfo lookup(String ingredientName, double amount, String unit)
      throws NutritionLookupException;
}
