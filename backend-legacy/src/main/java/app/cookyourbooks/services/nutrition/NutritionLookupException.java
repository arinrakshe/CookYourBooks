package app.cookyourbooks.services.nutrition;

/** Exception thrown when there is an error during nutrition lookup. */
public class NutritionLookupException extends Exception {

  public NutritionLookupException(String message) {
    super(message);
  }

  public NutritionLookupException(String message, Throwable cause) {
    super(message, cause);
  }
}
