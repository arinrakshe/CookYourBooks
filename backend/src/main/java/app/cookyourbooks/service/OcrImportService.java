package app.cookyourbooks.service;

import app.cookyourbooks.adapter.GeminiOcrAdapter;
import app.cookyourbooks.adapter.OcrExtractedRecipe;
import app.cookyourbooks.domain.Ingredient;
import app.cookyourbooks.domain.Recipe;
import app.cookyourbooks.domain.RecipeIngredient;
import app.cookyourbooks.domain.Unit;
import app.cookyourbooks.domain.User;
import app.cookyourbooks.dto.ocr.OcrImportResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OcrImportService {

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;

    private final GeminiOcrAdapter geminiOcrAdapter;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OcrImportResponse importFromImage(Long userId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image must be 8 MiB or smaller");
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded image");
        }
        OcrExtractedRecipe extracted = geminiOcrAdapter.extract(bytes, image.getContentType());
        if (extracted.title() == null || extracted.title().isBlank()) {
            throw new BadRequestException("OCR could not detect a recipe title");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Set<String> unmatchedIngredients = new LinkedHashSet<>();
        Set<String> unmatchedUnits = new LinkedHashSet<>();

        Recipe recipe = Recipe.builder()
            .user(user)
            .title(extracted.title().trim())
            .description(extracted.description())
            .servings(extracted.servings())
            .notes(extracted.notes())
            .steps(serializeSteps(extracted.steps()))
            .build();

        List<OcrExtractedRecipe.OcrExtractedIngredient> items = extracted.ingredients() == null
            ? List.of() : extracted.ingredients();
        int position = 0;
        for (OcrExtractedRecipe.OcrExtractedIngredient item : items) {
            String rawText = item.rawText() == null || item.rawText().isBlank()
                ? buildRawText(item) : item.rawText();
            if (rawText.isBlank()) {
                continue;
            }
            Ingredient ingredient = null;
            if (item.name() != null && !item.name().isBlank()) {
                ingredient = ingredientRepository.findByNameIgnoreCase(item.name().trim())
                    .orElse(null);
                if (ingredient == null) {
                    unmatchedIngredients.add(item.name());
                }
            }
            Unit unit = null;
            if (item.unit() != null && !item.unit().isBlank()) {
                unit = unitRepository.findByCode(item.unit().trim()).orElse(null);
                if (unit == null) {
                    unmatchedUnits.add(item.unit());
                }
            }
            recipe.getIngredients().add(RecipeIngredient.builder()
                .recipe(recipe)
                .ingredient(ingredient)
                .rawText(rawText)
                .quantity(item.quantity())
                .unit(unit)
                .preparation(item.preparation())
                .notes(item.notes())
                .position(position++)
                .build());
        }

        Recipe saved = recipeRepository.save(recipe);
        return OcrImportResponse.builder()
            .recipe(RecipeResponse.from(saved, objectMapper))
            .unmatchedIngredients(new ArrayList<>(unmatchedIngredients))
            .unmatchedUnits(new ArrayList<>(unmatchedUnits))
            .build();
    }

    private String serializeSteps(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String buildRawText(OcrExtractedRecipe.OcrExtractedIngredient item) {
        StringBuilder sb = new StringBuilder();
        if (item.quantity() != null) {
            sb.append(item.quantity().stripTrailingZeros().toPlainString()).append(' ');
        }
        if (item.unit() != null && !item.unit().isBlank()) {
            sb.append(item.unit()).append(' ');
        }
        if (item.name() != null) {
            sb.append(item.name());
        }
        return sb.toString().trim();
    }
}
