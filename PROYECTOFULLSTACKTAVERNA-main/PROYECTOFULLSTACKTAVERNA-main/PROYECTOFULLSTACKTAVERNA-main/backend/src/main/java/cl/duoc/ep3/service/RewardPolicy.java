package cl.duoc.ep3.service;

import java.util.List;
import java.util.Locale;

public final class RewardPolicy {

    public static final String REGISTRATION_CODE = "WelcomeToLevelUp";
    public static final int REGISTRATION_BONUS = 20;
    public static final long PESOS_PER_POINT_BLOCK = 200_000L;
    public static final int POINTS_PER_SPEND_BLOCK = 20;

    private static final List<Integer> SHIRT_COSTS = List.of(60, 65, 70, 75, 80);
    private static final List<Integer> HOODIE_COSTS = List.of(80, 85, 90, 95, 100, 110, 120);

    private RewardPolicy() {
    }

    public static int pointsEarned(long finalAmount) {
        // Solo cuentan bloques completos de $200.000 del total pagado.
        return Math.toIntExact(Math.multiplyExact(
                finalAmount / PESOS_PER_POINT_BLOCK,
                POINTS_PER_SPEND_BLOCK));
    }

    public static Integer redemptionCost(String sourceId, String title, String sourceRoute, String category) {
        // El costo se deriva de la identidad del producto y se mantiene estable entre reinicios.
        String identity = ((sourceId == null || sourceId.isBlank() ? "" : sourceId) + (title == null ? "" : title))
                .toLowerCase(Locale.ROOT);
        String route = sourceRoute == null ? "" : sourceRoute.toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "" : category.toLowerCase(Locale.ROOT);
        boolean isHoodie = identity.contains("poleron")
                || route.contains("poleron")
                || normalizedCategory.contains("poleron");
        boolean isShirt = !isHoodie && (identity.contains("polera")
                || route.contains("polera")
                || normalizedCategory.contains("polera"));
        if (!isHoodie && !isShirt) {
            return null;
        }

        List<Integer> costs = isHoodie ? HOODIE_COSTS : SHIRT_COSTS;
        return costs.get((int) (stableHash(identity) % costs.size()));
    }

    private static long stableHash(String value) {
        int hash = 0;
        for (int index = 0; index < value.length(); index++) {
            hash = (hash << 5) - hash + value.charAt(index);
        }
        return Math.abs((long) hash);
    }
}
