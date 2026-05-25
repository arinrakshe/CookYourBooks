package app.cookyourbooks.service;

import app.cookyourbooks.domain.Collection;
import app.cookyourbooks.domain.Recipe;
import app.cookyourbooks.domain.RecipeCollection;
import app.cookyourbooks.domain.User;
import app.cookyourbooks.dto.collection.CollectionRecipeResponse;
import app.cookyourbooks.dto.collection.CollectionRequest;
import app.cookyourbooks.dto.collection.CollectionResponse;
import app.cookyourbooks.exception.ConflictException;
import app.cookyourbooks.exception.NotFoundException;
import app.cookyourbooks.repository.CollectionRepository;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final RecipeCollectionRepository recipeCollectionRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CollectionResponse> list(Long userId) {
        return collectionRepository.findByUserId(userId).stream()
            .map(CollectionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CollectionResponse get(Long userId, Long collectionId) {
        return CollectionResponse.from(loadOwned(userId, collectionId));
    }

    @Transactional
    public CollectionResponse create(Long userId, CollectionRequest request) {
        if (collectionRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new ConflictException("Collection name already in use");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Collection collection = Collection.builder()
            .user(user)
            .name(request.name())
            .description(request.description())
            .build();
        return CollectionResponse.from(collectionRepository.save(collection));
    }

    @Transactional
    public CollectionResponse update(Long userId, Long collectionId, CollectionRequest request) {
        Collection collection = loadOwned(userId, collectionId);
        if (!collection.getName().equalsIgnoreCase(request.name())
                && collectionRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new ConflictException("Collection name already in use");
        }
        collection.setName(request.name());
        collection.setDescription(request.description());
        return CollectionResponse.from(collectionRepository.save(collection));
    }

    @Transactional
    public void delete(Long userId, Long collectionId) {
        Collection collection = loadOwned(userId, collectionId);
        collectionRepository.delete(collection);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecipeResponse> listRecipes(Long userId, Long collectionId) {
        loadOwned(userId, collectionId);
        return recipeCollectionRepository.findByCollectionIdOrderByPositionAsc(collectionId).stream()
            .map(CollectionRecipeResponse::from)
            .toList();
    }

    @Transactional
    public CollectionRecipeResponse addRecipe(Long userId, Long collectionId, Long recipeId) {
        Collection collection = loadOwned(userId, collectionId);
        Recipe recipe = recipeRepository.findByIdAndUserId(recipeId, userId)
            .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
        RecipeCollection.Key key = new RecipeCollection.Key(recipe.getId(), collection.getId());
        if (recipeCollectionRepository.existsById(key)) {
            throw new ConflictException("Recipe already in collection");
        }
        int nextPosition = recipeCollectionRepository
            .findTopByCollectionIdOrderByPositionDesc(collection.getId())
            .map(rc -> rc.getPosition() + 1)
            .orElse(0);
        RecipeCollection link = RecipeCollection.builder()
            .id(key)
            .recipe(recipe)
            .collection(collection)
            .position(nextPosition)
            .build();
        return CollectionRecipeResponse.from(recipeCollectionRepository.save(link));
    }

    @Transactional
    public void removeRecipe(Long userId, Long collectionId, Long recipeId) {
        loadOwned(userId, collectionId);
        RecipeCollection.Key key = new RecipeCollection.Key(recipeId, collectionId);
        if (!recipeCollectionRepository.existsById(key)) {
            throw new NotFoundException("Recipe not in collection");
        }
        recipeCollectionRepository.deleteById(key);
    }

    private Collection loadOwned(Long userId, Long collectionId) {
        return collectionRepository.findByIdAndUserId(collectionId, userId)
            .orElseThrow(() -> new NotFoundException("Collection not found: " + collectionId));
    }
}
