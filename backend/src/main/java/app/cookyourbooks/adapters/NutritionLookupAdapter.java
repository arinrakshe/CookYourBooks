package app.cookyourbooks.adapters;

import java.util.Locale;

import app.cookyourbooks.adapters.usda.NutritionInfo;
import app.cookyourbooks.adapters.usda.UsdaAuthException;
import app.cookyourbooks.adapters.usda.UsdaClient;
import app.cookyourbooks.adapters.usda.UsdaException;
import app.cookyourbooks.adapters.usda.UsdaNetworkException;
import app.cookyourbooks.adapters.usda.UsdaRateLimitException;
import app.cookyourbooks.adapters.usda.UsdaTimeoutException;
import app.cookyourbooks.services.nutrition.NutritionLookupException;
import app.cookyourbooks.services.nutrition.NutritionLookupService;

/**
 * Adapter that implements {@link NutritionLookupService} by delegating to a {@link UsdaClient}.
 *
 * <p>Converts weight-based units to grams for scaling, and translates USDA-specific exceptions into
 * user-friendly {@link NutritionLookupException} messages.
 */
public final class NutritionLookupAdapter implements NutritionLookupService {

  private final UsdaClient usdaClient;

  public NutritionLookupAdapter(UsdaClient usdaClient) {
    this.usdaClient = usdaClient;
  }

  @Override
  public NutritionInfo lookup(String ingredientName, double amount, String unit)
      throws NutritionLookupException {
    try {
      NutritionInfo per100g = usdaClient.getNutritionInfo(ingredientName);
      double grams = toGrams(amount, unit);
      if (grams > 0) {
        return per100g.scaleTo(grams);
      }
      // Can't convert to grams (volume/count/vague units) — return per 100g as-is
      return per100g;
    } catch (UsdaAuthException e) {
      throw new NutritionLookupException(
          "USDA API key error. Set the USDA_API_KEY environment variable.", e);
    } catch (UsdaRateLimitException e) {
      throw new NutritionLookupException("Too many requests. Wait a moment and try again.", e);
    } catch (UsdaTimeoutException e) {
      throw new NutritionLookupException("Request timed out. Check your internet connection.", e);
    } catch (UsdaNetworkException e) {
      throw new NutritionLookupException("Network error. Check your internet connection.", e);
    } catch (UsdaException e) {
      throw new NutritionLookupException("Failed to look up nutrition information.", e);
    }
  }

  /**
   * Converts an amount in the given unit to grams. Returns -1 if the unit is not a weight unit
   * (volume, count, and vague units cannot be reliably converted without ingredient-specific
   * density).
   */
  private static double toGrams(double amount, String unit) {
    String normalized = unit.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "g" -> amount;
      case "kg" -> amount * 1000.0;
      case "oz" -> amount * 28.3495;
      case "lb" -> amount * 453.592;
      default -> -1.0;
    };
  }
}
