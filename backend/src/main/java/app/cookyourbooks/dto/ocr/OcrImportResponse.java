package app.cookyourbooks.dto.ocr;

import app.cookyourbooks.dto.recipe.RecipeResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record OcrImportResponse(
        RecipeResponse recipe,
        List<String> unmatchedIngredients,
        List<String> unmatchedUnits) {
}
