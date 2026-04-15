package app.cookyourbooks.adapters.usda;

/** Thrown when the API key is invalid or missing (HTTP 401/403). */
public final class UsdaAuthException extends UsdaException {

  public UsdaAuthException() {
    super("Invalid or missing USDA API key");
  }
}
