package app.cookyourbooks.dto.recipe;

import app.cookyourbooks.domain.RecipeIngredient;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RecipeIngredientResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        String rawText,
        BigDecimal quantity,
        Long unitId,
        String unitCode,
        String preparation,
        String notes,
        Integer position) {

    public static RecipeIngredientResponse from(RecipeIngredient ri) {
        return RecipeIngredientResponse.builder()
            .id(ri.getId())
            .ingredientId(ri.getIngredient() != null ? ri.getIngredient().getId() : null)
            .ingredientName(ri.getIngredient() != null ? ri.getIngredient().getName() : null)
            .rawText(ri.getRawText())
            .quantity(ri.getQuantity())
            .unitId(ri.getUnit() != null ? ri.getUnit().getId() : null)
            .unitCode(ri.getUnit() != null ? ri.getUnit().getCode() : null)
            .preparation(ri.getPreparation())
            .notes(ri.getNotes())
            .position(ri.getPosition())
            .build();
    }
}
