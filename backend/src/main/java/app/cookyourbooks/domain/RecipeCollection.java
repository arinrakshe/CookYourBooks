package app.cookyourbooks.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "recipe_collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeCollection {

    @EmbeddedId
    private Key id;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @MapsId("collectionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @Column(nullable = false)
    private Integer position;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        @Column(name = "recipe_id")
        private Long recipeId;

        @Column(name = "collection_id")
        private Long collectionId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(recipeId, key.recipeId) && Objects.equals(collectionId, key.collectionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recipeId, collectionId);
        }
    }
}
