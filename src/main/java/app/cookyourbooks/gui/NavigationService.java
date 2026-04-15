package app.cookyourbooks.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.UnitSystem;

/**
 * Shared navigation state for the CookYourBooks GUI.
 *
 * <p>This service holds the current view and any context needed for cross-feature navigation (e.g.,
 * which recipe to open in the editor). It is shared across all feature ViewModels and the main
 * layout controller.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Navigate to a view
 * navigationService.navigateTo(NavigationService.View.LIBRARY);
 *
 * // Navigate to a specific recipe in the editor
 * navigationService.navigateToRecipe("recipe-id-123");
 *
 * // Listen for navigation changes in MainViewController
 * navigationService.currentViewProperty().addListener((obs, oldView, newView) -> {
 *     // swap the content pane
 * });
 * }</pre>
 */
public class NavigationService {

  /** The available views in the application. */
  public enum View {
    LIBRARY,
    RECIPE_EDITOR,
    IMPORT,
    SEARCH
  }

  private final ObjectProperty<View> currentView = new SimpleObjectProperty<>(View.LIBRARY);
  private final StringProperty selectedRecipeId = new SimpleStringProperty();
  private final ObjectProperty<UnitSystem> unitSystem =
      new SimpleObjectProperty<>(UnitSystem.IMPERIAL);
  private final BooleanProperty darkMode = new SimpleBooleanProperty(false);

  /**
   * Navigates to the specified view.
   *
   * @param view the view to navigate to
   */
  public void navigateTo(View view) {
    currentView.set(view);
  }

  /**
   * Navigates to the Recipe Editor with the specified recipe selected.
   *
   * @param recipeId the ID of the recipe to open
   */
  public void navigateToRecipe(String recipeId) {
    selectedRecipeId.set(recipeId);
    currentView.set(View.RECIPE_EDITOR);
  }

  /** Returns the current view property (for binding/listening). */
  public ObjectProperty<View> currentViewProperty() {
    return currentView;
  }

  /** Returns the current view. */
  public View getCurrentView() {
    return currentView.get();
  }

  /** Returns the selected recipe ID property (set when navigating to the editor). */
  public StringProperty selectedRecipeIdProperty() {
    return selectedRecipeId;
  }

  /** Returns the currently selected recipe ID, or null if none. */
  public @Nullable String getSelectedRecipeId() {
    return selectedRecipeId.get();
  }

  /** Returns the shared unit-system property (for binding/listening across feature ViewModels). */
  public ObjectProperty<UnitSystem> unitSystemProperty() {
    return unitSystem;
  }

  /** Returns the current app-wide unit system. */
  public UnitSystem getUnitSystem() {
    return unitSystem.get();
  }

  /** Sets the current app-wide unit system. */
  public void setUnitSystem(UnitSystem unitSystem) {
    this.unitSystem.set(unitSystem);
  }

  /** Returns the dark mode property (for binding/listening). */
  public BooleanProperty darkModeProperty() {
    return darkMode;
  }

  /** Returns whether dark mode is currently enabled. */
  public boolean isDarkMode() {
    return darkMode.get();
  }

  /** Sets dark mode on or off. */
  public void setDarkMode(boolean enabled) {
    darkMode.set(enabled);
  }
}
