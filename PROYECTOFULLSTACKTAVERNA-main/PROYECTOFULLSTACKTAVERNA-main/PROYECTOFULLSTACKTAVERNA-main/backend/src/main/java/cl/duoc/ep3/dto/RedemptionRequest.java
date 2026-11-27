package cl.duoc.ep3.dto;

import jakarta.validation.constraints.NotNull;

public record RedemptionRequest(@NotNull Long productId) {
}
