package app.cookyourbooks.adapters.usda;

/** Thrown for other network-related errors (connection refused, DNS failure, etc.). */
public final class UsdaNetworkException extends UsdaException {

  public UsdaNetworkException(String message, Throwable cause) {
    super(message, cause);
  }
}
