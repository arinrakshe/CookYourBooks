package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.CookbookImpl;
import app.cookyourbooks.model.PersonalCollectionImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.model.UnitSystem;
import app.cookyourbooks.services.LibrarianService;

@ExtendWith(MockitoExtension.class)
class LibraryViewModelImplTest extends ViewModelTestBase {

  private LibrarianService librarian;
  private NavigationService navigation;
  private LibraryViewModelImpl vm;

  @BeforeEach
  void setUp() {
    librarian = mock(LibrarianService.class);
    navigation = new NavigationService();
    vm = new LibraryViewModelImpl(librarian, navigation, Duration.ofMillis(60));
  }

  /** Waits for {@link BackgroundTaskRunner} to finish and FX callbacks to run. */
  private void waitForAsyncRefresh() throws InterruptedException {
    waitForFxEvents();
    Thread.sleep(200);
    waitForFxEvents();
  }

  @Test
  void L1_refresh_loadsCollectionsIntoList() throws Exception {
    RecipeCollection c =
        PersonalCollectionImpl.builder().id("c1").title("Desserts").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(c));

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();

    assertThat(vm.getCollectionIds()).containsExactly("c1");
  }

  @Test
  void L2_collectionEntriesExposeIdTitleSourceTypeAndCount() throws Exception {
    Recipe cake = new Recipe("r1", "Cake", null, List.of(), List.of(), List.of());
    RecipeCollection c =
        CookbookImpl.builder().id("cb1").title("Joy of Cooking").recipes(List.of(cake)).build();
    when(librarian.listCollections()).thenReturn(List.of(c));

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();

    @SuppressWarnings("unchecked")
    ObservableList<LibraryCollectionRow> rows =
        (ObservableList<LibraryCollectionRow>) vm.collectionsProperty();
    assertThat(rows).hasSize(1);
    LibraryCollectionRow row = rows.get(0);
    assertThat(row.id()).isEqualTo("cb1");
    assertThat(row.title()).isEqualTo("Joy of Cooking");
    assertThat(row.sourceType()).isEqualTo(SourceType.PUBLISHED_BOOK);
    assertThat(row.recipeCount()).isEqualTo(1);
  }

  @Test
  void L3_selectCollection_updatesSelectionAndRecipeList() throws Exception {
    Recipe r = new Recipe("rx", "Soup", null, List.of(), List.of(), List.of());
    RecipeCollection c =
        PersonalCollectionImpl.builder().id("c1").title("Main").recipes(List.of(r)).build();
    when(librarian.listCollections()).thenReturn(List.of(c));
    when(librarian.findCollectionById("c1")).thenReturn(Optional.of(c));

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();
    Platform.runLater(() -> vm.selectCollection("c1"));
    waitForFxEvents();

    assertThat(vm.getSelectedCollectionId()).isEqualTo("c1");
    assertThat(vm.getRecipeIds()).containsExactly("rx");
  }

  @Test
  void L4_createCollection_callsServiceAndRefresh() throws Exception {
    RecipeCollection created =
        PersonalCollectionImpl.builder().id("new1").title("New").recipes(List.of()).build();
    when(librarian.createCollection("New")).thenReturn(created);
    when(librarian.listCollections()).thenReturn(List.of(created));

    Platform.runLater(() -> vm.createCollection("New"));
    waitForAsyncRefresh();

    verify(librarian).createCollection("New");
    verify(librarian, timeout(5000).times(1)).listCollections();
  }

  @Test
  void L5_afterUndoTimeout_deleteHitsRepository() throws Exception {
    RecipeCollection c =
        PersonalCollectionImpl.builder().id("d1").title("Gone").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(c));

    LibraryViewModelImpl shortUndo =
        new LibraryViewModelImpl(librarian, navigation, Duration.ofMillis(50));
    Platform.runLater(() -> shortUndo.refresh());
    waitForAsyncRefresh();
    clearInvocations(librarian);

    Platform.runLater(() -> shortUndo.deleteCollection("d1"));
    waitForFxEvents();
    Thread.sleep(200);
    waitForFxEvents();

    verify(librarian).deleteCollection("d1");
  }

  @Test
  void L6_undoDelete_beforeTimeout_restoresWithoutRepositoryDelete() throws Exception {
    RecipeCollection c =
        PersonalCollectionImpl.builder().id("d1").title("Stay").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(c));

    LibraryViewModelImpl shortUndo =
        new LibraryViewModelImpl(librarian, navigation, Duration.ofMillis(200));
    Platform.runLater(() -> shortUndo.refresh());
    waitForAsyncRefresh();
    clearInvocations(librarian);

    Platform.runLater(() -> shortUndo.deleteCollection("d1"));
    waitForFxEvents();
    Platform.runLater(() -> shortUndo.undoDelete());
    waitForFxEvents();
    Thread.sleep(300);
    waitForFxEvents();

    verify(librarian, never()).deleteCollection(any());
  }

  @Test
  void L7_undoAvailabilityClearsAfterTimeout() throws Exception {
    RecipeCollection c =
        PersonalCollectionImpl.builder().id("d1").title("X").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(c));

    LibraryViewModelImpl shortUndo =
        new LibraryViewModelImpl(librarian, navigation, Duration.ofMillis(50));
    Platform.runLater(() -> shortUndo.refresh());
    waitForAsyncRefresh();

    Platform.runLater(() -> shortUndo.deleteCollection("d1"));
    waitForFxEvents();
    AtomicBoolean undoAvailMid = new AtomicBoolean(true);
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          undoAvailMid.set(shortUndo.isUndoAvailable());
          latch.countDown();
        });
    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertThat(undoAvailMid.get()).isTrue();

    Thread.sleep(200);
    waitForFxEvents();

    AtomicBoolean undoAfter = new AtomicBoolean(true);
    CountDownLatch latch2 = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          undoAfter.set(shortUndo.isUndoAvailable());
          latch2.countDown();
        });
    assertTrue(latch2.await(2, TimeUnit.SECONDS));
    assertThat(undoAfter.get()).isFalse();
  }

  @Test
  void L8_refresh_loadingTrueWhileBackgroundWorkRuns() throws Exception {
    CountDownLatch inCallable = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(librarian.listCollections())
        .thenAnswer(
            inv -> {
              inCallable.countDown();
              assertTrue(release.await(5, TimeUnit.SECONDS));
              return List.of();
            });

    Platform.runLater(() -> vm.refresh());
    assertTrue(inCallable.await(5, TimeUnit.SECONDS));

    AtomicBoolean loadingDuring = new AtomicBoolean(false);
    CountDownLatch read = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          loadingDuring.set(vm.isLoading());
          read.countDown();
        });
    assertTrue(read.await(2, TimeUnit.SECONDS));
    assertThat(loadingDuring.get()).isTrue();

    release.countDown();
    waitForFxEvents();

    AtomicBoolean loadingAfter = new AtomicBoolean(true);
    CountDownLatch read2 = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          loadingAfter.set(vm.isLoading());
          read2.countDown();
        });
    assertTrue(read2.await(2, TimeUnit.SECONDS));
    assertThat(loadingAfter.get()).isFalse();
  }

  @Test
  void L9_selectRecipe_navigatesWithRecipeId() throws Exception {
    Recipe r = new Recipe("rid-9", "Toast", null, List.of(), List.of(), List.of());
    RecipeCollection c =
        PersonalCollectionImpl.builder().id("c9").title("Brunch").recipes(List.of(r)).build();
    when(librarian.listCollections()).thenReturn(List.of(c));
    when(librarian.findCollectionById("c9")).thenReturn(Optional.of(c));

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();
    Platform.runLater(() -> vm.selectCollection("c9"));
    waitForFxEvents();
    Platform.runLater(() -> vm.selectRecipe("rid-9"));
    waitForFxEvents();

    assertThat(navigation.getSelectedRecipeId()).isEqualTo("rid-9");
    assertThat(navigation.getCurrentView()).isEqualTo(NavigationService.View.RECIPE_EDITOR);
  }

  @Test
  void L10_selectMissingCollectionId_clearsGracefully() throws Exception {
    when(librarian.listCollections()).thenReturn(List.of());
    when(librarian.findCollectionById("nope")).thenReturn(Optional.empty());

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();
    Platform.runLater(() -> vm.selectCollection("nope"));
    waitForFxEvents();

    assertThat(vm.getSelectedCollectionId()).isNull();
    assertThat(vm.getRecipeIds()).isEmpty();
  }

  @Test
  void L11_filterTextIsCaseInsensitiveSubstring() throws Exception {
    RecipeCollection apple =
        PersonalCollectionImpl.builder().id("a1").title("Apple Pie").recipes(List.of()).build();
    RecipeCollection banana =
        PersonalCollectionImpl.builder().id("b1").title("Banana Bread").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(apple, banana));

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();
    Platform.runLater(() -> vm.filterTextProperty().set("BANANA"));
    waitForFxEvents();

    assertThat(vm.getCollectionIds()).containsExactly("b1");
  }

  @Test
  void L12_filteredListUpdatesImmediatelyWhenFilterChanges() throws Exception {
    RecipeCollection a =
        PersonalCollectionImpl.builder().id("a1").title("A").recipes(List.of()).build();
    RecipeCollection b =
        PersonalCollectionImpl.builder().id("b1").title("B").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(a, b));

    Platform.runLater(() -> vm.refresh());
    waitForAsyncRefresh();
    Platform.runLater(() -> vm.filterTextProperty().set("a"));
    waitForFxEvents();
    assertThat(vm.getCollectionIds()).containsExactly("a1");

    Platform.runLater(() -> vm.filterTextProperty().set(""));
    waitForFxEvents();
    assertThat(vm.getCollectionIds()).containsExactly("a1", "b1");
  }

  @Test
  void L13_undoWithFilter_restoredOnlyIfMatchesFilter() throws Exception {
    RecipeCollection hello =
        PersonalCollectionImpl.builder().id("h1").title("Hello").recipes(List.of()).build();
    RecipeCollection world =
        PersonalCollectionImpl.builder().id("w1").title("World").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(hello, world));

    LibraryViewModelImpl shortUndo =
        new LibraryViewModelImpl(librarian, navigation, Duration.ofMillis(80));
    Platform.runLater(() -> shortUndo.refresh());
    waitForAsyncRefresh();

    Platform.runLater(() -> shortUndo.filterTextProperty().set("zzz"));
    waitForFxEvents();
    assertThat(shortUndo.getCollectionIds()).isEmpty();

    Platform.runLater(() -> shortUndo.deleteCollection("h1"));
    waitForFxEvents();
    Platform.runLater(() -> shortUndo.undoDelete());
    waitForFxEvents();

    assertThat(shortUndo.getCollectionIds()).isEmpty();

    Platform.runLater(() -> shortUndo.filterTextProperty().set(""));
    waitForFxEvents();
    assertThat(shortUndo.getCollectionIds()).containsExactlyInAnyOrder("h1", "w1");
  }

  @Test
  void newPendingDelete_commitsPreviousPendingToRepository() throws Exception {
    RecipeCollection c1 =
        PersonalCollectionImpl.builder().id("x1").title("First").recipes(List.of()).build();
    RecipeCollection c2 =
        PersonalCollectionImpl.builder().id("x2").title("Second").recipes(List.of()).build();
    when(librarian.listCollections()).thenReturn(List.of(c1, c2));

    LibraryViewModelImpl shortUndo =
        new LibraryViewModelImpl(librarian, navigation, Duration.ofMillis(500));
    Platform.runLater(() -> shortUndo.refresh());
    waitForAsyncRefresh();
    clearInvocations(librarian);

    Platform.runLater(() -> shortUndo.deleteCollection("x1"));
    waitForFxEvents();
    Platform.runLater(() -> shortUndo.deleteCollection("x2"));
    waitForFxEvents();

    verify(librarian).deleteCollection(eq("x1"));
    verify(librarian, times(1)).deleteCollection("x1");
  }

  @Test
  void globalUnitMode_propagatesFromNavigationService() {
    assertThat(vm.getUnitSystem()).isEqualTo(UnitSystem.IMPERIAL);
    navigation.setUnitSystem(UnitSystem.METRIC);
    assertThat(vm.getUnitSystem()).isEqualTo(UnitSystem.METRIC);
  }
}
