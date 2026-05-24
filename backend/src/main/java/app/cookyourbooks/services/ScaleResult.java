package app.cookyourbooks.services;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;

/**
 * Result of scaling a recipe to a target number of servings.
 *
 * <p>Contains both the original and scaled recipes, plus the scale factor. Does not persist the
 * scaled recipe — the CLI decides whether to save.
 *
 * @param original the original unscaled recipe
 * @param scaled the recipe after scaling
 * @param factor the scale factor applied
 */
public record ScaleResult(@NonNull Recipe original, @NonNull Recipe scaled, double factor) {}
