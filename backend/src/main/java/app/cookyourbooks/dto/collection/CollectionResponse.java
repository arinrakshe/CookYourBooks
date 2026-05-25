package app.cookyourbooks.dto.collection;

import app.cookyourbooks.domain.Collection;
import lombok.Builder;

import java.time.Instant;

@Builder
public record CollectionResponse(
        Long id,
        String name,
        String description,
        Instant createdAt) {

    public static CollectionResponse from(Collection collection) {
        return CollectionResponse.builder()
            .id(collection.getId())
            .name(collection.getName())
            .description(collection.getDescription())
            .createdAt(collection.getCreatedAt())
            .build();
    }
}
