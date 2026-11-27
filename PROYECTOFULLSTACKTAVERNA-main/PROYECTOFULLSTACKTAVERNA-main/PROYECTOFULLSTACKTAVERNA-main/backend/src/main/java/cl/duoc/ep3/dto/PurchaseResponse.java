package cl.duoc.ep3.dto;

import java.util.UUID;

public record PurchaseResponse(
        UUID orderId,
        long subtotal,
        long discount,
        long total,
        int pointsEarned,
        int puntosSaldo,
        AccountResponse user
) {
}
