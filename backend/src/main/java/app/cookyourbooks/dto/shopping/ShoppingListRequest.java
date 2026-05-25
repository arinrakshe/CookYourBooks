package app.cookyourbooks.dto.shopping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShoppingListRequest(
        @NotBlank @Size(max = 120) String name) {
}
