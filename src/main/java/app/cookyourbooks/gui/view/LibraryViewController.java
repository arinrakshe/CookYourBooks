package app.cookyourbooks.gui.view;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import app.cookyourbooks.gui.viewmodel.LibraryCollectionRow;
import app.cookyourbooks.gui.viewmodel.LibraryRecipeRow;
import app.cookyourbooks.gui.viewmodel.LibraryViewModel;
import app.cookyourbooks.model.SourceType;

/** Controller for {@code LibraryView.fxml}; binds to {@link LibraryViewModel}. */
@SuppressWarnings("NullAway.Init") // FXML fields injected by FXMLLoader
public class LibraryViewController {

  private final LibraryViewModel viewModel;
  private final ObjectProperty<LibraryCollectionRow> selectedCollectionProperty =
      new SimpleObjectProperty<>();

  @FXML private TextField filterField;
  @FXML private ProgressIndicator loadingIndicator;
  @FXML private ScrollPane collectionScroll;
  @FXML private FlowPane collectionCardGrid;
  @FXML private ListView<LibraryRecipeRow> recipeList;
  @FXML private Button newCollectionButton;
  @FXML private Button deleteCollectionButton;
  @FXML private Button undoButton;
  @FXML private Label undoLabel;
  @FXML private Button openRecipeButton;

  public LibraryViewController(LibraryViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @SuppressWarnings("UnusedMethod") // FXMLLoader invokes via reflection
  @FXML
  private void initialize() {
    filterField.setId("libraryFilter");
    loadingIndicator.setId("libraryLoading");
    collectionScroll.setId("libraryCollections");
    recipeList.setId("libraryRecipes");
    newCollectionButton.setId("libraryNewCollection");
    deleteCollectionButton.setId("libraryDeleteCollection");
    undoButton.setId("libraryUndo");
    openRecipeButton.setId("libraryOpenRecipe");

    filterField.textProperty().bindBidirectional(viewModel.filterTextProperty());

    loadingIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    loadingIndicator.setMaxSize(28, 28);
    loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
    loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

    collectionScroll.setFitToWidth(true);

    @SuppressWarnings("unchecked")
    var collectionItems = (ObservableList<LibraryCollectionRow>) viewModel.collectionsProperty();
    collectionItems.addListener(
        (ListChangeListener<LibraryCollectionRow>) c -> rebuildCollectionCards(collectionItems));

    @SuppressWarnings("unchecked")
    var recipeItems = (ObservableList<LibraryRecipeRow>) viewModel.recipesProperty();
    recipeList.setItems(recipeItems);

    recipeList.setFixedCellSize(32);
    recipeList.setPlaceholder(new Label("Select a collection to see its recipes."));
    recipeList.setCellFactory(lv -> new RecipeTitleCell());

    undoButton.disableProperty().bind(viewModel.undoAvailableProperty().not());
    undoLabel.textProperty().bind(viewModel.undoMessageProperty());

    openRecipeButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> recipeList.getSelectionModel().getSelectedItem() == null,
                recipeList.getSelectionModel().selectedItemProperty()));

    deleteCollectionButton.disableProperty().bind(selectedCollectionProperty.isNull());

    newCollectionButton.setOnAction(
        e -> {
          TextInputDialog dialog = new TextInputDialog();
          dialog.setTitle("New collection");
          dialog.setHeaderText("Create a new recipe collection");
          dialog.setContentText("Title:");
          Optional<String> titleRaw = dialog.showAndWait();
          Optional<String> title = titleRaw.map(String::trim).filter(s -> !s.isBlank());
          if (title.isEmpty()) {
            return;
          }
          String name = title.get();
          Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
          confirm.setTitle("Confirm new collection");
          confirm.setHeaderText("Create collection \"" + name + "\"?");
          confirm.setContentText(
              "A new empty collection will be added to your library. You can add recipes later from"
                  + " other parts of the app.");
          Optional<ButtonType> answer = confirm.showAndWait();
          if (answer.isPresent() && answer.get() == ButtonType.OK) {
            viewModel.createCollection(name);
          }
        });

    newCollectionButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            newCollectionButton.fire();
            event.consume();
          }
        });

    deleteCollectionButton.setOnAction(
        e -> {
          LibraryCollectionRow selected = selectedCollectionProperty.get();
          if (selected == null) {
            return;
          }
          int n = selected.recipeCount();
          String recipeLine =
              n == 0
                  ? "This collection has no recipes."
                  : "This collection contains "
                      + n
                      + " recipe(s). They will no longer appear here.";
          Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
          alert.setTitle("Delete collection");
          alert.setHeaderText("Permanently remove \"" + selected.title() + "\"?");
          alert.setContentText(
              recipeLine
                  + " You can undo this for a few seconds after deleting.\n\n"
                  + "Are you sure you want to delete this collection?");
          Optional<ButtonType> answer = alert.showAndWait();
          if (answer.isPresent() && answer.get() == ButtonType.OK) {
            viewModel.deleteCollection(selected.id());
            selectedCollectionProperty.set(null);
            rebuildCollectionCards(collectionItems);
          }
        });

    deleteCollectionButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            deleteCollectionButton.fire();
            event.consume();
          }
        });

    undoButton.setOnAction(
        e -> {
          viewModel.undoDelete();
          rebuildCollectionCards(collectionItems);
        });

    undoButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            undoButton.fire();
            event.consume();
          }
        });

    openRecipeButton.setOnAction(
        e -> {
          LibraryRecipeRow r = recipeList.getSelectionModel().getSelectedItem();
          if (r != null) {
            viewModel.selectRecipe(r.id());
          }
        });

    openRecipeButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            openRecipeButton.fire();
            event.consume();
          }
        });

    recipeList.setOnMouseClicked(
        ev -> {
          if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
            LibraryRecipeRow r = recipeList.getSelectionModel().getSelectedItem();
            if (r != null) {
              viewModel.selectRecipe(r.id());
            }
          }
        });

    viewModel.refresh();
    rebuildCollectionCards(collectionItems);
  }

  private void rebuildCollectionCards(ObservableList<LibraryCollectionRow> rows) {
    collectionCardGrid.getChildren().clear();

    if (rows.isEmpty()) {
      Label empty =
          new Label(
              "No collections match the filter (or the library is empty).\nTry clearing the filter or add a new collection.");
      empty.getStyleClass().add("library-collection-empty");
      empty.setWrapText(true);
      collectionCardGrid.getChildren().add(empty);
      paintSelectionHighlight();
      return;
    }

    LibraryCollectionRow current = selectedCollectionProperty.get();
    String vmSelected = viewModel.getSelectedCollectionId();

    if (current != null && rows.stream().noneMatch(r -> r.id().equals(current.id()))) {
      selectedCollectionProperty.set(null);
    }
    if (selectedCollectionProperty.get() == null && vmSelected != null) {
      rows.stream()
          .filter(r -> r.id().equals(vmSelected))
          .findFirst()
          .ifPresent(selectedCollectionProperty::set);
    }

    for (LibraryCollectionRow row : rows) {
      VBox card = createCollectionCard(row);
      card.setUserData(row);
      collectionCardGrid.getChildren().add(card);
    }
    paintSelectionHighlight();
  }

  private VBox createCollectionCard(LibraryCollectionRow row) {
    VBox card = new VBox(8);
    card.setAlignment(Pos.TOP_CENTER);
    card.getStyleClass().add("library-collection-card");
    card.setFocusTraversable(true);

    Label icon = new Label(sourceTypeGlyph(row.sourceType()));
    icon.getStyleClass().add("library-collection-card-icon");
    icon.setMouseTransparent(true);

    Label title = new Label(row.title());
    title.getStyleClass().add("library-card-title");
    title.setWrapText(true);
    title.setMouseTransparent(true);
    title.setMaxWidth(118);

    Label meta = new Label(row.recipeCount() + " recipes · " + formatSourceType(row.sourceType()));
    meta.getStyleClass().add("library-card-meta");
    meta.setMouseTransparent(true);
    meta.setWrapText(true);
    meta.setMaxWidth(118);

    card.getChildren().addAll(icon, title, meta);

    Runnable select =
        () -> {
          selectedCollectionProperty.set(row);
          viewModel.selectCollection(row.id());
          paintSelectionHighlight();
        };
    card.setOnMouseClicked(
        ev -> {
          if (ev.getButton() == MouseButton.PRIMARY) {
            select.run();
          }
        });
    card.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
            select.run();
            e.consume();
          }
        });

    return card;
  }

  private void paintSelectionHighlight() {
    LibraryCollectionRow selected = selectedCollectionProperty.get();
    for (var node : collectionCardGrid.getChildren()) {
      if (!(node instanceof VBox card)) {
        continue;
      }
      card.getStyleClass().remove("library-collection-card-selected");
      Object ud = card.getUserData();
      if (selected != null
          && ud instanceof LibraryCollectionRow row
          && row.id().equals(selected.id())) {
        card.getStyleClass().add("library-collection-card-selected");
      }
    }
  }

  private static String sourceTypeGlyph(SourceType st) {
    return switch (st) {
      case PERSONAL -> "📝";
      case PUBLISHED_BOOK -> "📗";
      case WEBSITE -> "🌐";
    };
  }

  private static String formatSourceType(SourceType st) {
    return switch (st) {
      case PERSONAL -> "Personal";
      case PUBLISHED_BOOK -> "Cookbook";
      case WEBSITE -> "Web";
    };
  }

  private static final class RecipeTitleCell extends ListCell<LibraryRecipeRow> {
    @Override
    protected void updateItem(LibraryRecipeRow item, boolean empty) {
      super.updateItem(item, empty);
      setGraphic(null);
      if (empty || item == null) {
        setText(null);
      } else {
        setText(item.title());
      }
    }
  }
}
