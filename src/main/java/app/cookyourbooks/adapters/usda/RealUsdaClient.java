package app.cookyourbooks.adapters.usda;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Real implementation of {@link UsdaClient} that calls the USDA FoodData Central API.
 *
 * <p>Uses the {@code /foods/search} endpoint to find foods by name and extract nutrient values from
 * the response. Nutrient IDs: 1008 (calories), 1003 (protein), 1004 (fat), 1005 (carbs), 1079
 * (fiber).
 */
public final class RealUsdaClient implements UsdaClient {

  private static final Logger LOG = LoggerFactory.getLogger(RealUsdaClient.class);
  private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";

  private static final int NUTRIENT_ID_CALORIES = 1008;
  private static final int NUTRIENT_ID_PROTEIN = 1003;
  private static final int NUTRIENT_ID_FAT = 1004;
  private static final int NUTRIENT_ID_CARBS = 1005;
  private static final int NUTRIENT_ID_FIBER = 1079;

  private final @Nullable String apiKey;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  /**
   * Constructs a new RealUsdaClient.
   *
   * @param apiKey the USDA API key (from USDA_API_KEY env var), may be null
   */
  public RealUsdaClient(@Nullable String apiKey) {
    this.apiKey = apiKey;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  }

  @Override
  public NutritionInfo getNutritionInfo(String foodName) throws UsdaException {
    if (apiKey == null || apiKey.isBlank()) {
      throw new UsdaAuthException();
    }

    String encodedQuery = URLEncoder.encode(foodName, StandardCharsets.UTF_8);
    String url =
        BASE_URL
            + "?query="
            + encodedQuery
            + "&pageSize=1"
            + "&dataType=SR%20Legacy,Foundation"
            + "&api_key="
            + apiKey;

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

    LOG.debug("Calling USDA API for food: {}", foodName);

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();

      if (status == 401 || status == 403) {
        throw new UsdaAuthException();
      }
      if (status == 429) {
        throw new UsdaRateLimitException();
      }
      if (status >= 500) {
        throw new UsdaServerException(status);
      }
      if (status != 200) {
        throw new UsdaParseException("API error: HTTP " + status);
      }

      return parseNutritionInfo(response.body());
    } catch (UsdaException e) {
      throw e;
    } catch (java.net.http.HttpTimeoutException e) {
      throw new UsdaTimeoutException();
    } catch (IOException e) {
      throw new UsdaNetworkException("Network error while contacting USDA API", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new UsdaNetworkException("Request interrupted", e);
    }
  }

  /**
   * Parses the USDA search response JSON and extracts nutrition info from the first result.
   *
   * @param json the raw JSON response body
   * @return parsed NutritionInfo
   * @throws UsdaParseException if the JSON is malformed or contains no results
   */
  NutritionInfo parseNutritionInfo(String json) throws UsdaParseException {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode foods = root.path("foods");

      if (!foods.isArray() || foods.isEmpty()) {
        throw new UsdaParseException("No foods found for the given query");
      }

      JsonNode food = foods.get(0);
      String description = food.path("description").asText("Unknown");
      JsonNode nutrients = food.path("foodNutrients");

      double calories = 0;
      double protein = 0;
      double fat = 0;
      double carbs = 0;
      double fiber = 0;

      for (JsonNode nutrient : nutrients) {
        int id = nutrient.path("nutrientId").asInt();
        double value = nutrient.path("value").asDouble(0.0);

        if (id == NUTRIENT_ID_CALORIES) {
          calories = value;
        } else if (id == NUTRIENT_ID_PROTEIN) {
          protein = value;
        } else if (id == NUTRIENT_ID_FAT) {
          fat = value;
        } else if (id == NUTRIENT_ID_CARBS) {
          carbs = value;
        } else if (id == NUTRIENT_ID_FIBER) {
          fiber = value;
        }
      }

      LOG.debug(
          "Parsed nutrition for '{}': cal={}, protein={}, fat={}, carbs={}, fiber={}",
          description,
          calories,
          protein,
          fat,
          carbs,
          fiber);

      return new NutritionInfo(description, calories, protein, fat, carbs, fiber);
    } catch (UsdaParseException e) {
      throw e;
    } catch (IOException | IllegalArgumentException e) {
      throw new UsdaParseException("Failed to parse USDA API response", e);
    }
  }
}
