package app.cookyourbooks.dto.recipe;

import app.cookyourbooks.domain.Recipe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record RecipeResponse(
        Long id,
        String title,
        String description,
        BigDecimal servings,
        String sourceUrl,
        String imageUrl,
        String notes,
        List<String> steps,
        List<RecipeIngredientResponse> ingredients,
        Instant createdAt,
        Instant updatedAt) {

    public static RecipeResponse from(Recipe recipe, ObjectMapper objectMapper) {
        return RecipeResponse.builder()
            .id(recipe.getId())
            .title(recipe.getTitle())
            .description(recipe.getDescription())
            .servings(recipe.getServings())
            .sourceUrl(recipe.getSourceUrl())
            .imageUrl(recipe.getImageUrl())
            .notes(recipe.getNotes())
            .steps(parseSteps(recipe.getSteps(), objectMapper))
            .ingredients(recipe.getIngredients().stream()
                .map(RecipeIngredientResponse::from)
                .toList())
            .createdAt(recipe.getCreatedAt())
            .updatedAt(recipe.getUpdatedAt())
            .build();
    }

    private static List<String> parseSteps(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
