package app.cookyourbooks.service;

import app.cookyourbooks.domain.Ingredient;
import app.cookyourbooks.domain.Recipe;
import app.cookyourbooks.domain.RecipeIngredient;
import app.cookyourbooks.domain.Unit;
import app.cookyourbooks.domain.User;
import app.cookyourbooks.dto.recipe.RecipeIngredientRequest;
import app.cookyourbooks.dto.recipe.RecipeRequest;
import app.cookyourbooks.dto.recipe.RecipeResponse;
import app.cookyourbooks.exception.BadRequestException;
import app.cookyourbooks.exception.NotFoundException;
import app.cookyourbooks.repository.IngredientRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.repository.UnitRepository;
import app.cookyourbooks.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<RecipeResponse> listForUser(Long userId) {
        return recipeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(r -> RecipeResponse.from(r, objectMapper))
            .toList();
    }

    @Transactional(readOnly = true)
    public RecipeResponse get(Long userId, Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndUserId(recipeId, userId)
            .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
        return RecipeResponse.from(recipe, objectMapper);
    }

    @Transactional
    public RecipeResponse create(Long userId, RecipeRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Recipe recipe = Recipe.builder()
            .user(user)
            .title(request.title())
            .description(request.description())
            .servings(request.servings())
            .sourceUrl(request.sourceUrl())
            .imageUrl(request.imageUrl())
            .notes(request.notes())
            .steps(serializeSteps(request.steps()))
            .build();
        applyIngredients(recipe, request.ingredients());
        Recipe saved = recipeRepository.save(recipe);
        return RecipeResponse.from(saved, objectMapper);
    }

    @Transactional
    public RecipeResponse update(Long userId, Long recipeId, RecipeRequest request) {
        Recipe recipe = recipeRepository.findByIdAndUserId(recipeId, userId)
            .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
        recipe.setTitle(request.title());
        recipe.setDescription(request.description());
        recipe.setServings(request.servings());
        recipe.setSourceUrl(request.sourceUrl());
        recipe.setImageUrl(request.imageUrl());
        recipe.setNotes(request.notes());
        recipe.setSteps(serializeSteps(request.steps()));
        recipe.getIngredients().clear();
        applyIngredients(recipe, request.ingredients());
        Recipe saved = recipeRepository.save(recipe);
        return RecipeResponse.from(saved, objectMapper);
    }

    @Transactional
    public void delete(Long userId, Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndUserId(recipeId, userId)
            .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
        recipeRepository.delete(recipe);
    }

    private void applyIngredients(Recipe recipe, List<RecipeIngredientRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int position = 0;
        for (RecipeIngredientRequest item : items) {
            Ingredient ingredient = item.ingredientId() == null ? null
                : ingredientRepository.findById(item.ingredientId())
                    .orElseThrow(() -> new BadRequestException(
                        "Unknown ingredient: " + item.ingredientId()));
            Unit unit = item.unitId() == null ? null
                : unitRepository.findById(item.unitId())
                    .orElseThrow(() -> new BadRequestException(
                        "Unknown unit: " + item.unitId()));
            recipe.getIngredients().add(RecipeIngredient.builder()
                .recipe(recipe)
                .ingredient(ingredient)
                .rawText(item.rawText())
                .quantity(item.quantity())
                .unit(unit)
                .preparation(item.preparation())
                .notes(item.notes())
                .position(position++)
                .build());
        }
    }

    private String serializeSteps(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid steps payload");
        }
    }
}
