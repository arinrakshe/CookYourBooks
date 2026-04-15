package app.cookyourbooks.adapters.usda;

/** Thrown when the API rate limit is exceeded (HTTP 429). */
public final class UsdaRateLimitException extends UsdaException {

  public UsdaRateLimitException() {
    super("Rate limit exceeded");
  }
}
