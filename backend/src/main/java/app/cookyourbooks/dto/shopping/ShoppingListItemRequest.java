package app.cookyourbooks.dto.shopping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShoppingListItemRequest(
        @NotBlank @Size(max = 512) String rawText,
        BigDecimal quantity,
        Long unitId,
        Long ingredientId,
        Long recipeId,
        Boolean checked) {
}
