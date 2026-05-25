package app.cookyourbooks.gui.viewmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.util.Duration;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.UnitSystem;
import app.cookyourbooks.services.LibrarianService;

/**
 * Implementation of {@link SearchViewModel}.
 *
 * <p>Debounces search input by 300 ms, runs the query on a background thread via {@link
 * BackgroundTaskRunner}, and applies ingredient filters using AND (intersection) logic. A
 * generation counter is used to discard results from stale (superseded) searches.
 */
public class SearchViewModelImpl implements SearchViewModel {

  private final LibrarianService librarianService;
  private final NavigationService navigationService;

  private final StringProperty query = new SimpleStringProperty("");
  private final ObservableList<SearchResult> results = FXCollections.observableArrayList();
  private final ObservableList<String> ingredientFilters = FXCollections.observableArrayList();
  private final BooleanProperty searching = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");
  private final ReadOnlyIntegerWrapper selectedIndex = new ReadOnlyIntegerWrapper(-1);

  // Debounce: each call to setQuery/addIngredientFilter restarts this timer.
  private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

  // Race-condition guard: incremented before each search; stale callbacks are dropped.
  private final AtomicInteger searchGeneration = new AtomicInteger(0);

  // Holds the most recent background task so Error Prone sees the return value is used.
  @SuppressWarnings("unused")
  private @Nullable Task<?> currentSearchTask;

  public SearchViewModelImpl(
      LibrarianService librarianService, NavigationService navigationService) {
    this.librarianService = librarianService;
    this.navigationService = navigationService;
    debounce.setOnFinished(e -> runSearch());
    navigationService.unitSystemProperty().addListener((obs, oldValue, newValue) -> {});
  }

  // ── Observable properties ─────────────────────────────────────────────────

  @Override
  public StringProperty queryProperty() {
    return query;
  }

  @Override
  public ObservableList<SearchResult> resultsProperty() {
    return results;
  }

  @Override
  public ObservableList<String> ingredientFiltersProperty() {
    return ingredientFilters;
  }

  @Override
  public BooleanProperty searchingProperty() {
    return searching;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  /**
   * The index of the currently selected result (-1 means no selection). The View listens to this to
   * keep the ListView selection in sync with keyboard navigation.
   */
  public ReadOnlyIntegerProperty selectedIndexProperty() {
    return selectedIndex.getReadOnlyProperty();
  }

  // ── Commands ──────────────────────────────────────────────────────────────

  @Override
  public void setQuery(String q) {
    query.set(q);
    debounce.playFromStart();
  }

  @Override
  public void addIngredientFilter(String ingredient) {
    if (!ingredient.isBlank() && !ingredientFilters.contains(ingredient)) {
      ingredientFilters.add(ingredient);
      runSearch();
    }
  }

  @Override
  public void removeIngredientFilter(String ingredient) {
    if (ingredientFilters.remove(ingredient)) {
      runSearch();
    }
  }

  @Override
  public void clearFilters() {
    debounce.stop();
    query.set("");
    ingredientFilters.clear();
    results.clear();
    selectedIndex.set(-1);
    statusMessage.set("");
  }

  @Override
  public void selectNextResult() {
    if (results.isEmpty()) {
      return;
    }
    int next = (selectedIndex.get() + 1) % results.size();
    selectedIndex.set(next);
  }

  @Override
  public void selectPreviousResult() {
    if (results.isEmpty()) {
      return;
    }
    int prev = selectedIndex.get() <= 0 ? results.size() - 1 : selectedIndex.get() - 1;
    selectedIndex.set(prev);
  }

  @Override
  public void navigateToSelectedResult() {
    int idx = selectedIndex.get();
    if (idx >= 0 && idx < results.size()) {
      navigationService.navigateToRecipe(results.get(idx).id());
    }
  }

  // ── Non-JavaFX accessors (for grading tests) ──────────────────────────────

  @Override
  public String getQuery() {
    return query.get();
  }

  @Override
  public List<String> getResultIds() {
    return results.stream().map(SearchResult::id).toList();
  }

  @Override
  public List<String> getIngredientFilters() {
    return List.copyOf(ingredientFilters);
  }

  @Override
  public boolean isSearching() {
    return searching.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  @Override
  public @Nullable String getSelectedResultId() {
    int idx = selectedIndex.get();
    if (idx >= 0 && idx < results.size()) {
      return results.get(idx).id();
    }
    return null;
  }

  // ── Internal: async search ─────────────────────────────────────────────────

  private void runSearch() {
    int myGeneration = searchGeneration.incrementAndGet();
    String currentQuery = query.get();
    List<String> currentFilters = List.copyOf(ingredientFilters);

    searching.set(true);
    statusMessage.set("Searching...");

    currentSearchTask =
        BackgroundTaskRunner.run(
            () -> doSearch(currentQuery, currentFilters),
            searchResults -> {
              // Discard if a newer search has already been started.
              if (myGeneration != searchGeneration.get()) {
                return;
              }
              results.setAll(searchResults);
              selectedIndex.set(searchResults.isEmpty() ? -1 : 0);
              int count = searchResults.size();
              statusMessage.set(
                  count == 0 ? "No results found" : count + " result" + (count == 1 ? "" : "s"));
              searching.set(false);
            },
            error -> {
              if (myGeneration != searchGeneration.get()) {
                return;
              }
              statusMessage.set("Search failed: " + error.getMessage());
              searching.set(false);
            });
  }

  /**
   * Runs on the background thread. Builds the result list using title search plus ingredient filter
   * intersection (AND logic).
   */
  private List<SearchResult> doSearch(String q, List<String> filters) {
    // Step 1: get the base result set from title search (or all recipes if blank).
    List<Recipe> base;
    if (q.isBlank()) {
      base = librarianService.listAllRecipes();
    } else {
      base = librarianService.resolveRecipes(q);
    }

    // Step 2: apply each ingredient filter as an AND intersection.
    for (String filter : filters) {
      Set<String> matchIds = new HashSet<>();
      for (Recipe r : librarianService.searchByIngredient(filter)) {
        matchIds.add(r.getId());
      }
      base = base.stream().filter(r -> matchIds.contains(r.getId())).toList();
    }

    // Step 3: build SearchResult entries with collection context.
    List<RecipeCollection> collections = librarianService.listCollections();
    List<SearchResult> output = new ArrayList<>(base.size());
    for (Recipe recipe : base) {
      String collectionTitle =
          collections.stream()
              .filter(c -> c.containsRecipe(recipe.getId()))
              .map(RecipeCollection::getTitle)
              .findFirst()
              .orElse("Unknown");
      output.add(new SearchResult(recipe.getId(), recipe.getTitle(), collectionTitle));
    }
    return output;
  }

  public UnitSystem getUnitSystem() {
    return navigationService.getUnitSystem();
  }
}
