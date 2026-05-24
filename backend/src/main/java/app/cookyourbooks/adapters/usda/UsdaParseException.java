package app.cookyourbooks.adapters.usda;

/** Thrown when the API response cannot be parsed as valid JSON. */
public final class UsdaParseException extends UsdaException {

  public UsdaParseException(String message) {
    super(message);
  }

  public UsdaParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
