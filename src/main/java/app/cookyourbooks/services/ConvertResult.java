package app.cookyourbooks.services;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;

/**
 * Result of converting a recipe to a target unit.
 *
 * <p>Contains both the original and converted recipes. Does not persist the converted recipe — the
 * CLI decides whether to save.
 *
 * @param original the original recipe before unit conversion
 * @param converted the recipe after unit conversion
 */
public record ConvertResult(@NonNull Recipe original, @NonNull Recipe converted) {}
