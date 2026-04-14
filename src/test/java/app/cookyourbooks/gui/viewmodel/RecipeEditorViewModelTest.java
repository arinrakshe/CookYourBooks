package app.cookyourbooks.gui.viewmodel;

import static app.cookyourbooks.cli.fixtures.TestRecipeBuilder.recipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.repository.RepositoryException;

/**
 * Unit tests for {@link RecipeEditorViewModelImpl}.
 *
 * <p>Uses a mocked {@link RecipeRepository} so tests are isolated from file I/O. Extends {@link
 * ViewModelTestBase} to initialize the JavaFX toolkit (required for JavaFX properties to work in
 * tests). Async save tests use {@link #waitForFxEvents()} to let background threads complete and
 * deliver their FX-thread callbacks before asserting.
 */
class RecipeEditorViewModelTest extends ViewModelTestBase {

  private RecipeRepository mockRepo;
  private RecipeEditorViewModelImpl vm;

  // A reusable test recipe with two measured ingredients
  private Recipe testRecipe;

  /**
   * Waits deterministically for an async save to complete. Polls {@code isSaving()} up to 2 seconds
   * rather than sleeping a fixed duration, so the test passes as soon as the background thread
   * finishes and is not flaky on slow CI machines.
   */
  private void waitForSave() throws InterruptedException {
    long deadline = System.currentTimeMillis() + 2_000;
    while (vm.isSaving() && System.currentTimeMillis() < deadline) {
      waitForFxEvents(); // drain the FX queue so the success/failure callback can run
      Thread.sleep(10);
    }
    waitForFxEvents(); // one final drain to ensure the callback has fully executed
  }

  @BeforeEach
  void setUp() {
    mockRepo = mock(RecipeRepository.class);
    vm = new RecipeEditorViewModelImpl(mockRepo);
    testRecipe =
        recipe("Pasta Carbonara")
            .withIngredient("flour", 2.0, Unit.CUP)
            .withIngredient("salt", 1.0, Unit.TEASPOON)
            .withStep("Mix ingredients")
            .build();
    when(mockRepo.findById(testRecipe.getId())).thenReturn(Optional.of(testRecipe));
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E1 — loadRecipe populates title and ingredient list
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void loadRecipe_populatesTitle() {
    vm.loadRecipe(testRecipe.getId());

    assertThat(vm.getTitle()).isEqualTo("Pasta Carbonara");
  }

  @Test
  void loadRecipe_populatesIngredientList() {
    vm.loadRecipe(testRecipe.getId());

    assertThat(vm.getIngredientCount()).isEqualTo(2);
    assertThat(vm.getIngredientNames()).containsExactly("flour", "salt");
  }

  @Test
  void loadRecipe_unknownId_doesNothing() {
    when(mockRepo.findById("unknown")).thenReturn(Optional.empty());

    vm.loadRecipe("unknown");

    // ViewModel should stay in its initial empty state
    assertThat(vm.getRecipeId()).isNull();
    assertThat(vm.getTitle()).isEmpty();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E2 — toggleEditMode switches between view and edit mode
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void toggleEditMode_startsInViewMode() {
    assertThat(vm.isEditing()).isFalse();
  }

  @Test
  void toggleEditMode_enablesEditMode() {
    vm.toggleEditMode();

    assertThat(vm.isEditing()).isTrue();
  }

  @Test
  void toggleEditMode_togglesBackToViewMode() {
    vm.toggleEditMode();
    vm.toggleEditMode();

    assertThat(vm.isEditing()).isFalse();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E3 + E4 — dirty listeners and discardChanges
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void changingTitleInEditMode_setsDirty() {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode(); // enter edit mode

    vm.titleProperty().set("New Title");

    assertThat(vm.isDirty()).isTrue();
  }

  @Test
  void changingTitleInViewMode_doesNotSetDirty() {
    // Changes outside edit mode should never mark dirty
    vm.loadRecipe(testRecipe.getId());

    vm.titleProperty().set("New Title");

    assertThat(vm.isDirty()).isFalse();
  }

  @Test
  void discardChanges_revertsTitleAndClearsDirty() {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    vm.titleProperty().set("Wrong Title");
    assertThat(vm.isDirty()).isTrue(); // confirm dirty before discard

    vm.discardChanges();

    assertThat(vm.getTitle()).isEqualTo("Pasta Carbonara");
    assertThat(vm.isDirty()).isFalse();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E3 + E10 — context switch (loadRecipe) does NOT set dirty
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void loadRecipe_doesNotSetDirty() {
    // Loading a recipe programmatically must NOT trigger the dirty listener
    vm.loadRecipe(testRecipe.getId());

    assertThat(vm.isDirty()).isFalse();
  }

  @Test
  void loadRecipe_whileInEditMode_doesNotSetDirty() {
    // Even if already editing, loading a new recipe should reset dirty to false
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();

    Recipe anotherRecipe =
        recipe("Chicken Soup").withIngredient("chicken", 1.0, Unit.POUND).build();
    when(mockRepo.findById(anotherRecipe.getId())).thenReturn(Optional.of(anotherRecipe));
    vm.loadRecipe(anotherRecipe.getId());

    assertThat(vm.isDirty()).isFalse();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E5 — isValid false when title blank
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void isValid_falseWhenTitleBlank() {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();

    vm.titleProperty().set("");

    assertThat(vm.isValid()).isFalse();
  }

  @Test
  void isValid_trueWhenTitleNonBlank() {
    vm.loadRecipe(testRecipe.getId());

    assertThat(vm.isValid()).isTrue();
  }

  @Test
  void isValid_falseForWhitespaceOnlyTitle() {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();

    vm.titleProperty().set("   ");

    assertThat(vm.isValid()).isFalse();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E6 — addIngredient / removeIngredient
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void addIngredient_appendsBlankRow() {
    vm.loadRecipe(testRecipe.getId());
    int before = vm.getIngredientCount();

    vm.addIngredient();

    assertThat(vm.getIngredientCount()).isEqualTo(before + 1);
    // The new row has a blank name
    assertThat(vm.getIngredientNames().get(before)).isEmpty();
  }

  @Test
  void removeIngredient_removesAtIndex() {
    vm.loadRecipe(testRecipe.getId());

    vm.removeIngredient(0); // remove "flour"

    assertThat(vm.getIngredientCount()).isEqualTo(1);
    assertThat(vm.getIngredientNames()).containsExactly("salt");
  }

  @Test
  void removeIngredient_outOfRange_doesNotThrow() {
    vm.loadRecipe(testRecipe.getId());

    // Should silently do nothing
    vm.removeIngredient(99);

    assertThat(vm.getIngredientCount()).isEqualTo(2);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E7 + E8 — save persists to repository on background thread
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void save_persistsUpdatedRecipeToRepository() throws InterruptedException {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    vm.titleProperty().set("Updated Pasta");

    vm.save();
    waitForSave();

    // Capture the saved Recipe and assert key fields — ensures the ViewModel isn't saving stale
    // data
    ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
    verify(mockRepo).save(captor.capture());
    Recipe saved = captor.getValue();
    assertThat(saved.getTitle()).isEqualTo("Updated Pasta");
    assertThat(saved.getId()).isEqualTo(testRecipe.getId()); // ID must be preserved
  }

  @Test
  void save_clearsEditModeAndDirtyOnSuccess() throws InterruptedException {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    vm.titleProperty().set("Updated Pasta");

    vm.save();
    waitForSave();

    assertThat(vm.isDirty()).isFalse();
    assertThat(vm.isEditing()).isFalse();
    assertThat(vm.getStatusMessage()).isEqualTo("Saved successfully.");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E9 — save failure preserves dirty state and shows error
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void save_failure_preservesDirtyState() throws InterruptedException {
    doThrow(new RepositoryException("Disk full")).when(mockRepo).save(any());
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    vm.titleProperty().set("Updated Pasta");

    vm.save();
    waitForSave();

    // Dirty must be preserved so the user doesn't lose their edits
    assertThat(vm.isDirty()).isTrue();
  }

  @Test
  void save_failure_showsErrorMessageAndStaysInEditMode() throws InterruptedException {
    doThrow(new RepositoryException("Disk full")).when(mockRepo).save(any());
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    vm.titleProperty().set("Updated Pasta");

    vm.save();
    waitForSave();

    assertThat(vm.isEditing()).isTrue();
    assertThat(vm.getStatusMessage()).contains("Save failed");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // E10 — save is a no-op when not dirty or not valid
  // ──────────────────────────────────────────────────────────────────────────

  @Test
  void save_noopWhenNotDirty() throws InterruptedException {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    // Do NOT change anything — isDirty stays false

    vm.save();
    waitForFxEvents();

    verify(mockRepo, never()).save(any());
  }

  @Test
  void save_noopWhenTitleBlank() throws InterruptedException {
    vm.loadRecipe(testRecipe.getId());
    vm.toggleEditMode();
    vm.titleProperty().set(""); // makes isDirty true but isValid false

    vm.save();
    waitForFxEvents();

    verify(mockRepo, never()).save(any());
  }
}
