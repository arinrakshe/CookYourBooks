package app.cookyourbooks.gui.controller;

import java.io.File;
import java.util.Objects;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.viewmodel.ImportViewModel;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.VagueIngredient;

public class ImportController {

  private @Nullable ImportViewModel viewModel;
  private @Nullable Timeline collectionRefreshTimeline;

  @FXML private @Nullable TextField titleField;
  @FXML private @Nullable ListView<Ingredient> ingredientsListView;
  @FXML private @Nullable ComboBox<RecipeCollection> collectionComboBox;
  @FXML private @Nullable Label statusLabel;
  @FXML private @Nullable Label errorLabel;
  @FXML private @Nullable Label stateLabel;
  @FXML private @Nullable Label progressLabel;
  @FXML private @Nullable ProgressBar progressBar;
  @FXML private @Nullable Button startButton;
  @FXML private @Nullable Button cancelButton;
  @FXML private @Nullable Button addIngredientButton;
  @FXML private @Nullable Button acceptButton;
  @FXML private @Nullable Button rejectButton;

  public ImportController() {}

  @FXML
  private void initialize() {
    Objects.requireNonNull(ingredientsListView, "ingredientsListView must be injected by FXML");
    Objects.requireNonNull(titleField, "titleField must be injected by FXML");
    Objects.requireNonNull(collectionComboBox, "collectionComboBox must be injected by FXML");
    Objects.requireNonNull(statusLabel, "statusLabel must be injected by FXML");
    Objects.requireNonNull(errorLabel, "errorLabel must be injected by FXML");
    Objects.requireNonNull(stateLabel, "stateLabel must be injected by FXML");
    Objects.requireNonNull(progressLabel, "progressLabel must be injected by FXML");
    Objects.requireNonNull(progressBar, "progressBar must be injected by FXML");
    Objects.requireNonNull(startButton, "startButton must be injected by FXML");
    Objects.requireNonNull(cancelButton, "cancelButton must be injected by FXML");
    Objects.requireNonNull(addIngredientButton, "addIngredientButton must be injected by FXML");
    Objects.requireNonNull(acceptButton, "acceptButton must be injected by FXML");
    Objects.requireNonNull(rejectButton, "rejectButton must be injected by FXML");
    titleField.setAccessibleText("Recipe title");
    ingredientsListView.setAccessibleText("Ingredients list");
    collectionComboBox.setAccessibleText("Target collection");
    progressBar.setAccessibleText("Import progress");
    ingredientsListView.setEditable(true);
    ingredientsListView.setCellFactory(
        listView ->
            new TextFieldListCell<>(
                new StringConverter<Ingredient>() {
                  @Override
                  public String toString(Ingredient ingredient) {
                    return ingredient == null ? "" : ingredient.getName();
                  }

                  @Override
                  public Ingredient fromString(String text) {
                    String trimmed = text == null ? "" : text.trim();
                    return new VagueIngredient(trimmed, null, null, null);
                  }
                }));
    ingredientsListView.setOnEditCommit(
        event -> {
          Ingredient newValue = event.getNewValue();
          String newName = newValue == null ? "" : newValue.getName();
          String trimmedName = newName == null ? "" : newName.trim();
          Objects.requireNonNull(
              ingredientsListView, "ingredientsListView must be injected by FXML");

          int index = event.getIndex();
          ObservableList<Ingredient> items = ingredientsListView.getItems();
          Ingredient oldValue = (index >= 0 && index < items.size()) ? items.get(index) : null;

          if (trimmedName.isEmpty()) {
            // Reject the edit: keep the old value and show a validation message.
            if (index >= 0 && index < items.size() && oldValue != null) {
              items.set(index, oldValue);
            }
            if (errorLabel != null) {
              errorLabel.setText("Ingredient name cannot be blank");
            }
          } else {
            // Accept the edit and update the list.
            if (index >= 0 && index < items.size()) {
              items.set(index, newValue);
            }
            if (errorLabel != null) {
              errorLabel.setText("");
            }
          }
        });
    ingredientsListView.setOnKeyPressed(
        event -> {
          ListView<Ingredient> listView =
              Objects.requireNonNull(ingredientsListView, "ingredientsListView must be injected");
          int selectedIndex = listView.getSelectionModel().getSelectedIndex();

          switch (event.getCode()) {
            case ENTER -> {
              // Enter: start editing the selected ingredient
              if (selectedIndex >= 0) {
                ingredientsListView.edit(selectedIndex);
                event.consume();
              }
            }
            case ESCAPE -> {
              // Escape: cancel current edit and lose focus from text field
              if (ingredientsListView.getEditingIndex() >= 0) {
                ingredientsListView.edit(-1);
              }
              event.consume();
            }
            case DELETE -> {
              // Delete: remove the selected ingredient with confirmation
              if (selectedIndex >= 0) {
                ObservableList<Ingredient> items = listView.getItems();
                if (selectedIndex < items.size()) {
                  Ingredient removed = items.remove(selectedIndex);
                  if (errorLabel != null) {
                    errorLabel.setText(
                        String.format("Removed '%s'. Press Ctrl+Z to undo.", removed.getName()));
                  }
                  event.consume();
                }
              }
            }
            case UP, DOWN, LEFT, RIGHT -> {
              // Arrow keys: allow default ListView navigation
            }
            default -> {
              // No special handling for other keys.
            }
          }
        });

    // Add keyboard support: Enter/Space should activate buttons
    startButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            Objects.requireNonNull(startButton, "startButton must be injected by FXML").fire();
            event.consume();
          }
        });

    cancelButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            Objects.requireNonNull(cancelButton, "cancelButton must be injected by FXML").fire();
            event.consume();
          }
        });

    addIngredientButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            Objects.requireNonNull(
                    addIngredientButton, "addIngredientButton must be injected by FXML")
                .fire();
            event.consume();
          }
        });

    acceptButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            Objects.requireNonNull(acceptButton, "acceptButton must be injected by FXML").fire();
            event.consume();
          }
        });

    rejectButton.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            Objects.requireNonNull(rejectButton, "rejectButton must be injected by FXML").fire();
            event.consume();
          }
        });
  }

  public void setViewModel(ImportViewModel viewModel) {
    this.viewModel = viewModel;
    bindViewModel();
  }

  private void bindViewModel() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling bindViewModel");
    Objects.requireNonNull(statusLabel, "statusLabel must be injected by FXML");
    Objects.requireNonNull(errorLabel, "errorLabel must be injected by FXML");
    Objects.requireNonNull(titleField, "titleField must be injected by FXML");
    Objects.requireNonNull(ingredientsListView, "ingredientsListView must be injected by FXML");
    Objects.requireNonNull(collectionComboBox, "collectionComboBox must be injected by FXML");
    Objects.requireNonNull(stateLabel, "stateLabel must be injected by FXML");
    Objects.requireNonNull(progressLabel, "progressLabel must be injected by FXML");
    Objects.requireNonNull(progressBar, "progressBar must be injected by FXML");

    statusLabel.textProperty().bind(viewModel.statusMessageProperty());
    errorLabel.textProperty().bind(viewModel.errorMessageProperty());
    titleField.textProperty().bindBidirectional(viewModel.importedTitleProperty());
    stateLabel.textProperty().bind(viewModel.currentStateProperty());
    progressBar.progressProperty().bind(viewModel.importProgressProperty());
    progressLabel
        .textProperty()
        .bind(Bindings.format("%.0f%%", viewModel.importProgressProperty().multiply(100)));
    ingredientsListView.setItems(ingredientItems());
    collectionComboBox.setItems(collectionItems());

    collectionComboBox.setConverter(
        new StringConverter<RecipeCollection>() {
          @Override
          public String toString(RecipeCollection collection) {
            return collection == null ? "" : collection.getTitle();
          }

          @Override
          public RecipeCollection fromString(String string) {
            throw new UnsupportedOperationException();
          }
        });

    collectionComboBox
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              Objects.requireNonNull(
                  viewModel, "viewModel must be set before calling bindViewModel");
              if (newValue != null) {
                viewModel.selectTargetCollection(newValue.getId());
              }
            });

    viewModel.loadCollections();
    startCollectionRefresh();

    if (!collectionComboBox.getItems().isEmpty()) {
      collectionComboBox.getSelectionModel().selectFirst();
      RecipeCollection selected = collectionComboBox.getSelectionModel().getSelectedItem();
      if (selected != null) {
        viewModel.selectTargetCollection(selected.getId());
      }
    }
  }

  private void startCollectionRefresh() {
    stopCollectionRefresh();

    collectionRefreshTimeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(3),
                event -> {
                  if (viewModel == null || collectionComboBox == null) {
                    return;
                  }

                  RecipeCollection previouslySelected =
                      collectionComboBox.getSelectionModel().getSelectedItem();

                  viewModel.loadCollections();

                  if (previouslySelected != null) {
                    for (RecipeCollection collection : collectionComboBox.getItems()) {
                      if (collection.getId().equals(previouslySelected.getId())) {
                        collectionComboBox.getSelectionModel().select(collection);
                        return;
                      }
                    }
                  }

                  if (!collectionComboBox.getItems().isEmpty()
                      && collectionComboBox.getSelectionModel().getSelectedItem() == null) {
                    collectionComboBox.getSelectionModel().selectFirst();
                  }
                }));

    collectionRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    collectionRefreshTimeline.play();
  }

  private void stopCollectionRefresh() {
    if (collectionRefreshTimeline != null) {
      collectionRefreshTimeline.stop();
      collectionRefreshTimeline = null;
    }
  }

  private ObservableList<Ingredient> ingredientItems() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling ingredientItems");
    return (ObservableList<Ingredient>) viewModel.importedIngredientsProperty();
  }

  private ObservableList<RecipeCollection> collectionItems() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling collectionItems");
    return (ObservableList<RecipeCollection>) viewModel.availableCollectionsProperty();
  }

  @FXML
  private void handleStartImport() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling handleStartImport");
    Objects.requireNonNull(titleField, "titleField must be injected by FXML");

    FileChooser chooser = new FileChooser();
    chooser.setTitle("Select Recipe Image");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));

    File file = chooser.showOpenDialog(titleField.getScene().getWindow());
    if (file != null) {
      viewModel.startImport(file.toPath());
    }
  }

  @FXML
  private void handleAccept() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling handleAccept");
    Objects.requireNonNull(collectionComboBox, "collectionComboBox must be injected by FXML");

    RecipeCollection selected = collectionComboBox.getSelectionModel().getSelectedItem();
    if (selected != null) {
      viewModel.selectTargetCollection(selected.getId());
    }

    viewModel.acceptImport();
  }

  @FXML
  private void handleReject() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling handleReject");
    viewModel.rejectImport();
  }

  @FXML
  private void handleCancel() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling handleCancel");
    viewModel.cancelImport();
  }

  @FXML
  private void handleAddIngredient() {
    Objects.requireNonNull(viewModel, "viewModel must be set before calling handleAddIngredient");

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Add Ingredient");
    dialog.setHeaderText("Add a new ingredient");
    dialog.setContentText("Ingredient name:");

    dialog
        .showAndWait()
        .ifPresent(
            name -> {
              String trimmed = name.trim();
              Objects.requireNonNull(
                  ingredientsListView, "ingredientsListView must be injected by FXML");
              if (!trimmed.isEmpty()) {
                ingredientsListView.getItems().add(new VagueIngredient(trimmed, null, null, null));
              }
            });
  }
}
