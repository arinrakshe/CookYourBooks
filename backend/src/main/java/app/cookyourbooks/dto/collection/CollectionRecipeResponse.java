package app.cookyourbooks.dto.collection;

import app.cookyourbooks.domain.RecipeCollection;
import lombok.Builder;

@Builder
public record CollectionRecipeResponse(
        Long recipeId,
        String title,
        String imageUrl,
        Integer position) {

    public static CollectionRecipeResponse from(RecipeCollection rc) {
        return CollectionRecipeResponse.builder()
            .recipeId(rc.getRecipe().getId())
            .title(rc.getRecipe().getTitle())
            .imageUrl(rc.getRecipe().getImageUrl())
            .position(rc.getPosition())
            .build();
    }
}
