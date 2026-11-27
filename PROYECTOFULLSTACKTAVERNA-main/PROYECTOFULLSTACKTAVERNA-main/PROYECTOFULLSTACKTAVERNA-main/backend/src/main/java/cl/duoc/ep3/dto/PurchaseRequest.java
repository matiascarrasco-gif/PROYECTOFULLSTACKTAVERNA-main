package cl.duoc.ep3.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PurchaseRequest(
        @NotNull UUID orderId,
        @NotEmpty @Size(max = 100) List<@Valid PurchaseItemRequest> items
) {
}
