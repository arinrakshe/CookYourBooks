package app.cookyourbooks.gui.view;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import app.cookyourbooks.adapters.usda.NutritionInfo;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.viewmodel.EditableIngredient;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModel;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.services.nutrition.NutritionLookupService;

/**
 * Controller for {@code RecipeEditorView.fxml}.
 *
 * <p>Binds all FXML controls to the {@link RecipeEditorViewModel}. The controller itself contains
 * no business logic — all state and behaviour live in the ViewModel. The controller's only jobs
 * are:
 *
 * <ol>
 *   <li>Wire bindings and listeners in {@link #initialize()} after FXML injection.
 *   <li>Listen for navigation events to call {@link RecipeEditorViewModel#loadRecipe(String)}.
 *   <li>Hook up button actions to ViewModel commands.
 *   <li>Provide a custom cell factory so each ingredient row renders as editable fields.
 * </ol>
 */
@SuppressWarnings("NullAway.Init") // FXML fields are injected by FXMLLoader, not the constructor
public class RecipeEditorViewController {

  // ── FXML-injected controls ──────────────────────────────────────────────

  @FXML private Label titleLabel;
  @FXML private TextField titleField;
  @FXML private Button editButton;
  @FXML private Button saveButton;
  @FXML private Button discardButton;
  @FXML private Label statusLabel;
  @FXML private Button addIngredientButton;
  @FXML private ListView<EditableIngredient> ingredientListView;
  @FXML private ListView<String> instructionListView;

  // ── Dependencies ─────────────────────────────────────────────────────────

  private final RecipeEditorViewModel vm;
  private final NavigationService navigationService;
  private final NutritionLookupService nutritionLookupService;

  /**
   * Constructs the controller with its required dependencies.
   *
   * @param vm the ViewModel for this feature
   * @param navigationService used to listen for recipe-selection navigation events
   * @param nutritionLookupService used to fetch nutrition data for ingredients
   */
  public RecipeEditorViewController(
      RecipeEditorViewModel vm,
      NavigationService navigationService,
      NutritionLookupService nutritionLookupService) {
    this.vm = vm;
    this.navigationService = navigationService;
    this.nutritionLookupService = nutritionLookupService;
  }

  // ── Initialization ───────────────────────────────────────────────────────

  /**
   * Called by FXMLLoader after all FXML fields are injected. This is where we set up all bindings
   * between the View controls and the ViewModel properties.
   */
  @SuppressWarnings("UnusedMethod") // called reflectively by FXMLLoader
  @FXML
  private void initialize() {
    bindTitleControls();
    bindToolbarButtons();
    bindStatusLabel();
    bindIngredientList();
    bindInstructionList();
    listenForNavigation();
  }

  // ── Binding helpers ──────────────────────────────────────────────────────

  /**
   * Binds the title label (view mode) and title text field (edit mode) to the ViewModel.
   *
   * <p>The label shows the current title in view mode. The text field is bidirectionally bound to
   * {@code vm.titleProperty()} so user edits flow directly into the ViewModel.
   */
  private void bindTitleControls() {
    // Label shows the current title in view mode
    titleLabel.textProperty().bind(vm.titleProperty());

    // TextField is bidirectionally bound — user edits update the ViewModel immediately
    titleField.textProperty().bindBidirectional(vm.titleProperty());

    // Swap label vs. field depending on edit mode
    titleLabel.visibleProperty().bind(vm.editingProperty().not());
    titleLabel.managedProperty().bind(vm.editingProperty().not());
    titleField.visibleProperty().bind(vm.editingProperty());
    titleField.managedProperty().bind(vm.editingProperty());
    // Disable the title field while saving so mid-save edits can't be lost (E8)
    titleField.disableProperty().bind(vm.isSavingProperty());
  }

  /**
   * Wires the toolbar buttons (Edit, Save, Discard) to ViewModel commands and binds their
   * visibility/enabled state to ViewModel properties.
   *
   * <ul>
   *   <li>Edit button — visible in view mode only
   *   <li>Save button — visible in edit mode; disabled when not valid or currently saving; shows
   *       "Saving..." while isSaving is true
   *   <li>Discard button — visible in edit mode; disabled while saving
   * </ul>
   */
  private void bindToolbarButtons() {
    // Edit button: shown in view mode, calls toggleEditMode()
    editButton.visibleProperty().bind(vm.editingProperty().not());
    editButton.managedProperty().bind(vm.editingProperty().not());
    editButton.setOnAction(e -> vm.toggleEditMode());

    // Save button: shown in edit mode, disabled when invalid or saving
    saveButton.visibleProperty().bind(vm.editingProperty());
    saveButton.managedProperty().bind(vm.editingProperty());
    // Disable when not valid OR currently saving (E8: controls disabled during save)
    saveButton.disableProperty().bind(vm.isValidProperty().not().or(vm.isSavingProperty()));
    // Show "Saving..." on the button while the background thread is running
    saveButton
        .textProperty()
        .bind(
            javafx.beans.binding.Bindings.when(vm.isSavingProperty())
                .then("Saving...")
                .otherwise("Save"));
    saveButton.setOnAction(e -> vm.save());

    // Discard button: shown in edit mode, disabled while saving
    discardButton.visibleProperty().bind(vm.editingProperty());
    discardButton.managedProperty().bind(vm.editingProperty());
    discardButton.disableProperty().bind(vm.isSavingProperty());
    discardButton.setOnAction(e -> handleDiscard());
  }

  /**
   * Handles the Discard button click. Shows a confirmation dialog when there are unsaved changes
   * (isDirty = true) before reverting. If nothing has changed, discards immediately.
   */
  private void handleDiscard() {
    if (vm.isDirty()) {
      // Confirmation dialog — prevents accidental loss of unsaved work
      javafx.scene.control.Alert alert =
          new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
      alert.setTitle("Discard Changes");
      alert.setHeaderText("You have unsaved changes.");
      alert.setContentText("Are you sure you want to discard them?");
      alert
          .showAndWait()
          .filter(response -> response == javafx.scene.control.ButtonType.OK)
          .ifPresent(
              response -> {
                vm.discardChanges();
                vm.toggleEditMode(); // return to view mode
              });
    } else {
      vm.discardChanges();
      vm.toggleEditMode();
    }
  }

  /** Binds the status label to the ViewModel's status message. Hidden when message is empty. */
  private void bindStatusLabel() {
    statusLabel.textProperty().bind(vm.statusMessageProperty());
    // Hide the label entirely when there is no message (avoid empty space)
    statusLabel.visibleProperty().bind(vm.statusMessageProperty().isNotEmpty());
    statusLabel.managedProperty().bind(vm.statusMessageProperty().isNotEmpty());
  }

  /**
   * Binds the ingredient ListView to {@code vm.ingredientsProperty()} and installs a custom cell
   * factory that renders each {@link EditableIngredient} as either a read-only string (view mode)
   * or an editable row with name/amount/unit fields and a Remove button (edit mode).
   */
  private void bindIngredientList() {
    // The interface declares ObservableList<?> to allow flexibility in the entry type.
    // Our impl uses EditableIngredient — the cast is safe here.
    @SuppressWarnings("unchecked")
    javafx.collections.ObservableList<EditableIngredient> items =
        (javafx.collections.ObservableList<EditableIngredient>) vm.ingredientsProperty();
    ingredientListView.setItems(items);
    addIngredientButton.visibleProperty().bind(vm.editingProperty());
    addIngredientButton.managedProperty().bind(vm.editingProperty());
    addIngredientButton.disableProperty().bind(vm.isSavingProperty());
    addIngredientButton.setOnAction(e -> vm.addIngredient());
    // Disable the list itself during save so rows can't be edited mid-flight (E8)
    ingredientListView.disableProperty().bind(vm.isSavingProperty());

    // Custom cell factory — switches between read-only and editable rendering
    ingredientListView.setCellFactory(list -> new IngredientCell());

    // When edit mode toggles, force all cells to re-render (updateItem is only called on data
    // changes by default, so we need refresh() to pick up the mode switch)
    vm.editingProperty().addListener((obs, oldVal, newVal) -> ingredientListView.refresh());
    navigationService
        .unitSystemProperty()
        .addListener((obs, oldVal, newVal) -> ingredientListView.refresh());
  }

  /**
   * Binds the instruction ListView to {@code vm.instructionsProperty()}. Instructions are read-only
   * — editing them is out of scope for this feature.
   */
  private void bindInstructionList() {
    instructionListView.setItems(vm.instructionsProperty());
  }

  /**
   * Listens for recipe-selection navigation events from {@link NavigationService}. When another
   * feature (e.g. Library View) calls {@code navigationService.navigateToRecipe(id)}, this listener
   * calls {@code vm.loadRecipe(id)} to populate the editor.
   */
  private void listenForNavigation() {
    // Use a named listener so we can temporarily remove it when resetting the selected ID on cancel
    javafx.beans.value.ChangeListener<String> listener =
        new javafx.beans.value.ChangeListener<>() {
          @Override
          public void changed(
              javafx.beans.value.ObservableValue<? extends String> obs,
              String oldId,
              String newId) {
            if (newId == null || newId.isBlank()) {
              return;
            }

            // Block navigation entirely while a save is in progress — loading now would race the
            // background save callbacks which may update state for the previous recipe afterward.
            if (vm.isSaving()) {
              resetSelectedId(oldId);
              return;
            }

            // If there are unsaved edits, confirm before discarding them
            if (vm.isDirty()) {
              javafx.scene.control.Alert alert =
                  new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
              alert.setTitle("Unsaved Changes");
              alert.setHeaderText("You have unsaved changes.");
              alert.setContentText("Loading a new recipe will discard them. Continue?");
              boolean confirmed =
                  alert
                      .showAndWait()
                      .filter(r -> r == javafx.scene.control.ButtonType.OK)
                      .isPresent();
              if (!confirmed) {
                // Reset the property back to oldId so a later click on the same recipe fires again
                resetSelectedId(oldId);
                return;
              }
            }

            vm.loadRecipe(newId);
            ingredientListView.refresh();
          }

          private void resetSelectedId(String id) {
            // Remove self to avoid re-entering while we reset the value
            navigationService.selectedRecipeIdProperty().removeListener(this);
            navigationService.selectedRecipeIdProperty().set(id);
            navigationService.selectedRecipeIdProperty().addListener(this);
          }
        };
    navigationService.selectedRecipeIdProperty().addListener(listener);
  }

  // ── Custom cell ──────────────────────────────────────────────────────────

  /**
   * A ListView cell that renders an {@link EditableIngredient} as either a read-only string (view
   * mode) or an editable row (edit mode).
   *
   * <ul>
   *   <li><b>View mode:</b> shows "{amount} {unit} {name}" as plain text.
   *   <li><b>Edit mode:</b> shows a row with a name TextField, amount TextField, unit ComboBox,
   *       vague CheckBox, and a Remove button.
   * </ul>
   *
   * <p>Controls are created <b>once</b> in the constructor and reused across calls to {@link
   * #updateItem}. Bindings are explicitly unbound before rebinding to the new item, preventing the
   * listener/binding accumulation that would otherwise occur on every {@code refresh()}.
   */
  private final class IngredientCell extends ListCell<EditableIngredient> {

    // Controls created once and reused
    private final TextField nameField = new TextField();
    private final TextField amountField = new TextField();
    private final ComboBox<String> unitCombo = new ComboBox<>();
    private final CheckBox vagueCheck = new CheckBox("Vague");
    private final Button removeBtn = new Button("\u2715");
    private final Button nutritionBtn = new Button("Nutrition");
    private final HBox editRow;

    // Stored listener references so they can be removed when the cell is rebound
    private @org.jspecify.annotations.Nullable ChangeListener<String> unitComboListener = null;
    private @org.jspecify.annotations.Nullable ChangeListener<String> unitPropListener = null;

    // The ingredient this cell is currently bound to (null if unbound)
    private @org.jspecify.annotations.Nullable EditableIngredient boundItem = null;

    IngredientCell() {
      nameField.setPromptText("Name");
      nameField.setPrefWidth(160);
      amountField.setPromptText("Amount");
      amountField.setPrefWidth(70);
      for (Unit u : Unit.values()) {
        unitCombo.getItems().add(u.getAbbreviation());
      }
      removeBtn.setAccessibleText("Remove ingredient");
      removeBtn.setTooltip(new Tooltip("Remove ingredient"));
      removeBtn.setOnAction(e -> vm.removeIngredient(getIndex()));
      nutritionBtn.setAccessibleText("Lookup nutrition");
      nutritionBtn.setTooltip(new Tooltip("Lookup nutrition"));
      nutritionBtn.setOnAction(
          e -> {
            String name = nameField.getText();
            if (name == null || name.isBlank()) {
              return;
            }
            double amount = 0;
            try {
              amount = Double.parseDouble(amountField.getText());
            } catch (NumberFormatException ignored) {
              // non-numeric or empty — adapter will return per-100g
            }
            String unit = unitCombo.getValue() != null ? unitCombo.getValue() : "";
            try {
              NutritionInfo info = nutritionLookupService.lookup(name, amount, unit);
              showNutritionDialog(info, amount, unit);
            } catch (app.cookyourbooks.services.nutrition.NutritionLookupException ex) {
              new javafx.scene.control.Alert(
                      javafx.scene.control.Alert.AlertType.ERROR, ex.getMessage())
                  .showAndWait();
            }
          });
      editRow = new HBox(8, nameField, amountField, unitCombo, vagueCheck, removeBtn, nutritionBtn);
      editRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(EditableIngredient item, boolean empty) {
      super.updateItem(item, empty);

      if (empty || item == null) {
        unbind();
        setGraphic(null);
        setText(null);
        return;
      }

      if (vm.isEditing()) {
        if (item != boundItem) {
          unbind();
          bindTo(item);
        }
        // Refresh IDs so TestFX locators stay accurate after list reordering
        nameField.setId("ingredient-name-" + getIndex());
        amountField.setId("ingredient-amount-" + getIndex());
        unitCombo.setId("ingredient-unit-" + getIndex());
        vagueCheck.setId("ingredient-vague-" + getIndex());
        removeBtn.setId("ingredient-remove-" + getIndex());
        setGraphic(editRow);
        setText(null);
      } else {
        unbind();
        setGraphic(null);
        String display =
            item.isVague()
                ? item.getName()
                : item.getAmount() + " " + item.getUnit() + " " + item.getName();
        setText(display.trim());
      }
    }

    /** Shows a popup dialog with the nutrition information. */
    private void showNutritionDialog(NutritionInfo info, double amount, String unit) {
      GridPane grid = new GridPane();
      grid.setHgap(20);
      grid.setVgap(6);
      grid.setPadding(new javafx.geometry.Insets(10));

      String header =
          (amount > 0 && !unit.isBlank())
              ? String.format("For %.1f %s:", amount, unit)
              : "Per 100g:";
      grid.add(new Label(header), 0, 0, 2, 1);
      grid.add(new Label("Calories"), 0, 1);
      grid.add(new Label(String.format("%.1f kcal", info.calories())), 1, 1);
      grid.add(new Label("Protein"), 0, 2);
      grid.add(new Label(String.format("%.1f g", info.protein())), 1, 2);
      grid.add(new Label("Fat"), 0, 3);
      grid.add(new Label(String.format("%.1f g", info.fat())), 1, 3);
      grid.add(new Label("Carbs"), 0, 4);
      grid.add(new Label(String.format("%.1f g", info.carbohydrates())), 1, 4);
      grid.add(new Label("Fiber"), 0, 5);
      grid.add(new Label(String.format("%.1f g", info.fiber())), 1, 5);

      Label source = new Label("Source: USDA FoodData Central");
      source.setStyle("-fx-font-style: italic; -fx-font-size: 10;");
      grid.add(source, 0, 6, 2, 1);

      javafx.scene.control.Alert dialog =
          new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
      dialog.setTitle("Nutrition Info");
      dialog.setHeaderText(info.description());
      dialog.getDialogPane().setContent(grid);
      dialog.showAndWait();
    }

    /** Removes all bindings and listeners from the currently bound item. */
    private void unbind() {
      if (boundItem != null) {
        nameField.textProperty().unbindBidirectional(boundItem.nameProperty());
        amountField.textProperty().unbindBidirectional(boundItem.amountProperty());
        vagueCheck.selectedProperty().unbindBidirectional(boundItem.vagueProperty());
        amountField.disableProperty().unbind();
        unitCombo.disableProperty().unbind();
        if (unitComboListener != null) {
          unitCombo.valueProperty().removeListener(unitComboListener);
          unitComboListener = null;
        }
        if (unitPropListener != null) {
          boundItem.unitProperty().removeListener(unitPropListener);
          unitPropListener = null;
        }
        boundItem = null;
      }
    }

    /** Binds all controls to the given ingredient. */
    private void bindTo(EditableIngredient item) {
      nameField.textProperty().bindBidirectional(item.nameProperty());
      amountField.textProperty().bindBidirectional(item.amountProperty());
      vagueCheck.selectedProperty().bindBidirectional(item.vagueProperty());
      amountField.disableProperty().bind(item.vagueProperty());
      unitCombo.disableProperty().bind(item.vagueProperty());

      // Default the ComboBox display to CUP when unit is blank, but only mutate the model for
      // non-vague ingredients — vague ingredients have no unit and setting one would falsely
      // mark the recipe dirty without any user action.
      String displayUnit = item.getUnit().isBlank() ? Unit.CUP.getAbbreviation() : item.getUnit();
      if (item.getUnit().isBlank() && !item.isVague()) {
        item.unitProperty().set(Unit.CUP.getAbbreviation());
      }
      unitCombo.setValue(displayUnit);

      unitComboListener = (obs, oldVal, newVal) -> item.unitProperty().set(newVal);
      unitCombo.valueProperty().addListener(unitComboListener);

      unitPropListener =
          (obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(unitCombo.getValue())) {
              unitCombo.setValue(newVal);
            }
          };
      item.unitProperty().addListener(unitPropListener);

      boundItem = item;
    }
  }
}
