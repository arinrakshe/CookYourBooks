package app.cookyourbooks.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import app.cookyourbooks.gui.viewmodel.LibraryRecipeRow;

/**
 * Integration tests that exercise navigation and data flow across Library, Recipe Editor, Import,
 * and Search. Each nested suite uses {@link CookYourBooksGuiApp#resolveLibraryPath()} via {@code
 * cookyourbooks.test.library.path} so tests never read or write the project {@code
 * cyb-library.json}.
 */
class GuiCrossFeatureIntegrationTest {

  private static void installIsolatedLibraryAndLaunch(Stage stage, Path tempDir)
      throws IOException {
    Path lib = tempDir.resolve("cyb-library.json");
    Files.deleteIfExists(lib);
    System.setProperty("cookyourbooks.test.library.path", lib.toAbsolutePath().toString());
    new CookYourBooksGuiApp().start(stage);
  }

  private static void clearLibraryProperty() {
    System.clearProperty("cookyourbooks.test.library.path");
  }

  private static void waitForSearchResults(FxRobot robot) {
    try {
      WaitForAsyncUtils.waitFor(
          5,
          TimeUnit.SECONDS,
          () -> {
            try {
              ListView<?> list = robot.lookup("#search-results-list").queryAs(ListView.class);
              return !list.getItems().isEmpty();
            } catch (RuntimeException e) {
              return false;
            }
          });
    } catch (Exception e) {
      throw new AssertionError("Timed out waiting for search results", e);
    }
  }

  @Nested
  @DisplayName("Sidebar navigation exposes all four feature surfaces")
  @ExtendWith(ApplicationExtension.class)
  @SuppressWarnings("NullAway.Init") // @TempDir is injected by JUnit before @Start
  class SidebarNavigationSurfaces {

    @TempDir private Path tempDir;

    @Start
    void start(Stage stage) throws IOException {
      installIsolatedLibraryAndLaunch(stage, tempDir);
    }

    @AfterEach
    void tearDown() {
      clearLibraryProperty();
    }

    @Test
    @DisplayName("Library view shows collection chrome")
    void libraryViewShowsCollectionUi(FxRobot robot) {
      robot.clickOn("#libraryButton");
      Node filter = robot.lookup("#libraryFilter").query();
      Node collections = robot.lookup("#libraryCollections").query();
      org.junit.jupiter.api.Assertions.assertNotNull(filter);
      org.junit.jupiter.api.Assertions.assertNotNull(collections);
    }

    @Test
    @DisplayName("Search view shows query and filter controls")
    void searchViewShowsSearchChrome(FxRobot robot) {
      robot.clickOn("#searchButton");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("#search-query-field").query());
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("#search-results-list").query());
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("#search-filter-input").query());
    }

    @Test
    @DisplayName("Import view shows import workflow controls")
    void importViewShowsImportChrome(FxRobot robot) {
      robot.clickOn("#importButton");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("Choose Image").query());
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("Accept Import").query());
    }

    @Test
    @DisplayName("Recipe Editor view shows Edit and Ingredients when opened from sidebar")
    void recipeEditorViewShowsEditorChrome(FxRobot robot) {
      robot.clickOn("#editorButton");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("Edit").query());
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("Ingredients").query());
    }

    @Test
    @DisplayName("After visiting Import, Library still lists seeded collections")
    void libraryRemainsAvailableAfterImportView(FxRobot robot) {
      robot.clickOn("#importButton");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("Target Collection").query());

      robot.clickOn("#libraryButton");
      robot.clickOn("Weeknight Favorites");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("#libraryRecipes").query());
    }

    @Test
    @DisplayName("Round-trip: Library → Import → Search → Library")
    void navigateAcrossImportSearchAndBackToLibrary(FxRobot robot) {
      robot.clickOn("#libraryButton");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("#libraryFilter").query());

      robot.clickOn("#importButton");
      robot.clickOn("Cancel");

      robot.clickOn("#searchButton");
      robot.clickOn("#search-query-field");
      robot.write("Salad");
      waitForSearchResults(robot);

      robot.clickOn("#libraryButton");
      robot.clickOn("Weeknight Favorites");
      org.junit.jupiter.api.Assertions.assertNotNull(robot.lookup("#libraryRecipes").query());
    }
  }

  @Nested
  @DisplayName("Library ↔ Recipe Editor (open recipe from library)")
  @ExtendWith(ApplicationExtension.class)
  @SuppressWarnings("NullAway.Init") // @TempDir is injected by JUnit before @Start
  class LibraryToRecipeEditorFlow {

    @TempDir private Path tempDir;

    @Start
    void start(Stage stage) throws IOException {
      installIsolatedLibraryAndLaunch(stage, tempDir);
    }

    @AfterEach
    void tearDown() {
      clearLibraryProperty();
    }

    @Test
    @DisplayName("Selecting a seeded collection and opening a recipe loads the editor")
    void doubleClickRecipeNavigatesToEditorWithTitle(FxRobot robot) {
      robot.clickOn("#libraryButton");
      robot.clickOn("Weeknight Favorites");
      robot.doubleClickOn("One-Pot Tomato Pasta");

      Label title = robot.lookup(".recipe-title").queryAs(Label.class);
      Assertions.assertThat(title.getText()).isEqualTo("One-Pot Tomato Pasta");
    }

    @Test
    @DisplayName("Open recipe button navigates to editor with same recipe title")
    void openRecipeButtonLoadsEditor(FxRobot robot) {
      robot.clickOn("#libraryButton");
      robot.clickOn("Weeknight Favorites");

      @SuppressWarnings("unchecked")
      ListView<LibraryRecipeRow> recipes = robot.lookup("#libraryRecipes").queryAs(ListView.class);
      robot.interact(() -> recipes.getSelectionModel().select(0));
      robot.clickOn("#libraryOpenRecipe");

      Label title = robot.lookup(".recipe-title").queryAs(Label.class);
      Assertions.assertThat(title.getText()).isEqualTo("One-Pot Tomato Pasta");
    }
  }

  @Nested
  @DisplayName("Search ↔ Recipe Editor (open result from search)")
  @ExtendWith(ApplicationExtension.class)
  @SuppressWarnings("NullAway.Init") // @TempDir is injected by JUnit before @Start
  class SearchToRecipeEditorFlow {

    @TempDir private Path tempDir;

    @Start
    void start(Stage stage) throws IOException {
      installIsolatedLibraryAndLaunch(stage, tempDir);
    }

    @AfterEach
    void tearDown() {
      clearLibraryProperty();
    }

    @Test
    @DisplayName("Query + Enter opens selected result in the editor")
    void searchEnterOpensRecipeInEditor(FxRobot robot) {
      robot.clickOn("#searchButton");
      robot.clickOn("#search-query-field");
      robot.write("Tomato");
      waitForSearchResults(robot);
      robot.press(javafx.scene.input.KeyCode.ENTER);

      Label title = robot.lookup(".recipe-title").queryAs(Label.class);
      Assertions.assertThat(title.getText()).isEqualTo("One-Pot Tomato Pasta");
    }

    @Test
    @DisplayName("Ingredient filter narrows results; editor still opens correct recipe")
    void searchWithIngredientFilterStillOpensRecipe(FxRobot robot) {
      robot.clickOn("#searchButton");
      robot.clickOn("#search-query-field");
      robot.write("Muffin");
      waitForSearchResults(robot);

      robot.clickOn("#search-filter-input");
      robot.write("flour");
      robot.clickOn("#search-add-filter-button");
      waitForSearchResults(robot);
      robot.clickOn("#search-query-field");
      robot.press(javafx.scene.input.KeyCode.ENTER);

      Label title = robot.lookup(".recipe-title").queryAs(Label.class);
      Assertions.assertThat(title.getText()).isEqualTo("Morning Muffins");
    }
  }
}
