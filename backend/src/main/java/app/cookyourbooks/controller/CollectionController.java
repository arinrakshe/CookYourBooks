package app.cookyourbooks.controller;

import app.cookyourbooks.dto.collection.CollectionRecipeResponse;
import app.cookyourbooks.dto.collection.CollectionRequest;
import app.cookyourbooks.dto.collection.CollectionResponse;
import app.cookyourbooks.security.SecurityUtils;
import app.cookyourbooks.service.CollectionService;
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
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public List<CollectionResponse> list() {
        return collectionService.list(SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}")
    public CollectionResponse get(@PathVariable Long id) {
        return collectionService.get(SecurityUtils.currentUserId(), id);
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> create(@Valid @RequestBody CollectionRequest request) {
        CollectionResponse created = collectionService.create(
            SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public CollectionResponse update(@PathVariable Long id,
                                     @Valid @RequestBody CollectionRequest request) {
        return collectionService.update(SecurityUtils.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        collectionService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recipes")
    public List<CollectionRecipeResponse> listRecipes(@PathVariable Long id) {
        return collectionService.listRecipes(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/{id}/recipes/{recipeId}")
    public ResponseEntity<CollectionRecipeResponse> addRecipe(
            @PathVariable Long id,
            @PathVariable Long recipeId) {
        CollectionRecipeResponse added = collectionService.addRecipe(
            SecurityUtils.currentUserId(), id, recipeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    @DeleteMapping("/{id}/recipes/{recipeId}")
    public ResponseEntity<Void> removeRecipe(
            @PathVariable Long id,
            @PathVariable Long recipeId) {
        collectionService.removeRecipe(SecurityUtils.currentUserId(), id, recipeId);
        return ResponseEntity.noContent().build();
    }
}
