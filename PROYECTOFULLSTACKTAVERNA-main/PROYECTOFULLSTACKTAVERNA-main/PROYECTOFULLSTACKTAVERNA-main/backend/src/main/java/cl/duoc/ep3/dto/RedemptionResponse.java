package cl.duoc.ep3.dto;

import java.util.UUID;

public record RedemptionResponse(
        UUID redemptionId,
        Long productId,
        int pointsCost,
        int puntosSaldo,
        AccountResponse user
) {
}
