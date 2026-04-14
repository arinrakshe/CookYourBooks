package app.cookyourbooks.importInterfaceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.List;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.RecipeOcrService;

/** Test suite for ImportViewModelImpl covering all 10 requirements. */
public class ImportInterfaceTests {

  private RecipeOcrService ocrService;
  private LibrarianService librarianService;
  private ImportViewModelImpl viewModel;

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
    }
  }

  @BeforeEach
  void setUp() {
    ocrService = mock(RecipeOcrService.class);
    librarianService = mock(LibrarianService.class);
    viewModel = new ImportViewModelImpl(ocrService, librarianService);
  }

  // ===== I1: Initial state is idle; no imported recipe, no error =====
  @Test
  void testI1_initialStateIsIdle() {
    assertEquals("idle", viewModel.currentStateProperty().get());
    assertEquals("Ready", viewModel.statusMessageProperty().get());
    assertEquals("", viewModel.errorMessageProperty().get());
    assertEquals("", viewModel.importedTitleProperty().get());
    assertTrue(viewModel.importedIngredientsProperty().isEmpty());
    assertEquals(0.0, viewModel.importProgressProperty().get());
    assertEquals("idle", viewModel.getState());
    assertNull(viewModel.getErrorMessage());
    assertNull(viewModel.getImportedRecipeTitle());
    assertTrue(viewModel.getImportedIngredientNames().isEmpty());
  }

  // ===== I2: startImport(path) transitions to processing; status message updates =====
  @Test
  void testI2_startImportTransitionsToProcessing() throws Exception {
    when(ocrService.extractRecipe(any(Path.class)))
        .thenAnswer(
            invocation -> {
              Thread.sleep(1000);
              return new Recipe(
                  "recipe-1",
                  "Test Recipe",
                  new Servings(2),
                  List.of(new VagueIngredient("Flour", null, null, null)),
                  List.of(),
                  List.of());
            });

    viewModel.startImport(Path.of("test-image.png"));

    assertEquals("processing", viewModel.getState());
    assertEquals("processing", viewModel.currentStateProperty().get());
    assertEquals("Parsing recipe image...", viewModel.getStatusMessage());
    assertEquals("Parsing recipe image...", viewModel.statusMessageProperty().get());

    viewModel.cancelImport();
  }

  private void waitForState(String expectedState, long timeoutMillis) throws Exception {
    long deadline = System.currentTimeMillis() + timeoutMillis;

    while (System.currentTimeMillis() < deadline) {
      if (expectedState.equals(viewModel.getState())) {
        return;
      }
      Thread.sleep(50);
    }

    fail("Timed out waiting for state: " + expectedState);
  }

  // ===== I3: Successful OCR transitions to review; imported recipe is populated =====
  @Test
  void testI3_successfulOcrTransitionsToReview() throws Exception {
    Recipe importedRecipe =
        new Recipe(
            "recipe-1",
            "Chocolate Cake",
            new Servings(4),
            List.of(
                new VagueIngredient("Flour", null, null, null),
                new VagueIngredient("Sugar", null, null, null)),
            List.of(),
            List.of());

    when(ocrService.extractRecipe(any(Path.class))).thenReturn(importedRecipe);

    viewModel.startImport(Path.of("test-image.png"));

    waitForState("review", 5000);

    assertEquals("review", viewModel.getState());
    assertEquals("review", viewModel.currentStateProperty().get());
    assertEquals("Chocolate Cake", viewModel.getImportedRecipeTitle());
    assertEquals("Chocolate Cake", viewModel.importedTitleProperty().get());
    assertEquals(List.of("Flour", "Sugar"), viewModel.getImportedIngredientNames());
    assertEquals(2, viewModel.importedIngredientsProperty().size());
    assertEquals("Parsing complete. Review extracted recipe.", viewModel.getStatusMessage());
  }

  // ===== I4: OCR failure transitions to error; error message is populated =====
  @Test
  void testI4_ocrFailureTransitionsToError() throws Exception {
    when(ocrService.extractRecipe(any(Path.class)))
        .thenThrow(new RuntimeException("OCR failed badly"));

    viewModel.startImport(Path.of("bad-image.png"));

    waitForState("error", 5000);

    assertEquals("error", viewModel.getState());
    assertEquals("error", viewModel.currentStateProperty().get());
    assertEquals("OCR failed badly", viewModel.getErrorMessage());
    assertEquals("OCR failed badly", viewModel.errorMessageProperty().get());
    assertEquals("Import failed.", viewModel.getStatusMessage());
  }

  // ===== I5: cancelImport() during processing transitions back to idle =====
  @Test
  void testI5_cancelImportDuringProcessingReturnsToIdle() throws Exception {
    when(ocrService.extractRecipe(any(Path.class)))
        .thenAnswer(
            invocation -> {
              Thread.sleep(2000);
              return new Recipe(
                  "recipe-1",
                  "Late Recipe",
                  new Servings(2),
                  List.of(new VagueIngredient("Flour", null, null, null)),
                  List.of(),
                  List.of());
            });

    viewModel.startImport(Path.of("test-image.png"));

    assertEquals("processing", viewModel.getState());

    viewModel.cancelImport();

    assertEquals("idle", viewModel.getState());
    assertEquals("idle", viewModel.currentStateProperty().get());
    assertEquals("Import cancelled. You can start a new import.", viewModel.getStatusMessage());

    Thread.sleep(300);
    assertEquals("idle", viewModel.getState());
  }

  // ===== I6: acceptImport() saves the recipe to the selected collection and transitions to idle
  // =====
  @Test
  void testI6_acceptImportSavesRecipeAndTransitionsToIdle() throws Exception {
    // Setup
    RecipeCollection collection1 = mock(RecipeCollection.class);
    when(collection1.getId()).thenReturn("coll-1");
    when(collection1.getTitle()).thenReturn("My Recipes");

    when(librarianService.listCollections()).thenReturn(List.of(collection1));

    Ingredient flour = new VagueIngredient("Flour", null, null, null);
    Recipe mockRecipe =
        new Recipe(
            "recipe-1", "Chocolate Cake", new Servings(4), List.of(flour), List.of(), List.of());

    when(ocrService.extractRecipe(any(Path.class))).thenReturn(mockRecipe);

    viewModel.startImport(Path.of("test.png"));
    waitForState("review", 5000);

    assertEquals("review", viewModel.currentStateProperty().get());

    viewModel.selectTargetCollection("coll-1");
    viewModel.acceptImport();

    assertEquals("idle", viewModel.currentStateProperty().get());
    assertEquals("", viewModel.importedTitleProperty().get());
    assertTrue(viewModel.importedIngredientsProperty().isEmpty());
    assertEquals(0.0, viewModel.importProgressProperty().get());

    verify(librarianService, times(1)).saveRecipe(any(Recipe.class), eq("coll-1"));
  }

  // ===== I7: rejectImport() discards the imported recipe and transitions to idle =====
  @Test
  void testI7_rejectImportDiscardsRecipeAndTransitionsToIdle() throws Exception {
    // Setup
    Ingredient flour = new VagueIngredient("Flour", null, null, null);
    Recipe mockRecipe =
        new Recipe(
            "recipe-1", "Chocolate Cake", new Servings(4), List.of(flour), List.of(), List.of());

    when(ocrService.extractRecipe(any(Path.class))).thenReturn(mockRecipe);

    viewModel.startImport(Path.of("test.png"));
    waitForState("review", 5000);

    assertEquals("review", viewModel.currentStateProperty().get());
    assertEquals("Chocolate Cake", viewModel.importedTitleProperty().get());
    assertEquals(1, viewModel.importedIngredientsProperty().size());

    viewModel.rejectImport();

    assertEquals("idle", viewModel.currentStateProperty().get());
    assertEquals("", viewModel.importedTitleProperty().get());
    assertTrue(viewModel.importedIngredientsProperty().isEmpty());
  }

  // ===== I8: Available collections are loaded from the repository =====
  @Test
  void testI8_availableCollectionsAreLoadedFromRepository() {
    RecipeCollection coll1 = mock(RecipeCollection.class);
    RecipeCollection coll2 = mock(RecipeCollection.class);

    when(coll1.getId()).thenReturn("id-1");
    when(coll1.getTitle()).thenReturn("Collection 1");
    when(coll2.getId()).thenReturn("id-2");
    when(coll2.getTitle()).thenReturn("Collection 2");

    when(librarianService.listCollections()).thenReturn(List.of(coll1, coll2));

    viewModel.loadCollections();

    assertEquals(2, viewModel.availableCollectionsProperty().size());
    assertEquals("Collection 1", viewModel.availableCollectionsProperty().get(0).getTitle());
    assertEquals("Collection 2", viewModel.availableCollectionsProperty().get(1).getTitle());
  }

  // ===== I9: Pre-save editing: imported recipe title/ingredients can be modified before accept
  // =====
  @Test
  void testI9_preSaveEditingAllowsModifyingTitleAndIngredients() throws Exception {
    RecipeCollection collection1 = mock(RecipeCollection.class);
    when(collection1.getId()).thenReturn("coll-1");

    when(librarianService.listCollections()).thenReturn(List.of(collection1));

    Ingredient flour = new VagueIngredient("Flour", null, null, null);
    Ingredient sugar = new VagueIngredient("Sugar", null, null, null);
    Recipe mockRecipe =
        new Recipe(
            "recipe-1",
            "Chocolate Cake",
            new Servings(4),
            List.of(flour, sugar),
            List.of(),
            List.of());

    when(ocrService.extractRecipe(any(Path.class))).thenReturn(mockRecipe);

    viewModel.startImport(Path.of("test.png"));
    waitForState("review", 5000);

    assertEquals("review", viewModel.currentStateProperty().get());
    assertEquals("Chocolate Cake", viewModel.importedTitleProperty().get());
    assertEquals(2, viewModel.importedIngredientsProperty().size());

    viewModel.importedTitleProperty().set("Modified Cake");
    assertEquals("Modified Cake", viewModel.importedTitleProperty().get());

    Ingredient vanilla = new VagueIngredient("Vanilla", null, null, null);
    viewModel.importedIngredientsProperty().add(vanilla);
    assertEquals(3, viewModel.importedIngredientsProperty().size());

    viewModel.selectTargetCollection("coll-1");
    viewModel.acceptImport();

    verify(librarianService, times(1))
        .saveRecipe(
            argThat(
                recipe -> {
                  return recipe.getTitle().equals("Modified Cake")
                      && recipe.getIngredients().size() == 3;
                }),
            eq("coll-1"));
  }

  // ===== I10: Edge case: acceptImport() with no selected collection or no recipe is a no-op =====
  @Test
  void testI10_acceptImportWithoutSelectedCollectionIsNoOp() throws Exception {
    Ingredient flour = new VagueIngredient("Flour", null, null, null);
    Recipe mockRecipe =
        new Recipe(
            "recipe-1", "Chocolate Cake", new Servings(4), List.of(flour), List.of(), List.of());

    when(ocrService.extractRecipe(any(Path.class))).thenReturn(mockRecipe);

    viewModel.startImport(Path.of("test.png"));
    waitForState("review", 5000);

    assertEquals("review", viewModel.currentStateProperty().get());

    viewModel.acceptImport();

    assertEquals("review", viewModel.currentStateProperty().get());
    assertEquals(1, viewModel.importedIngredientsProperty().size());

    verify(librarianService, never()).saveRecipe(any(), anyString());
  }

  @Test
  void testI10_acceptImportWithoutRecipeIsNoOp() {
    assertEquals("idle", viewModel.currentStateProperty().get());
    assertTrue(viewModel.importedIngredientsProperty().isEmpty());

    viewModel.selectTargetCollection("coll-1");
    viewModel.acceptImport();

    assertEquals("idle", viewModel.currentStateProperty().get());
    verify(librarianService, never()).saveRecipe(any(), anyString());
  }
}
