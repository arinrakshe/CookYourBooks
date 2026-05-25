package app.cookyourbooks.dto.shopping;

import app.cookyourbooks.domain.ShoppingList;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ShoppingListResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<ShoppingListItemResponse> items) {

    public static ShoppingListResponse from(ShoppingList list) {
        return ShoppingListResponse.builder()
            .id(list.getId())
            .name(list.getName())
            .createdAt(list.getCreatedAt())
            .updatedAt(list.getUpdatedAt())
            .items(list.getItems().stream()
                .map(ShoppingListItemResponse::from)
                .toList())
            .build();
    }
}
