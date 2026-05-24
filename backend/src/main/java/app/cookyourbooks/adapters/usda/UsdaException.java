package app.cookyourbooks.adapters.usda;

/** Base exception for USDA API errors. Subtypes represent specific failure scenarios. */
public abstract class UsdaException extends Exception {

  protected UsdaException(String message) {
    super(message);
  }

  protected UsdaException(String message, Throwable cause) {
    super(message, cause);
  }
}
