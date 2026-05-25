package app.cookyourbooks.dto.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
        Long ingredientId,
        @NotBlank @Size(max = 512) String rawText,
        BigDecimal quantity,
        Long unitId,
        @Size(max = 255) String preparation,
        @Size(max = 512) String notes) {
}
