package app.cookyourbooks.repository;

import app.cookyourbooks.domain.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);
    Page<Ingredient> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);
}
