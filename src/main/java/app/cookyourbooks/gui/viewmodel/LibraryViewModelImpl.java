package app.cookyourbooks.gui.viewmodel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.services.LibrarianService;

/** Default implementation of {@link LibraryViewModel}. */
public final class LibraryViewModelImpl implements LibraryViewModel {

  private static final Logger LOG = LoggerFactory.getLogger(LibraryViewModelImpl.class);

  private final LibrarianService librarianService;
  private final NavigationService navigationService;
  private final Duration undoTimeout;

  private final ObservableList<LibraryCollectionRow> collections =
      FXCollections.observableArrayList();
  private final ObservableList<LibraryRecipeRow> recipes = FXCollections.observableArrayList();
  private final BooleanProperty loading = new SimpleBooleanProperty(false);
  private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
  private final StringProperty undoMessage = new SimpleStringProperty("");
  private final StringProperty filterText = new SimpleStringProperty("");

  /** Summaries from the last successful {@link #refresh()}, before filter and pending-delete. */
  private List<LibraryCollectionRow> allRows = List.of();

  private @Nullable String selectedCollectionId;

  private final PauseTransition undoTimer;
  private @Nullable String pendingDeletionId;

  public LibraryViewModelImpl(
      LibrarianService librarianService,
      NavigationService navigationService,
      Duration undoTimeout) {
    this.librarianService = librarianService;
    this.navigationService = navigationService;
    this.undoTimeout = undoTimeout;
    this.undoTimer = new PauseTransition();
    this.undoTimer.setDuration(javafx.util.Duration.millis(Math.max(1, undoTimeout.toMillis())));
    this.undoTimer.setOnFinished(
        e -> {
          if (pendingDeletionId != null) {
            String toDelete = pendingDeletionId;
            librarianService.deleteCollection(toDelete);
            pendingDeletionId = null;
            undoAvailable.set(false);
            undoMessage.set("");
            removeFromAllRows(toDelete);
            rebuildFilteredCollections();
            reconcileSelectionAfterDataChange();
          }
        });

    filterText.addListener((obs, o, n) -> rebuildFilteredCollections());
  }

  @Override
  public ObservableList<?> collectionsProperty() {
    return collections;
  }

  @Override
  public StringProperty filterTextProperty() {
    return filterText;
  }

  @Override
  public ObservableList<?> recipesProperty() {
    return recipes;
  }

  @Override
  public BooleanProperty loadingProperty() {
    return loading;
  }

  @Override
  public BooleanProperty undoAvailableProperty() {
    return undoAvailable;
  }

  @Override
  public StringProperty undoMessageProperty() {
    return undoMessage;
  }

  @Override
  public void refresh() {
    loading.set(true);
    BackgroundTaskRunner.run(
        librarianService::listCollections,
        this::onRefreshSuccess,
        error -> {
          LOG.warn("refresh failed", error);
          loading.set(false);
        });
  }

  private void onRefreshSuccess(List<RecipeCollection> fromService) {
    List<LibraryCollectionRow> next = new ArrayList<>(fromService.size());
    for (RecipeCollection c : fromService) {
      next.add(
          new LibraryCollectionRow(
              c.getId(), c.getTitle(), c.getSourceType(), c.getRecipes().size()));
    }
    allRows = List.copyOf(next);
    rebuildFilteredCollections();
    reconcileSelectionAfterDataChange();
    loading.set(false);
  }

  private void reconcileSelectionAfterDataChange() {
    if (selectedCollectionId == null) {
      return;
    }
    if (pendingDeletionId != null && pendingDeletionId.equals(selectedCollectionId)) {
      clearSelection();
      return;
    }
    Optional<RecipeCollection> coll = librarianService.findCollectionById(selectedCollectionId);
    if (coll.isEmpty()) {
      clearSelection();
    } else {
      populateRecipes(coll.get());
    }
  }

  private void clearSelection() {
    selectedCollectionId = null;
    recipes.clear();
  }

  @Override
  public void selectCollection(String collectionId) {
    Optional<RecipeCollection> coll = librarianService.findCollectionById(collectionId);
    if (coll.isEmpty()) {
      clearSelection();
      return;
    }
    if (pendingDeletionId != null && pendingDeletionId.equals(collectionId)) {
      clearSelection();
      return;
    }
    selectedCollectionId = collectionId;
    populateRecipes(coll.get());
  }

  private void populateRecipes(RecipeCollection collection) {
    List<LibraryRecipeRow> rows = new ArrayList<>();
    for (Recipe r : collection.getRecipes()) {
      rows.add(new LibraryRecipeRow(r.getId(), r.getTitle()));
    }
    recipes.setAll(rows);
  }

  @Override
  public void createCollection(String title) {
    if (title == null || title.isBlank()) {
      return;
    }
    librarianService.createCollection(title.trim());
    refresh();
  }

  @Override
  public void deleteCollection(String collectionId) {
    commitPendingDeletionImmediately();
    pendingDeletionId = collectionId;
    Optional<RecipeCollection> coll = librarianService.findCollectionById(collectionId);
    String label = coll.map(RecipeCollection::getTitle).orElse(collectionId);
    undoMessage.set("Deleted: " + label);
    undoAvailable.set(true);
    rebuildFilteredCollections();
    if (pendingDeletionId != null && pendingDeletionId.equals(selectedCollectionId)) {
      clearSelection();
    }
    undoTimer.stop();
    undoTimer.setDuration(javafx.util.Duration.millis(Math.max(1, undoTimeout.toMillis())));
    undoTimer.playFromStart();
  }

  /** Finishes any in-memory pending delete by performing the repository delete. */
  private void commitPendingDeletionImmediately() {
    if (pendingDeletionId == null) {
      return;
    }
    undoTimer.stop();
    String toDelete = pendingDeletionId;
    librarianService.deleteCollection(toDelete);
    pendingDeletionId = null;
    undoAvailable.set(false);
    undoMessage.set("");
    removeFromAllRows(toDelete);
    rebuildFilteredCollections();
    reconcileSelectionAfterDataChange();
  }

  @Override
  public void undoDelete() {
    if (pendingDeletionId == null) {
      return;
    }
    undoTimer.stop();
    pendingDeletionId = null;
    undoAvailable.set(false);
    undoMessage.set("");
    rebuildFilteredCollections();
    reconcileSelectionAfterDataChange();
  }

  @Override
  public void selectRecipe(String recipeId) {
    if (selectedCollectionId == null) {
      return;
    }
    Optional<RecipeCollection> coll = librarianService.findCollectionById(selectedCollectionId);
    if (coll.isEmpty()) {
      return;
    }
    if (coll.get().findRecipeById(recipeId).isEmpty()) {
      return;
    }
    navigationService.navigateToRecipe(recipeId);
  }

  @Override
  public List<String> getCollectionIds() {
    return collections.stream().map(LibraryCollectionRow::id).toList();
  }

  @Override
  public @Nullable String getSelectedCollectionId() {
    return selectedCollectionId;
  }

  @Override
  public List<String> getRecipeIds() {
    return recipes.stream().map(LibraryRecipeRow::id).toList();
  }

  @Override
  public boolean isLoading() {
    return loading.get();
  }

  @Override
  public boolean isUndoAvailable() {
    return undoAvailable.get();
  }

  @Override
  public String getFilterText() {
    return filterText.get();
  }

  private void rebuildFilteredCollections() {
    String needle =
        filterText.get() == null ? "" : filterText.get().trim().toLowerCase(Locale.ROOT);
    List<LibraryCollectionRow> out = new ArrayList<>();
    for (LibraryCollectionRow row : allRows) {
      if (pendingDeletionId != null && pendingDeletionId.equals(row.id())) {
        continue;
      }
      if (!needle.isEmpty() && !row.title().toLowerCase(Locale.ROOT).contains(needle)) {
        continue;
      }
      out.add(row);
    }
    collections.setAll(out);
  }

  private void removeFromAllRows(String collectionId) {
    allRows = allRows.stream().filter(r -> !r.id().equals(collectionId)).toList();
  }
}
