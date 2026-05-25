package app.cookyourbooks.dto.shopping;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenerateFromRecipesRequest(
        @NotEmpty List<Long> recipeIds) {
}
