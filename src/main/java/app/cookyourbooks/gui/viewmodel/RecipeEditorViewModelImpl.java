package app.cookyourbooks.gui.viewmodel;

import java.util.List;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.UnitSystem;
import app.cookyourbooks.repository.RecipeRepository;

/**
 * Implementation of {@link RecipeEditorViewModel}.
 *
 * <p>Manages the mutable editing state for a single recipe. Tracks dirty state, validates inputs,
 * and persists changes asynchronously via {@link BackgroundTaskRunner} so the UI stays responsive
 * during saves.
 *
 * <h2>Dirty tracking</h2>
 *
 * <p>A {@code suppressDirtyTracking} flag prevents programmatic resets (e.g., loading a new recipe
 * or discarding changes) from falsely triggering the dirty flag. Any method that sets properties
 * programmatically — rather than in response to a user action — must wrap those updates with this
 * flag set to {@code true}.
 *
 * <h2>Save lifecycle</h2>
 *
 * <ol>
 *   <li>Collect current edits and build an updated {@link Recipe} (still on FX thread).
 *   <li>Hand off the repository write to a background thread via {@link BackgroundTaskRunner}.
 *   <li>On success (FX thread): update {@code originalRecipe}, clear dirty, exit edit mode.
 *   <li>On failure (FX thread): leave dirty state intact so the user's edits are preserved.
 * </ol>
 */
public class RecipeEditorViewModelImpl implements RecipeEditorViewModel {

  private final RecipeRepository recipeRepository;
  private final NavigationService navigationService;
  private final Supplier<ConversionRegistry> conversionRegistrySupplier;

  // ──────────────────────────────────────────────────────────────────────────
  // Internal state (not exposed to the View)
  // ──────────────────────────────────────────────────────────────────────────

  /** ID of the currently loaded recipe, or null if no recipe has been loaded yet. */
  private @Nullable String recipeId = null;

  /**
   * The last-saved (or freshly loaded) version of the recipe. This is the snapshot we revert to on
   * discard, and the source of id/servings/instructions/conversionRules when rebuilding on save.
   */
  private @Nullable Recipe originalRecipe = null;

  /**
   * While true, property change listeners skip setting {@code isDirty}. Set to true before any
   * programmatic property update and restored to false immediately after, so only user-driven
   * changes mark the state dirty.
   */
  private boolean suppressDirtyTracking = false;

  private boolean suppressUnitRollback = false;

  // ──────────────────────────────────────────────────────────────────────────
  // Observable properties (bound to the View via FXML controller)
  // ──────────────────────────────────────────────────────────────────────────

  /** Editable title, bound to the title TextField in the View. */
  private final StringProperty title = new SimpleStringProperty("");

  /** Editable ingredient list, bound to a ListView/TableView in the View. */
  private final ObservableList<EditableIngredient> ingredients =
      FXCollections.observableArrayList();

  /** Read-only instruction strings, formatted as "1. Mix ingredients". */
  private final ObservableList<String> instructions = FXCollections.observableArrayList();

  /** True when the editor is in edit mode; false in view-only mode. */
  private final BooleanProperty editing = new SimpleBooleanProperty(false);

  /** True when the user has made changes that haven't been saved yet. */
  private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

  /** True when the current title is non-blank (the only required field). */
  private final BooleanProperty isValid = new SimpleBooleanProperty(false);

  /** True while a background save is in progress; used to disable controls and show "Saving...". */
  private final BooleanProperty isSaving = new SimpleBooleanProperty(false);

  /** Displays feedback to the user, e.g. "Saved successfully." or "Save failed: ...". */
  private final StringProperty statusMessage = new SimpleStringProperty("");

  public RecipeEditorViewModelImpl(
      RecipeRepository recipeRepository,
      NavigationService navigationService,
      Supplier<ConversionRegistry> conversionRegistrySupplier) {
    this.recipeRepository = recipeRepository;
    this.navigationService = navigationService;
    this.conversionRegistrySupplier = conversionRegistrySupplier;
    attachDirtyListeners();
    attachValidationListeners();
    attachUnitSystemListener();
  }

  public RecipeEditorViewModelImpl(RecipeRepository recipeRepository) {
    this(
        recipeRepository,
        new NavigationService(),
        app.cookyourbooks.conversion.LayeredConversionRegistry::new);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Listener setup
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Attaches listeners that set {@code isDirty = true} whenever the title or ingredient list
   * changes while in edit mode and dirty tracking is not suppressed.
   */
  private void attachDirtyListeners() {
    // Title changes in edit mode → dirty
    title.addListener((obs, oldVal, newVal) -> markDirtyIfEditing());
    // Additions/removals AND field edits on existing ingredients → dirty
    ingredients.addListener(
        (javafx.collections.ListChangeListener<EditableIngredient>)
            change -> {
              markDirtyIfEditing();
              while (change.next()) {
                if (change.wasAdded()) {
                  // Attach field-level listeners to each newly added ingredient
                  change.getAddedSubList().forEach(this::attachIngredientFieldListeners);
                }
              }
            });
  }

  /**
   * Attaches listeners to an ingredient's individual fields so that editing name, amount, unit, or
   * vague in the View will mark the ViewModel dirty.
   */
  private void attachIngredientFieldListeners(EditableIngredient ingredient) {
    ingredient.nameProperty().addListener((obs, o, n) -> markDirtyIfEditing());
    ingredient.amountProperty().addListener((obs, o, n) -> markDirtyIfEditing());
    ingredient.unitProperty().addListener((obs, o, n) -> markDirtyIfEditing());
    ingredient.vagueProperty().addListener((obs, o, n) -> markDirtyIfEditing());
  }

  /**
   * Marks the recipe as dirty only when the user (not our own code) is making the change. The guard
   * conditions are: (1) dirty tracking is not suppressed, and (2) the editor is in edit mode.
   */
  private void markDirtyIfEditing() {
    if (!suppressDirtyTracking && editing.get()) {
      isDirty.set(true);
    }
  }

  /**
   * Attaches a listener that keeps {@code isValid} in sync with the title field. The recipe is
   * valid as long as the title is non-blank.
   */
  private void attachValidationListeners() {
    title.addListener((obs, oldVal, newVal) -> isValid.set(newVal != null && !newVal.isBlank()));
  }

  /** Keeps editable measured ingredients synchronized with the app-wide unit system. */
  private void attachUnitSystemListener() {
    navigationService
        .unitSystemProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (oldValue == null || newValue == null || oldValue == newValue) {
                return;
              }
              if (suppressUnitRollback) {
                return;
              }
              if (isDirty.get()) {
                suppressUnitRollback = true;
                navigationService.setUnitSystem(oldValue);
                suppressUnitRollback = false;
                statusMessage.set("Save or discard edits before changing unit system.");
                return;
              }
              ConversionSummary summary = convertIngredientsToSystem(newValue);
              if (!summary.anyConverted()) {
                statusMessage.set("No measurable ingredients to convert.");
              }
            });
  }

  // ──────────────────────────────────────────────────────────────────────────
  // RecipeEditorViewModel — observable properties
  // ──────────────────────────────────────────────────────────────────────────

  @Override
  public StringProperty titleProperty() {
    return title;
  }

  @Override
  public ObservableList<EditableIngredient> ingredientsProperty() {
    return ingredients;
  }

  @Override
  public ObservableList<String> instructionsProperty() {
    return instructions;
  }

  @Override
  public BooleanProperty editingProperty() {
    return editing;
  }

  @Override
  public BooleanProperty isDirtyProperty() {
    return isDirty;
  }

  @Override
  public BooleanProperty isValidProperty() {
    return isValid;
  }

  @Override
  public BooleanProperty isSavingProperty() {
    return isSaving;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // RecipeEditorViewModel — commands
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Loads a recipe by ID. If found, stores it as the "original" snapshot and populates all
   * observable properties. Does nothing if the ID is not found in the repository.
   */
  @Override
  public void loadRecipe(String recipeId) {
    recipeRepository
        .findById(recipeId)
        .ifPresent(
            recipe -> {
              this.recipeId = recipeId;
              this.originalRecipe = recipe; // save snapshot for discard/rebuild
              populateFromRecipe(recipe);
            });
  }

  /**
   * Flips between view mode (read-only) and edit mode (editable fields). The View uses {@link
   * #editingProperty()} to enable/disable controls.
   */
  @Override
  public void toggleEditMode() {
    editing.set(!editing.get());
  }

  /**
   * Persists the current edits to the repository on a background thread.
   *
   * <p>This is a no-op when: (a) there are no unsaved changes, (b) the title is blank, or (c) no
   * recipe has been loaded. Otherwise: builds an updated {@link Recipe} by combining the edited
   * title and ingredient list with the original recipe's unchanged fields (id, servings,
   * instructions, conversionRules), then hands it off to {@link BackgroundTaskRunner}.
   */
  @Override
  public void save() {
    if (!isDirty.get()) {
      statusMessage.set("No changes to save.");
      return;
    }
    if (!isValid.get()) {
      statusMessage.set("Title cannot be blank.");
      return;
    }
    if (originalRecipe == null) {
      statusMessage.set("No recipe selected.");
      return;
    }

    // Parse each ingredient once — blank-named rows produce empty and are skipped at the end
    List<java.util.Optional<Ingredient>> parsed =
        ingredients.stream().map(EditableIngredient::toIngredient).toList();

    // Validate: any named row that failed to parse blocks the save
    boolean hasParseFailures =
        java.util.stream.IntStream.range(0, ingredients.size())
            .anyMatch(
                i -> !ingredients.get(i).getName().trim().isBlank() && parsed.get(i).isEmpty());
    if (hasParseFailures) {
      statusMessage.set("Save failed: one or more ingredients have invalid amounts or units.");
      return;
    }

    // Reuse parse results — no second pass needed
    List<Ingredient> updatedIngredients =
        parsed.stream().filter(java.util.Optional::isPresent).map(java.util.Optional::get).toList();

    // Rebuild the recipe: same id + unchanged fields, updated title and ingredients
    Recipe updatedRecipe =
        new Recipe(
            originalRecipe.getId(),
            title.get(),
            originalRecipe.getServings(),
            updatedIngredients,
            originalRecipe.getInstructions(),
            originalRecipe.getConversionRules());

    // Disable controls while save is in progress (E8)
    isSaving.set(true);
    statusMessage.set("");

    BackgroundTaskRunner.run(
        () -> {
          // Runs on background thread — repository write happens here
          recipeRepository.save(updatedRecipe);
          return null; // Void callable: Task<Void>
        },
        ignored -> {
          // Runs on FX thread after success — safe to update UI properties here
          originalRecipe = updatedRecipe; // advance the snapshot to the saved version
          suppressDirtyTracking = true; // prevent the isDirty listener from firing
          isDirty.set(false);
          suppressDirtyTracking = false; // re-enable dirty tracking for future user edits
          editing.set(false); // exit edit mode on success
          isSaving.set(false);
          statusMessage.set("Saved successfully.");
        },
        error -> {
          // Runs on FX thread after failure — preserve dirty state so user doesn't lose work
          // (E9)
          isSaving.set(false);
          String msg = error.getMessage();
          statusMessage.set(
              (msg == null || msg.isBlank()) ? "Save failed." : "Save failed: " + msg);
          // isDirty intentionally NOT cleared — user must be able to retry or discard
        });
  }

  /**
   * Reverts all editable fields to the last-saved (or freshly loaded) state. Clears dirty flag. The
   * View should show a confirmation dialog before calling this when {@code isDirty} is true.
   */
  @Override
  public void discardChanges() {
    if (originalRecipe != null) {
      populateFromRecipe(originalRecipe); // resets title, ingredients, isDirty, statusMessage
    }
  }

  /** Appends a new blank ingredient row to the list. The user fills in the fields in the View. */
  @Override
  public void addIngredient() {
    ingredients.add(new EditableIngredient());
  }

  /** Removes the ingredient at the given zero-based index. Silently ignored if out of range. */
  @Override
  public void removeIngredient(int index) {
    if (index >= 0 && index < ingredients.size()) {
      ingredients.remove(index);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // RecipeEditorViewModel — non-JavaFX accessors (used by grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  @Override
  public @Nullable String getRecipeId() {
    return recipeId;
  }

  @Override
  public String getTitle() {
    return title.get();
  }

  @Override
  public int getIngredientCount() {
    return ingredients.size();
  }

  @Override
  public List<String> getIngredientNames() {
    return ingredients.stream().map(EditableIngredient::getName).toList();
  }

  @Override
  public boolean isEditing() {
    return editing.get();
  }

  @Override
  public boolean isDirty() {
    return isDirty.get();
  }

  @Override
  public boolean isValid() {
    return isValid.get();
  }

  @Override
  public boolean isSaving() {
    return isSaving.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Populates all observable properties from a recipe snapshot. Used by both {@link
   * #loadRecipe(String)} and {@link #discardChanges()}.
   *
   * <p>Dirty tracking is suppressed for the duration of this method so that setting properties
   * programmatically does not falsely mark the editor as dirty. After all properties are set, dirty
   * tracking is re-enabled and {@code isDirty} is explicitly reset to {@code false}.
   */
  private void populateFromRecipe(Recipe recipe) {
    suppressDirtyTracking = true; // all property changes below should NOT set isDirty
    title.set(recipe.getTitle());
    // Wrap each domain Ingredient in an EditableIngredient for UI binding
    ingredients.setAll(recipe.getIngredients().stream().map(EditableIngredient::new).toList());
    convertIngredientsToSystem(navigationService.getUnitSystem());
    // Format instructions as "1. Step text" for display in the read-only list
    instructions.setAll(
        recipe.getInstructions().stream()
            .map(i -> i.getStepNumber() + ". " + i.getText())
            .toList());
    isDirty.set(false); // explicit reset — we're at a clean state
    suppressDirtyTracking = false; // re-enable: user edits from here on should set isDirty
    isValid.set(!recipe.getTitle().isBlank()); // sync validation state with loaded title
    statusMessage.set(""); // clear any previous save/error message
  }

  private ConversionSummary convertIngredientsToSystem(UnitSystem targetSystem) {
    ConversionRegistry registry = conversionRegistrySupplier.get();
    boolean sawMeasuredIngredient = false;
    boolean anyConverted = false;
    suppressDirtyTracking = true;
    for (EditableIngredient ingredient : ingredients) {
      ConversionResult result = convertIngredientIfPossible(ingredient, targetSystem, registry);
      sawMeasuredIngredient = sawMeasuredIngredient || result.wasMeasured();
      anyConverted = anyConverted || result.converted();
    }
    suppressDirtyTracking = false;
    return new ConversionSummary(sawMeasuredIngredient, anyConverted);
  }

  private ConversionResult convertIngredientIfPossible(
      EditableIngredient ingredient, UnitSystem targetSystem, ConversionRegistry registry) {
    if (ingredient.isVague()) {
      return new ConversionResult(false, false);
    }
    var parsed = ingredient.toIngredient();
    if (parsed.isEmpty() || !(parsed.get() instanceof MeasuredIngredient measured)) {
      return new ConversionResult(false, false);
    }
    Unit sourceUnit = measured.getQuantity().getUnit();
    if (sourceUnit.getSystem() == UnitSystem.HOUSE || sourceUnit.getSystem() == targetSystem) {
      return new ConversionResult(true, false);
    }
    var rule = registry.findConversionToSystem(sourceUnit, targetSystem);
    if (rule.isEmpty()) {
      return new ConversionResult(true, false);
    }
    try {
      Quantity converted =
          registry.convert(measured.getQuantity(), rule.get().toUnit(), ingredient.getName());
      ingredient.amountProperty().set(extractAmountString(converted));
      ingredient.unitProperty().set(converted.getUnit().getAbbreviation());
      return new ConversionResult(true, true);
    } catch (UnsupportedConversionException | RuntimeException conversionError) {
      // Leave ingredient unchanged when unsupported or invalid.
      return new ConversionResult(true, false);
    }
  }

  private String extractAmountString(Quantity quantity) {
    String unitAbbreviation = quantity.getUnit().getAbbreviation();
    String unitPlural = quantity.getUnit().getPluralAbbreviation();
    String value = quantity.toString();
    if (!unitAbbreviation.equals(unitPlural) && value.endsWith(" " + unitPlural)) {
      return value.substring(0, value.length() - unitPlural.length() - 1).trim();
    }
    if (value.endsWith(" " + unitAbbreviation)) {
      return value.substring(0, value.length() - unitAbbreviation.length() - 1).trim();
    }
    return value;
  }

  private record ConversionResult(boolean wasMeasured, boolean converted) {}

  private record ConversionSummary(boolean hadMeasuredIngredients, boolean anyConverted) {}
}
