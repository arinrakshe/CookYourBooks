package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

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
}
