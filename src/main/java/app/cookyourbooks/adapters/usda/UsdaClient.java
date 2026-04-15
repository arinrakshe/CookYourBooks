package app.cookyourbooks.adapters.usda;

/**
 * Client for fetching nutrition information from the USDA FoodData Central API.
 *
 * <p>Implementations handle HTTP communication and JSON parsing. Callers receive a {@link
 * NutritionInfo} record with key nutrient values.
 */
public interface UsdaClient {

  /**
   * Fetches the nutrition information for a given food item.
   *
   * @param foodName the name of the food item to search for
   * @return a NutritionInfo object containing the nutrition information for the food item
   * @throws UsdaAuthException for HTTP 401/403 (invalid or missing API key)
   * @throws UsdaRateLimitException for HTTP 429 (rate limited)
   * @throws UsdaServerException for HTTP 5xx (server error)
   * @throws UsdaTimeoutException if request times out
   * @throws UsdaNetworkException for other network errors
   * @throws UsdaParseException if the API response cannot be parsed
   */
  NutritionInfo getNutritionInfo(String foodName) throws UsdaException;
}
