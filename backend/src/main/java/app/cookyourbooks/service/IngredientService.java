package app.cookyourbooks.service;

import app.cookyourbooks.domain.Ingredient;
import app.cookyourbooks.dto.ingredient.IngredientResponse;
import app.cookyourbooks.exception.NotFoundException;
import app.cookyourbooks.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

    private static final int MAX_PAGE_SIZE = 100;

    private final IngredientRepository ingredientRepository;

    public Page<IngredientResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        String q = query == null ? "" : query.trim();
        Page<Ingredient> result = q.isEmpty()
            ? ingredientRepository.findAll(pageable)
            : ingredientRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q, pageable);
        return result.map(IngredientResponse::from);
    }

    public IngredientResponse get(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Ingredient not found: " + id));
        return IngredientResponse.from(ingredient);
    }
}
