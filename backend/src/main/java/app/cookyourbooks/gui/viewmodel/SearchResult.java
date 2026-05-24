package app.cookyourbooks.gui.viewmodel;

/**
 * A single search result entry displayed in the Search &amp; Filter view.
 *
 * @param id the unique recipe identifier
 * @param title the recipe title
 * @param collectionTitle the name of the collection containing this recipe
 */
public record SearchResult(String id, String title, String collectionTitle) {

  @Override
  public String toString() {
    return title + "  (" + collectionTitle + ")";
  }
}
