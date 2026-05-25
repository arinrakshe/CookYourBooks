package app.cookyourbooks.dto.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RecipeRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        BigDecimal servings,
        @Size(max = 1024) String sourceUrl,
        @Size(max = 1024) String imageUrl,
        String notes,
        List<String> steps,
        @Valid List<RecipeIngredientRequest> ingredients) {
}
