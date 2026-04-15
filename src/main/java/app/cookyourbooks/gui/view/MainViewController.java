package app.cookyourbooks.gui.view;

import java.util.EnumMap;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.NavigationService.View;
import app.cookyourbooks.model.UnitSystem;

/**
 * Controller for the main application layout ({@code MainView.fxml}).
 *
 * <p>Manages the sidebar navigation buttons and swaps the content area when the user navigates
 * between features. Each feature's view is provided via {@link #setViewNode(View, Node)} during
 * application startup.
 *
 * <h2>How navigation works</h2>
 *
 * <ol>
 *   <li>The user clicks a sidebar button (e.g., "Library")
 *   <li>The button handler calls {@link NavigationService#navigateTo(View)}
 *   <li>The navigation listener in this controller swaps the content area to show the corresponding
 *       feature view
 * </ol>
 */
@SuppressWarnings(
    "NullAway.Init") // FXML fields are injected by the FXMLLoader, not the constructor
public class MainViewController {

  @FXML private StackPane contentArea;
  @FXML private Button libraryButton;
  @FXML private Button editorButton;
  @FXML private Button importButton;
  @FXML private Button searchButton;
  @FXML private ComboBox<UnitSystem> unitSystemComboBox;
  @FXML private ToggleButton darkModeToggle;

  private final NavigationService navigationService;
  private final Map<View, Node> viewNodes = new EnumMap<>(View.class);

  /**
   * Constructs the main view controller.
   *
   * @param navigationService the shared navigation service
   */
  public MainViewController(NavigationService navigationService) {
    this.navigationService = navigationService;
  }

  /**
   * Registers a feature view's root node for a given navigation view.
   *
   * <p>Call this during app startup for each feature that has been implemented. Views that are not
   * registered will show a placeholder when navigated to.
   *
   * @param view the navigation view
   * @param node the root node of the feature's FXML view
   */
  public void setViewNode(View view, Node node) {
    viewNodes.put(view, node);
  }

  /** Called by FXML after the layout is loaded. Sets up button handlers and navigation listener. */
  @SuppressWarnings("UnusedMethod") // Called reflectively by FXMLLoader
  @FXML
  private void initialize() {
    libraryButton.setOnAction(e -> navigationService.navigateTo(View.LIBRARY));
    editorButton.setOnAction(e -> navigationService.navigateTo(View.RECIPE_EDITOR));
    importButton.setOnAction(e -> navigationService.navigateTo(View.IMPORT));
    searchButton.setOnAction(e -> navigationService.navigateTo(View.SEARCH));
    unitSystemComboBox.getItems().setAll(UnitSystem.IMPERIAL, UnitSystem.METRIC);
    unitSystemComboBox.setValue(navigationService.getUnitSystem());
    unitSystemComboBox
        .valueProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null && newValue != navigationService.getUnitSystem()) {
                navigationService.setUnitSystem(newValue);
              }
            });
    navigationService
        .unitSystemProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null && newValue != unitSystemComboBox.getValue()) {
                unitSystemComboBox.setValue(newValue);
              }
            });

    // Add keyboard support: Enter/Space should activate buttons
    libraryButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            libraryButton.fire();
            event.consume();
          }
        });
    editorButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            editorButton.fire();
            event.consume();
          }
        });
    importButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            importButton.fire();
            event.consume();
          }
        });
    searchButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            searchButton.fire();
            event.consume();
          }
        });

    // Wire dark mode toggle
    darkModeToggle.setSelected(navigationService.isDarkMode());
    darkModeToggle
        .selectedProperty()
        .addListener(
            (obs, wasSelected, isSelected) -> {
              navigationService.setDarkMode(isSelected);
              applyTheme(isSelected);
            });

    // Listen for navigation changes and swap the content area
    navigationService
        .currentViewProperty()
        .addListener((obs, oldView, newView) -> showView(newView));

    // Show the initial view
    showView(navigationService.getCurrentView());
  }

  private void applyTheme(boolean dark) {
    String darkCss = getClass().getResource("/css/dark.css").toExternalForm();

    // Each FXML file loads cookyourbooks.css at node level, which outranks scene-level
    // stylesheets. So we must add dark.css to every node that carries its own stylesheet list:
    // 1. The scene root (BorderPane from MainView.fxml) — covers content-area, sidebar
    // 2. Each feature view node (HBox/VBox roots from LibraryView.fxml, etc.)
    // 3. The scene itself, as a fallback for any remaining nodes
    addOrRemove(contentArea.getScene().getRoot().getStylesheets(), darkCss, dark);
    for (Node node : viewNodes.values()) {
      if (node instanceof Parent parent) {
        addOrRemove(parent.getStylesheets(), darkCss, dark);
      }
    }
    addOrRemove(contentArea.getScene().getStylesheets(), darkCss, dark);

    darkModeToggle.setText(dark ? "Light Mode" : "Dark Mode");
  }

  private static void addOrRemove(
      javafx.collections.ObservableList<String> list, String item, boolean add) {
    if (add) {
      if (!list.contains(item)) {
        list.add(item);
      }
    } else {
      list.remove(item);
    }
  }

  private void showView(View view) {
    contentArea.getChildren().clear();
    Node node = viewNodes.get(view);
    if (node != null) {
      contentArea.getChildren().add(node);
    } else {
      // Placeholder for features not yet implemented
      Label placeholder = new Label(view.name() + " — not yet implemented");
      placeholder.setStyle("-fx-text-fill: #888; -fx-font-size: 16;");
      contentArea.getChildren().add(placeholder);
    }

    // Update button styles to highlight the active view
    libraryButton.getStyleClass().remove("nav-active");
    editorButton.getStyleClass().remove("nav-active");
    importButton.getStyleClass().remove("nav-active");
    searchButton.getStyleClass().remove("nav-active");

    switch (view) {
      case LIBRARY -> libraryButton.getStyleClass().add("nav-active");
      case RECIPE_EDITOR -> editorButton.getStyleClass().add("nav-active");
      case IMPORT -> importButton.getStyleClass().add("nav-active");
      case SEARCH -> searchButton.getStyleClass().add("nav-active");
      default -> {
        /* no additional views */
      }
    }
  }
}
