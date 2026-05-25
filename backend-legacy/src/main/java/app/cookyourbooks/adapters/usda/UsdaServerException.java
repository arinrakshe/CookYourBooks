package app.cookyourbooks.adapters.usda;

/** Thrown when the USDA API returns a server error (HTTP 5xx). */
public final class UsdaServerException extends UsdaException {

  private final int statusCode;

  public UsdaServerException(int statusCode) {
    super("Server error: HTTP " + statusCode);
    this.statusCode = statusCode;
  }

  /** Returns the HTTP status code (e.g., 500, 503). */
  public int getStatusCode() {
    return statusCode;
  }
}
