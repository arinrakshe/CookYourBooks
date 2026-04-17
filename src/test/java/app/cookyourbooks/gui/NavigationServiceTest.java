package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.cookyourbooks.model.UnitSystem;

class NavigationServiceTest {

  @Test
  void unitSystem_defaultsToImperial() {
    NavigationService navigationService = new NavigationService();
    assertThat(navigationService.getUnitSystem()).isEqualTo(UnitSystem.IMPERIAL);
    assertThat(navigationService.unitSystemProperty().get()).isEqualTo(UnitSystem.IMPERIAL);
  }

  @Test
  void unitSystem_setterUpdatesProperty() {
    NavigationService navigationService = new NavigationService();
    navigationService.setUnitSystem(UnitSystem.METRIC);
    assertThat(navigationService.getUnitSystem()).isEqualTo(UnitSystem.METRIC);
    assertThat(navigationService.unitSystemProperty().get()).isEqualTo(UnitSystem.METRIC);
  }

  // ── Dark mode tests ──

  @Test
  void darkMode_defaultsToFalse() {
    NavigationService navigationService = new NavigationService();
    assertThat(navigationService.isDarkMode()).isFalse();
    assertThat(navigationService.darkModeProperty().get()).isFalse();
  }

  @Test
  void darkMode_setToTrue_updatesGetterAndProperty() {
    NavigationService navigationService = new NavigationService();
    navigationService.setDarkMode(true);
    assertThat(navigationService.isDarkMode()).isTrue();
    assertThat(navigationService.darkModeProperty().get()).isTrue();
  }

  @Test
  void darkMode_toggleTwice_returnsToFalse() {
    NavigationService navigationService = new NavigationService();
    navigationService.setDarkMode(true);
    navigationService.setDarkMode(false);
    assertThat(navigationService.isDarkMode()).isFalse();
    assertThat(navigationService.darkModeProperty().get()).isFalse();
  }

  @Test
  void darkMode_propertyChange_notifiesListeners() {
    NavigationService navigationService = new NavigationService();
    List<Boolean> observed = new ArrayList<>();
    navigationService.darkModeProperty().addListener((obs, oldVal, newVal) -> observed.add(newVal));

    navigationService.setDarkMode(true);
    navigationService.setDarkMode(false);

    assertThat(observed).containsExactly(true, false);
  }

  @Test
  void darkMode_toggle_doesNotAffectOtherProperties() {
    NavigationService navigationService = new NavigationService();
    navigationService.setDarkMode(true);
    assertThat(navigationService.getUnitSystem()).isEqualTo(UnitSystem.IMPERIAL);
    assertThat(navigationService.getCurrentView()).isEqualTo(NavigationService.View.LIBRARY);
  }
}
