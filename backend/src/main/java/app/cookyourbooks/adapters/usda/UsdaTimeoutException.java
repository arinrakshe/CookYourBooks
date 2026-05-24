package app.cookyourbooks.adapters.usda;

/** Thrown when the request to the USDA API times out. */
public final class UsdaTimeoutException extends UsdaException {

  public UsdaTimeoutException() {
    super("Request timed out");
  }
}
