package app.cookyourbooks.gui.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import app.cookyourbooks.gui.viewmodel.SearchResult;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl;

/**
 * FXML controller for {@code SearchView.fxml}.
 *
 * <p>Binds UI elements to {@link SearchViewModelImpl} properties and delegates all business logic
 * to the ViewModel.
 */
@SuppressWarnings("NullAway.Init")
public class SearchViewController {

  @FXML private TextField queryField;
  @FXML private ProgressIndicator progressIndicator;
  @FXML private Button clearButton;
  @FXML private Label statusLabel;

  @FXML private TextField filterInputField;
  @FXML private Button addFilterButton;
  @FXML private ListView<String> filterListView;
  @FXML private Button removeFilterButton;

  @FXML private ListView<SearchResult> resultsListView;

  private final SearchViewModelImpl viewModel;

  public SearchViewController(SearchViewModelImpl viewModel) {
    this.viewModel = viewModel;
  }

  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    // Set fx:id values used by tests
    queryField.setId("search-query-field");
    resultsListView.setId("search-results-list");
    filterInputField.setId("search-filter-input");
    filterListView.setId("search-filter-list");
    statusLabel.setId("search-status-label");
    addFilterButton.setId("search-add-filter-button");
    removeFilterButton.setId("search-remove-filter-button");
    clearButton.setId("search-clear-button");
    progressIndicator.setId("search-progress");

    // ── Bind query field ──────────────────────────────────────────────
    // When the user types, notify the ViewModel (triggers debounced search).
    queryField.textProperty().addListener((obs, oldVal, newVal) -> viewModel.setQuery(newVal));

    // ── Bind results list ─────────────────────────────────────────────
    resultsListView.setItems(viewModel.resultsProperty());

    // Drive ListView selection FROM the ViewModel (one-way: ViewModel → View).
    viewModel
        .selectedIndexProperty()
        .addListener(
            (obs, oldIdx, newIdx) -> {
              int idx = newIdx.intValue();
              if (idx >= 0 && idx < resultsListView.getItems().size()) {
                resultsListView.getSelectionModel().select(idx);
                resultsListView.scrollTo(idx);
              } else {
                resultsListView.getSelectionModel().clearSelection();
              }
            });

    // Keyboard navigation in the search field (user types then navigates without leaving focus).
    queryField.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case DOWN -> {
              viewModel.selectNextResult();
              event.consume();
            }
            case UP -> {
              viewModel.selectPreviousResult();
              event.consume();
            }
            case ENTER -> {
              viewModel.navigateToSelectedResult();
              event.consume();
            }
            default -> {}
          }
        });

    // Keyboard navigation on the results list (user clicks into list then navigates).
    resultsListView.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case UP -> {
              viewModel.selectPreviousResult();
              event.consume();
            }
            case DOWN -> {
              viewModel.selectNextResult();
              event.consume();
            }
            case ENTER -> {
              viewModel.navigateToSelectedResult();
              event.consume();
            }
            default -> {}
          }
        });

    // ── Bind progress indicator and status label ──────────────────────
    progressIndicator.visibleProperty().bind(viewModel.searchingProperty());
    progressIndicator.managedProperty().bind(viewModel.searchingProperty());
    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    // ── Bind ingredient filter list ───────────────────────────────────
    filterListView.setItems(viewModel.ingredientFiltersProperty());

    // Add filter on button click or Enter in the filter input field.
    addFilterButton.setOnAction(e -> addFilter());
    filterInputField.setOnAction(e -> addFilter());

    addFilterButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            addFilterButton.fire();
            event.consume();
          }
        });

    // Remove selected filter.
    removeFilterButton.setOnAction(
        e -> {
          String selected = filterListView.getSelectionModel().getSelectedItem();
          if (selected != null) {
            viewModel.removeIngredientFilter(selected);
          }
        });

    removeFilterButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            removeFilterButton.fire();
            event.consume();
          }
        });

    // Clear button resets everything.
    clearButton.setOnAction(
        e -> {
          queryField.clear();
          filterInputField.clear();
          viewModel.clearFilters();
        });

    clearButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            clearButton.fire();
            event.consume();
          }
        });
  }

  private void addFilter() {
    String ingredient = filterInputField.getText().trim();
    if (!ingredient.isBlank()) {
      viewModel.addIngredientFilter(ingredient);
      filterInputField.clear();
    }
  }
}
