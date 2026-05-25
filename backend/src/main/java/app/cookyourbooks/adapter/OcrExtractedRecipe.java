package app.cookyourbooks.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrExtractedRecipe(
        String title,
        String description,
        BigDecimal servings,
        List<String> steps,
        List<OcrExtractedIngredient> ingredients,
        String notes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OcrExtractedIngredient(
            String rawText,
            String name,
            BigDecimal quantity,
            String unit,
            String preparation,
            String notes) {
    }
}
