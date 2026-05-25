package app.cookyourbooks.repository;

import app.cookyourbooks.domain.RecipeCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeCollectionRepository extends JpaRepository<RecipeCollection, RecipeCollection.Key> {
    List<RecipeCollection> findByCollectionIdOrderByPositionAsc(Long collectionId);
    Optional<RecipeCollection> findTopByCollectionIdOrderByPositionDesc(Long collectionId);
}
