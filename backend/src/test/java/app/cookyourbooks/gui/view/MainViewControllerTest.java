package app.cookyourbooks.gui.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.UnitSystem;

class MainViewControllerTest extends ViewModelTestBase {

  private NavigationService navigationService;
  private MainViewController controller;

  @BeforeEach
  void setUp() throws Exception {
    navigationService = new NavigationService();
    controller = new MainViewController(navigationService);
    runOnFxThreadAndWait(
        () -> {
          try {
            setField("contentArea", new StackPane());
            setField("libraryButton", new Button("Library"));
            setField("editorButton", new Button("Recipe Editor"));
            setField("importButton", new Button("Import"));
            setField("searchButton", new Button("Search"));
            setField("unitSystemComboBox", new ComboBox<UnitSystem>());
            Method initialize = MainViewController.class.getDeclaredMethod("initialize");
            initialize.setAccessible(true);
            initialize.invoke(controller);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void unitCombo_initializesWithExpectedOptionsAndValue() throws Exception {
    ComboBox<UnitSystem> combo = getUnitCombo();
    assertThat(combo.getItems()).containsExactly(UnitSystem.IMPERIAL, UnitSystem.METRIC);
    assertThat(combo.getValue()).isEqualTo(UnitSystem.IMPERIAL);
  }

  @Test
  void unitCombo_selectionUpdatesNavigationService() throws Exception {
    ComboBox<UnitSystem> combo = getUnitCombo();
    runOnFxThreadAndWait(() -> combo.setValue(UnitSystem.METRIC));
    assertThat(navigationService.getUnitSystem()).isEqualTo(UnitSystem.METRIC);
  }

  @Test
  void externalNavigationUnitChangeUpdatesComboSelection() throws Exception {
    ComboBox<UnitSystem> combo = getUnitCombo();
    runOnFxThreadAndWait(() -> navigationService.setUnitSystem(UnitSystem.METRIC));
    assertThat(combo.getValue()).isEqualTo(UnitSystem.METRIC);
  }

  private void setField(String fieldName, Object value) throws Exception {
    Field field = MainViewController.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(controller, value);
  }

  @SuppressWarnings("unchecked")
  private ComboBox<UnitSystem> getUnitCombo() throws Exception {
    Field field = MainViewController.class.getDeclaredField("unitSystemComboBox");
    field.setAccessible(true);
    return (ComboBox<UnitSystem>) field.get(controller);
  }

  private static void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          try {
            action.run();
          } finally {
            latch.countDown();
          }
        });
    latch.await();
  }
}
