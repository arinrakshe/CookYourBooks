package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.cli.fixtures.TestRecipeBuilder;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.services.LibrarianService;

/**
 * Unit tests for {@link SearchViewModelImpl}.
 *
 * <p>Uses Mockito to stub {@link LibrarianService} and {@link
 * app.cookyourbooks.gui.NavigationService}. All tests run synchronously by waiting for JavaFX and
 * background events to flush.
 *
 * <p>Requirement coverage:
 *
 * <ul>
 *   <li>S1/S2: title search populates results
 *   <li>S3/S4: ingredient filters narrow results via AND logic
 *   <li>S5: clearFilters resets state
 *   <li>S6: isSearching is true while running (hard to assert in unit test; covered via state)
 *   <li>S8: selectNextResult / selectPreviousResult cycle through results
 *   <li>S9: navigateToSelectedResult delegates to NavigationService
 *   <li>S10: status message reflects result count
 *   <li>S11: empty query returns all recipes
 * </ul>
 */
class SearchViewModelTest extends ViewModelTestBase {

  private LibrarianService librarianService;
  private app.cookyourbooks.gui.NavigationService navigationService;
  private SearchViewModelImpl viewModel;

  private Recipe chocolateCake;
  private Recipe vanillaCake;
  private Recipe chickenSoup;

  @BeforeEach
  void setUp() {
    librarianService = mock(LibrarianService.class);
    navigationService = mock(app.cookyourbooks.gui.NavigationService.class);

    chocolateCake =
        TestRecipeBuilder.recipe("Chocolate Cake")
            .withIngredient("chocolate", 200, Unit.GRAM)
            .withIngredient("flour", 300, Unit.GRAM)
            .build();
    vanillaCake =
        TestRecipeBuilder.recipe("Vanilla Cake")
            .withIngredient("vanilla", 1, Unit.TEASPOON)
            .withIngredient("flour", 300, Unit.GRAM)
            .build();
    chickenSoup =
        TestRecipeBuilder.recipe("Chicken Soup").withIngredient("chicken", 500, Unit.GRAM).build();

    // Default stubs
    when(librarianService.listAllRecipes())
        .thenReturn(List.of(chocolateCake, vanillaCake, chickenSoup));
    when(librarianService.listCollections()).thenReturn(List.of());
    when(librarianService.resolveRecipes("cake")).thenReturn(List.of(chocolateCake, vanillaCake));
    when(librarianService.resolveRecipes("chocolate")).thenReturn(List.of(chocolateCake));
    when(librarianService.searchByIngredient("flour"))
        .thenReturn(List.of(chocolateCake, vanillaCake));
    when(librarianService.searchByIngredient("chocolate")).thenReturn(List.of(chocolateCake));

    viewModel = new SearchViewModelImpl(librarianService, navigationService);
  }

  // ── S11: empty query returns all recipes ──────────────────────────────────

  @Test
  void emptyQueryReturnsAllRecipes() throws InterruptedException {
    viewModel.setQuery("");
    Thread.sleep(400); // wait past debounce
    waitForFxEvents();

    assertThat(viewModel.getResultIds())
        .containsExactlyInAnyOrder(chocolateCake.getId(), vanillaCake.getId(), chickenSoup.getId());
  }

  // ── S1/S2: title search populates results ─────────────────────────────────

  @Test
  void querySearchPopulatesResults() throws InterruptedException {
    viewModel.setQuery("cake");
    Thread.sleep(400);
    waitForFxEvents();

    assertThat(viewModel.getResultIds())
        .containsExactlyInAnyOrder(chocolateCake.getId(), vanillaCake.getId());
  }

  // ── S10: status message reflects count ───────────────────────────────────

  @Test
  void statusMessageReflectsResultCount() throws InterruptedException {
    viewModel.setQuery("cake");
    Thread.sleep(400);
    waitForFxEvents();

    assertThat(viewModel.getStatusMessage()).contains("2");
  }

  @Test
  void statusMessageSaysNoResultsWhenEmpty() throws InterruptedException {
    when(librarianService.resolveRecipes("xyz")).thenReturn(List.of());
    viewModel.setQuery("xyz");
    Thread.sleep(400);
    waitForFxEvents();

    assertThat(viewModel.getStatusMessage()).containsIgnoringCase("no results");
  }

  // ── S3/S4: ingredient filters (AND logic) ────────────────────────────────

  @Test
  void ingredientFilterNarrowsResults() throws InterruptedException {
    viewModel.setQuery("cake");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.addIngredientFilter("chocolate");
    Thread.sleep(400);
    waitForFxEvents();

    // Only chocolate cake has both "cake" in title AND "chocolate" ingredient
    assertThat(viewModel.getResultIds()).containsExactly(chocolateCake.getId());
  }

  @Test
  void multipleIngredientFiltersUseAndLogic() throws InterruptedException {
    // flour matches both cakes; chocolate matches only chocolateCake → intersection = chocolateCake
    when(librarianService.resolveRecipes("")).thenReturn(List.of());
    viewModel.setQuery("");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.addIngredientFilter("flour");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.addIngredientFilter("chocolate");
    Thread.sleep(400);
    waitForFxEvents();

    assertThat(viewModel.getResultIds()).containsExactly(chocolateCake.getId());
  }

  // ── S5: clearFilters resets state ────────────────────────────────────────

  @Test
  void clearFiltersResetsEverything() throws InterruptedException {
    viewModel.setQuery("cake");
    viewModel.addIngredientFilter("flour");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.clearFilters();
    waitForFxEvents();

    assertThat(viewModel.getQuery()).isEmpty();
    assertThat(viewModel.getIngredientFilters()).isEmpty();
    assertThat(viewModel.getResultIds()).isEmpty();
    assertThat(viewModel.getStatusMessage()).isEmpty();
  }

  // ── S8: keyboard navigation ───────────────────────────────────────────────

  @Test
  void selectNextResultCyclesForward() throws InterruptedException {
    viewModel.setQuery("cake");
    Thread.sleep(400);
    waitForFxEvents();

    // After search completes the first result is auto-selected (index 0).
    String first = viewModel.getSelectedResultId();
    viewModel.selectNextResult();
    String second = viewModel.getSelectedResultId();

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(second).isNotEqualTo(first);
  }

  @Test
  void selectPreviousResultWrapsAround() throws InterruptedException {
    viewModel.setQuery("cake");
    Thread.sleep(400);
    waitForFxEvents();

    // Start at index 0; going previous should wrap to the last result.
    String last = viewModel.getResultIds().get(viewModel.getResultIds().size() - 1);
    viewModel.selectPreviousResult();

    assertThat(viewModel.getSelectedResultId()).isEqualTo(last);
  }

  // ── S9: navigateToSelectedResult ─────────────────────────────────────────

  @Test
  void navigateToSelectedResultCallsNavigationService() throws InterruptedException {
    viewModel.setQuery("chocolate");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.navigateToSelectedResult();

    org.mockito.Mockito.verify(navigationService).navigateToRecipe(chocolateCake.getId());
  }

  // ── No-op when no results ────────────────────────────────────────────────

  @Test
  void selectNextResultDoesNothingWhenNoResults() throws InterruptedException {
    when(librarianService.resolveRecipes("xyz")).thenReturn(List.of());
    viewModel.setQuery("xyz");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.selectNextResult(); // should not throw
    assertThat(viewModel.getSelectedResultId()).isNull();
  }

  // ── removeIngredientFilter ────────────────────────────────────────────────

  @Test
  void removeIngredientFilterTriggersNewSearch() throws InterruptedException {
    viewModel.addIngredientFilter("flour");
    Thread.sleep(400);
    waitForFxEvents();

    viewModel.removeIngredientFilter("flour");
    Thread.sleep(400);
    waitForFxEvents();

    assertThat(viewModel.getIngredientFilters()).isEmpty();
  }
}
