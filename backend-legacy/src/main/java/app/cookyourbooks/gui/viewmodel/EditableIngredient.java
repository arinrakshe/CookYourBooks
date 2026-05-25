package app.cookyourbooks.gui.viewmodel;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.services.ParseException;
import app.cookyourbooks.services.parsing.IngredientParser;

/**
 * A mutable, UI-friendly wrapper around an ingredient for use in the Recipe Editor.
 *
 * <p>Domain {@link Ingredient} objects are immutable and reject blank names — making them
 * unsuitable for binding to live text fields. This class holds the ingredient's editable state as
 * JavaFX properties and converts back to a domain {@link Ingredient} via {@link #toIngredient()}
 * only at save time.
 *
 * <p>The {@code amount} field holds the numeric part of the quantity (e.g., "2", "1/2", "1-2") and
 * {@code unit} holds the unit abbreviation (e.g., "cups", "g"). When {@code vague} is true, amount
 * and unit are ignored and the ingredient is treated as a vague ingredient.
 */
public class EditableIngredient {

  private final StringProperty name = new SimpleStringProperty("");
  private final StringProperty amount = new SimpleStringProperty("");
  private final StringProperty unit = new SimpleStringProperty("");
  private final BooleanProperty vague = new SimpleBooleanProperty(false);

  /** Creates a blank ingredient for use when the user clicks "Add Ingredient". */
  public EditableIngredient() {}

  /**
   * Creates an editable ingredient pre-populated from an existing domain ingredient.
   *
   * @param ingredient the domain ingredient to copy state from
   */
  public EditableIngredient(Ingredient ingredient) {
    name.set(ingredient.getName());
    if (ingredient instanceof MeasuredIngredient mi) {
      Quantity q = mi.getQuantity();
      String unitAbbr = q.getUnit().getAbbreviation();
      String pluralAbbr = q.getUnit().getPluralAbbreviation();
      String qStr = q.toString();
      // Strip the unit abbreviation from the end of the quantity string to get just the amount
      if (!unitAbbr.equals(pluralAbbr) && qStr.endsWith(" " + pluralAbbr)) {
        amount.set(qStr.substring(0, qStr.length() - pluralAbbr.length() - 1).trim());
      } else if (qStr.endsWith(" " + unitAbbr)) {
        amount.set(qStr.substring(0, qStr.length() - unitAbbr.length() - 1).trim());
      } else {
        amount.set(qStr);
      }
      unit.set(unitAbbr);
    } else {
      vague.set(true);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Properties
  // ──────────────────────────────────────────────────────────────────────────

  public StringProperty nameProperty() {
    return name;
  }

  public StringProperty amountProperty() {
    return amount;
  }

  public StringProperty unitProperty() {
    return unit;
  }

  public BooleanProperty vagueProperty() {
    return vague;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Plain accessors
  // ──────────────────────────────────────────────────────────────────────────

  public String getName() {
    return name.get();
  }

  public String getAmount() {
    return amount.get();
  }

  public String getUnit() {
    return unit.get();
  }

  public boolean isVague() {
    return vague.get();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Conversion
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Attempts to convert this editable ingredient into a domain {@link Ingredient}.
   *
   * <p>Returns empty if the name is blank — blank rows are silently skipped at save time. Assembles
   * a parseable string from the amount, unit, and name fields and delegates to {@link
   * IngredientParser}. Parse failures also return empty.
   *
   * @return the parsed ingredient, or empty if the name is blank or parsing fails
   */
  public Optional<Ingredient> toIngredient() {
    String nameVal = name.get().trim();
    if (nameVal.isBlank()) {
      return Optional.empty();
    }

    String raw;
    if (vague.get() || amount.get().isBlank()) {
      raw = nameVal;
    } else if (unit.get().isBlank()) {
      raw = amount.get().trim() + " " + nameVal;
    } else {
      raw = amount.get().trim() + " " + unit.get().trim() + " " + nameVal;
    }

    try {
      return Optional.of(new IngredientParser().parse(raw));
    } catch (ParseException e) {
      return Optional.empty();
    }
  }
}
