package app.cookyourbooks.gui;

import java.nio.file.Path;
import java.util.List;

import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.ocr.RecipeOcrService;

public class LocalFakeRecipeOcrService implements RecipeOcrService {

  @Override
  public Recipe extractRecipe(Path imagePath) {
    return new Recipe(
        "Imported Test Recipe",
        new Servings(2),
        List.of(
            new VagueIngredient("flour", null, null, null),
            new VagueIngredient("sugar", null, null, null),
            new VagueIngredient("eggs", null, null, null)),
        List.of(new Instruction(1, "prep", List.of())),
        List.of());
  }
}
