package app.cookyourbooks.controller;

import app.cookyourbooks.dto.shopping.GenerateFromRecipesRequest;
import app.cookyourbooks.dto.shopping.ShoppingListItemRequest;
import app.cookyourbooks.dto.shopping.ShoppingListItemResponse;
import app.cookyourbooks.dto.shopping.ShoppingListRequest;
import app.cookyourbooks.dto.shopping.ShoppingListResponse;
import app.cookyourbooks.security.SecurityUtils;
import app.cookyourbooks.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    @GetMapping
    public List<ShoppingListResponse> list() {
        return shoppingListService.list(SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}")
    public ShoppingListResponse get(@PathVariable Long id) {
        return shoppingListService.get(SecurityUtils.currentUserId(), id);
    }

    @PostMapping
    public ResponseEntity<ShoppingListResponse> create(
            @Valid @RequestBody ShoppingListRequest request) {
        ShoppingListResponse created = shoppingListService.create(
            SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ShoppingListResponse rename(@PathVariable Long id,
                                       @Valid @RequestBody ShoppingListRequest request) {
        return shoppingListService.rename(SecurityUtils.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shoppingListService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ShoppingListItemResponse> addItem(
            @PathVariable Long id,
            @Valid @RequestBody ShoppingListItemRequest request) {
        ShoppingListItemResponse item = shoppingListService.addItem(
            SecurityUtils.currentUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{id}/items/{itemId}")
    public ShoppingListItemResponse updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody ShoppingListItemRequest request) {
        return shoppingListService.updateItem(
            SecurityUtils.currentUserId(), id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        shoppingListService.deleteItem(SecurityUtils.currentUserId(), id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate-from-recipes")
    public ShoppingListResponse generateFromRecipes(
            @PathVariable Long id,
            @Valid @RequestBody GenerateFromRecipesRequest request) {
        return shoppingListService.generateFromRecipes(
            SecurityUtils.currentUserId(), id, request);
    }
}
