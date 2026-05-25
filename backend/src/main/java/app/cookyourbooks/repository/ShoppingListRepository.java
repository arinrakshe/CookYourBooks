package app.cookyourbooks.repository;

import app.cookyourbooks.domain.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    List<ShoppingList> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ShoppingList> findByIdAndUserId(Long id, Long userId);
}
