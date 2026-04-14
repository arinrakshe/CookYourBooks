package app.cookyourbooks.gui.viewmodel;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.RecipeOcrService;

public class ImportViewModelImpl implements ImportViewModel {

  private final RecipeOcrService ocrService;
  private final LibrarianService librarianService;

  private final StringProperty statusMessage;
  private final StringProperty errorMessage;
  private final StringProperty importedTitle;
  private final ObservableList<Ingredient> importedIngredients;
  private final ObservableList<RecipeCollection> availableCollections;
  private final StringProperty currentState;
  private final DoubleProperty importProgress;

  private State state;
  private @Nullable String selectedCollectionId;
  private @Nullable Recipe importedRecipe;
  private @Nullable Task<Recipe> importTask = null;

  /**
   * Constructs a new ImportViewModelImpl with the given OCR service and librarian service.
   *
   * @param ocrService the OCR service used to extract recipes from images
   * @param librarianService the librarian service used to manage recipe collections
   */
  public ImportViewModelImpl(RecipeOcrService ocrService, LibrarianService librarianService) {
    this.ocrService = ocrService;
    this.librarianService = librarianService;

    this.statusMessage = new SimpleStringProperty("Ready");
    this.errorMessage = new SimpleStringProperty("");
    this.importedTitle = new SimpleStringProperty("");
    this.importedIngredients = FXCollections.observableArrayList();
    this.availableCollections = FXCollections.observableArrayList();

    this.state = State.IDLE;
    this.selectedCollectionId = null;
    this.importedRecipe = null;

    this.currentState = new SimpleStringProperty("idle");
    this.importProgress = new SimpleDoubleProperty(0.0);
  }

  /**
   * Resets the view model to the idle state, clearing any imported recipe data and error messages.
   */
  private void resetToIdle() {
    state = State.IDLE;
    currentState.set("idle");
    errorMessage.set("");
    importedTitle.set("");
    importedIngredients.clear();
    importedRecipe = null;
    importTask = null;
    importProgress.set(0.0);
  }

  /**
   * Returns the observable status message property used by the import view.
   *
   * @return the status message property
   */
  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  /**
   * Returns the observable error message property used by the import view.
   *
   * @return the error message property
   */
  @Override
  public StringProperty errorMessageProperty() {
    return errorMessage;
  }

  /**
   * Returns the observable imported title property used by the import view.
   *
   * @return the imported title property
   */
  @Override
  public StringProperty importedTitleProperty() {
    return importedTitle;
  }

  /**
   * Returns the observable imported ingredients property used by the import view.
   *
   * @return the imported ingredients property
   */
  @Override
  public ObservableList<Ingredient> importedIngredientsProperty() {
    return importedIngredients;
  }

  /**
   * Returns the observable available collections property used by the import view.
   *
   * @return the available collections property
   */
  @Override
  public ObservableList<RecipeCollection> availableCollectionsProperty() {
    return availableCollections;
  }

  /**
   * Starts the import process for the given image path.
   *
   * @param imagePath the path to the image containing the recipe
   */
  @Override
  public void startImport(Path imagePath) {
    if (state == State.PROCESSING || state == State.REVIEW) {
      return;
    }

    resetToIdle();
    state = State.PROCESSING;
    currentState.set("processing");
    statusMessage.set("Parsing recipe image...");

    importTask =
        BackgroundTaskRunner.run(
            () -> {
              int steps = 30;
              for (int i = 1; i <= steps; i++) {
                Thread.sleep(100);
                final double progress = i / (double) steps;
                javafx.application.Platform.runLater(() -> importProgress.set(progress));
              }
              return ocrService.extractRecipe(imagePath);
            },
            result -> {
              importedRecipe = result;
              importedTitle.set(result.getTitle());
              importedIngredients.setAll(result.getIngredients());
              statusMessage.set("Parsing complete. Review extracted recipe.");
              importTask = null;
              state = State.REVIEW;
              currentState.set("review");
              importProgress.set(1.0);
            },
            (Throwable error) -> {
              importedRecipe = null;
              importedIngredients.clear();
              importedTitle.set("");
              String message = error.getMessage() == null ? "Import failed." : error.getMessage();
              errorMessage.set(message);
              statusMessage.set("Import failed.");
              importTask = null;
              state = State.ERROR;
              currentState.set("error");
              importProgress.set(0.0);
            });
  }

  /**
   * Cancels the ongoing import process if it is currently in progress. Resets the view model to the
   * idle state and updates the status message.
   */
  @Override
  public void cancelImport() {
    if (state == State.IDLE) {
      return;
    }

    if (importTask != null && importTask.isRunning()) {
      importTask.cancel();
      importTask = null;
    }

    resetToIdle();
    statusMessage.set("Import cancelled. You can start a new import.");
  }

  /**
   * Accepts the imported recipe and saves it to the selected collection. Resets the view model to
   * the idle state and updates the status message.
   */
  @Override
  public void acceptImport() {
    if (state != State.REVIEW || importedRecipe == null || selectedCollectionId == null) {
      return;
    }

    String selectedValue = selectedCollectionId.trim();
    if (selectedValue.isBlank()) {
      return;
    }

    // Resolve the selected value to a valid collection ID.
    // Prefer exact ID match, then fall back to title match to handle UI/value mismatches.
    Collection<RecipeCollection> collections = librarianService.listCollections();
    String targetCollectionId =
        collections.stream()
            .filter(c -> c.getId().equals(selectedValue))
            .map(RecipeCollection::getId)
            .findFirst()
            .or(
                () ->
                    collections.stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(selectedValue))
                        .map(RecipeCollection::getId)
                        .findFirst())
            .orElse("");

    if (targetCollectionId.isEmpty()) {
      errorMessage.set("Please select a valid target collection.");
      statusMessage.set("Failed to import recipe.");
      return;
    }

    String editedTitle = importedTitle.get();

    if (editedTitle == null || editedTitle.isBlank()) {
      errorMessage.set("Recipe title cannot be blank.");
      return;
    }

    List<Ingredient> editedIngredients = List.copyOf(importedIngredients);

    Recipe recipeToSave =
        new Recipe(
            importedRecipe.getId(),
            editedTitle,
            importedRecipe.getServings(),
            editedIngredients,
            importedRecipe.getInstructions(),
            importedRecipe.getConversionRules());

    try {
      librarianService.saveRecipe(recipeToSave, targetCollectionId);
      resetToIdle();
      statusMessage.set("Recipe imported successfully.");
    } catch (RuntimeException ex) {
      errorMessage.set("Failed to save recipe: " + ex.getMessage());
      statusMessage.set("Failed to import recipe.");
    }
  }

  /**
   * Rejects the imported recipe and resets the view model to the idle state. Updates the status
   * message to indicate that the import was rejected.
   */
  @Override
  public void rejectImport() {
    if (state != State.REVIEW) {
      return;
    }

    resetToIdle();
    statusMessage.set("Import rejected. You can start a new import.");
  }

  /**
   * Selects the target collection for saving the imported recipe.
   *
   * @param collectionId the ID of the collection to select
   */
  @Override
  public void selectTargetCollection(String collectionId) {
    selectedCollectionId = collectionId;
  }

  /**
   * Loads the available recipe collections from the librarian service and updates the view model.
   */
  @Override
  public void loadCollections() {
    Collection<RecipeCollection> collections = librarianService.listCollections();
    availableCollections.setAll(collections);
  }

  /**
   * Returns the current state of the import process.
   *
   * @return the current state as a lowercase string
   */
  @Override
  public String getState() {
    return state.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the current status message of the import process.
   *
   * @return the current status message
   */
  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  /**
   * Returns the current error message of the import process, if any.
   *
   * @return the current error message, or null if there is no error
   */
  @Override
  public @Nullable String getErrorMessage() {
    return errorMessage.get().isEmpty() ? null : errorMessage.get();
  }

  /**
   * Returns the title of the imported recipe, if the import is in the review state.
   *
   * @return the imported recipe title, or null if not in review state
   */
  @Override
  public @Nullable String getImportedRecipeTitle() {
    if (state != State.REVIEW) {
      return null;
    }
    return importedTitle.get();
  }

  /**
   * Returns the list of ingredient names of the imported recipe, if the import is in the review
   * state.
   *
   * @return the list of imported ingredient names, or an empty list if not in review state
   */
  @Override
  public List<String> getImportedIngredientNames() {
    if (state != State.REVIEW) {
      return List.of();
    }
    return importedIngredients.stream().map(Ingredient::getName).toList();
  }

  /**
   * Returns the list of available collection IDs.
   *
   * @return the list of available collection IDs
   */
  @Override
  public List<String> getAvailableCollectionIds() {
    return availableCollections.stream().map(RecipeCollection::getId).toList();
  }

  /**
   * Returns the ID of the currently selected collection, if any.
   *
   * @return the selected collection ID, or null if no collection is selected
   */
  @Override
  public @Nullable String getSelectedCollectionId() {
    return selectedCollectionId;
  }

  @Override
  public StringProperty currentStateProperty() {
    return currentState;
  }

  @Override
  public DoubleProperty importProgressProperty() {
    return importProgress;
  }
}
