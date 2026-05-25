package app.cookyourbooks.dto.ingredient;

import app.cookyourbooks.domain.Ingredient;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record IngredientResponse(
        Long id,
        String name,
        Long defaultUnitId,
        String defaultUnitCode,
        BigDecimal densityGPerMl,
        Long usdaFdcId) {

    public static IngredientResponse from(Ingredient ingredient) {
        return IngredientResponse.builder()
            .id(ingredient.getId())
            .name(ingredient.getName())
            .defaultUnitId(ingredient.getDefaultUnit() != null
                ? ingredient.getDefaultUnit().getId() : null)
            .defaultUnitCode(ingredient.getDefaultUnit() != null
                ? ingredient.getDefaultUnit().getCode() : null)
            .densityGPerMl(ingredient.getDensityGPerMl())
            .usdaFdcId(ingredient.getUsdaFdcId())
            .build();
    }
}
