package app.cookyourbooks.service;

import app.cookyourbooks.domain.Ingredient;
import app.cookyourbooks.domain.Recipe;
import app.cookyourbooks.domain.RecipeIngredient;
import app.cookyourbooks.domain.ShoppingList;
import app.cookyourbooks.domain.ShoppingListItem;
import app.cookyourbooks.domain.Unit;
import app.cookyourbooks.domain.User;
import app.cookyourbooks.dto.shopping.GenerateFromRecipesRequest;
import app.cookyourbooks.dto.shopping.ShoppingListItemRequest;
import app.cookyourbooks.dto.shopping.ShoppingListItemResponse;
import app.cookyourbooks.dto.shopping.ShoppingListRequest;
import app.cookyourbooks.dto.shopping.ShoppingListResponse;
import app.cookyourbooks.exception.BadRequestException;
import app.cookyourbooks.exception.NotFoundException;
import app.cookyourbooks.repository.IngredientRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.repository.ShoppingListItemRepository;
import app.cookyourbooks.repository.ShoppingListRepository;
import app.cookyourbooks.repository.UnitRepository;
import app.cookyourbooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ShoppingListResponse> list(Long userId) {
        return shoppingListRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(ShoppingListResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse get(Long userId, Long listId) {
        return ShoppingListResponse.from(loadOwned(userId, listId));
    }

    @Transactional
    public ShoppingListResponse create(Long userId, ShoppingListRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        ShoppingList list = ShoppingList.builder()
            .user(user)
            .name(request.name())
            .build();
        return ShoppingListResponse.from(shoppingListRepository.save(list));
    }

    @Transactional
    public ShoppingListResponse rename(Long userId, Long listId, ShoppingListRequest request) {
        ShoppingList list = loadOwned(userId, listId);
        list.setName(request.name());
        return ShoppingListResponse.from(shoppingListRepository.save(list));
    }

    @Transactional
    public void delete(Long userId, Long listId) {
        shoppingListRepository.delete(loadOwned(userId, listId));
    }

    @Transactional
    public ShoppingListItemResponse addItem(Long userId, Long listId,
                                            ShoppingListItemRequest request) {
        ShoppingList list = loadOwned(userId, listId);
        ShoppingListItem item = buildItem(list, request, nextPosition(list));
        list.getItems().add(item);
        shoppingListRepository.save(list);
        return ShoppingListItemResponse.from(item);
    }

    @Transactional
    public ShoppingListItemResponse updateItem(Long userId, Long listId, Long itemId,
                                               ShoppingListItemRequest request) {
        ShoppingList list = loadOwned(userId, listId);
        ShoppingListItem item = list.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Shopping list item not found: " + itemId));
        item.setRawText(request.rawText());
        item.setQuantity(request.quantity());
        item.setUnit(resolveUnit(request.unitId()));
        item.setIngredient(resolveIngredient(request.ingredientId()));
        item.setRecipe(resolveRecipe(userId, request.recipeId()));
        if (request.checked() != null) {
            item.setChecked(request.checked());
        }
        return ShoppingListItemResponse.from(shoppingListItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long userId, Long listId, Long itemId) {
        ShoppingList list = loadOwned(userId, listId);
        boolean removed = list.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new NotFoundException("Shopping list item not found: " + itemId);
        }
        shoppingListRepository.save(list);
    }

    @Transactional
    public ShoppingListResponse generateFromRecipes(Long userId, Long listId,
                                                    GenerateFromRecipesRequest request) {
        ShoppingList list = loadOwned(userId, listId);
        int position = nextPosition(list);
        for (Long recipeId : request.recipeIds()) {
            Recipe recipe = recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
            for (RecipeIngredient ri : recipe.getIngredients()) {
                ShoppingListItem item = ShoppingListItem.builder()
                    .shoppingList(list)
                    .recipe(recipe)
                    .ingredient(ri.getIngredient())
                    .rawText(ri.getRawText())
                    .quantity(ri.getQuantity())
                    .unit(ri.getUnit())
                    .checked(false)
                    .position(position++)
                    .build();
                list.getItems().add(item);
            }
        }
        return ShoppingListResponse.from(shoppingListRepository.save(list));
    }

    private ShoppingList loadOwned(Long userId, Long listId) {
        return shoppingListRepository.findByIdAndUserId(listId, userId)
            .orElseThrow(() -> new NotFoundException("Shopping list not found: " + listId));
    }

    private ShoppingListItem buildItem(ShoppingList list, ShoppingListItemRequest request,
                                       int position) {
        return ShoppingListItem.builder()
            .shoppingList(list)
            .rawText(request.rawText())
            .quantity(request.quantity())
            .unit(resolveUnit(request.unitId()))
            .ingredient(resolveIngredient(request.ingredientId()))
            .recipe(resolveRecipe(list.getUser().getId(), request.recipeId()))
            .checked(Boolean.TRUE.equals(request.checked()))
            .position(position)
            .build();
    }

    private Unit resolveUnit(Long unitId) {
        if (unitId == null) {
            return null;
        }
        return unitRepository.findById(unitId)
            .orElseThrow(() -> new BadRequestException("Unknown unit: " + unitId));
    }

    private Ingredient resolveIngredient(Long ingredientId) {
        if (ingredientId == null) {
            return null;
        }
        return ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new BadRequestException("Unknown ingredient: " + ingredientId));
    }

    private Recipe resolveRecipe(Long userId, Long recipeId) {
        if (recipeId == null) {
            return null;
        }
        return recipeRepository.findByIdAndUserId(recipeId, userId)
            .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
    }

    private int nextPosition(ShoppingList list) {
        return list.getItems().stream()
            .mapToInt(ShoppingListItem::getPosition)
            .max()
            .orElse(-1) + 1;
    }
}
