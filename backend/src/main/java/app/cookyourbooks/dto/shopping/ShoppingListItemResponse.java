package app.cookyourbooks.dto.shopping;

import app.cookyourbooks.domain.ShoppingListItem;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ShoppingListItemResponse(
        Long id,
        String rawText,
        BigDecimal quantity,
        Long unitId,
        String unitCode,
        Long ingredientId,
        String ingredientName,
        Long recipeId,
        boolean checked,
        Integer position) {

    public static ShoppingListItemResponse from(ShoppingListItem item) {
        return ShoppingListItemResponse.builder()
            .id(item.getId())
            .rawText(item.getRawText())
            .quantity(item.getQuantity())
            .unitId(item.getUnit() != null ? item.getUnit().getId() : null)
            .unitCode(item.getUnit() != null ? item.getUnit().getCode() : null)
            .ingredientId(item.getIngredient() != null ? item.getIngredient().getId() : null)
            .ingredientName(item.getIngredient() != null ? item.getIngredient().getName() : null)
            .recipeId(item.getRecipe() != null ? item.getRecipe().getId() : null)
            .checked(item.isChecked())
            .position(item.getPosition())
            .build();
    }
}
