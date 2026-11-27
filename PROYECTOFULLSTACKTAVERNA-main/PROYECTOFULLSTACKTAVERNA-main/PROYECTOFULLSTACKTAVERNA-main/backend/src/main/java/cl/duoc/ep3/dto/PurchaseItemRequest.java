package cl.duoc.ep3.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PurchaseItemRequest(
        @NotNull Long productId,
        @Min(1) @Max(50) int quantity,
        UUID redemptionId
) {
}
