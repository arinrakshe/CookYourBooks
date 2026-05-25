package app.cookyourbooks.controller;

import app.cookyourbooks.dto.ingredient.IngredientResponse;
import app.cookyourbooks.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public Page<IngredientResponse> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        return ingredientService.search(query, page, size);
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable Long id) {
        return ingredientService.get(id);
    }
}
