package cl.duoc.ep3.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RewardPolicyTest {

    @Test
    void preservesLegacyHashCostForTheMinimumIntegerHash() {
        assertEquals(75, RewardPolicy.redemptionCost("polygenelubricants", "", "", "polera"));
    }

    @Test
    void awardsPointsForEachCompleteTwoHundredThousandPesos() {
        assertEquals(0, RewardPolicy.pointsEarned(199_999));
        assertEquals(20, RewardPolicy.pointsEarned(200_000));
        assertEquals(20, RewardPolicy.pointsEarned(399_999));
        assertEquals(40, RewardPolicy.pointsEarned(400_000));
    }
}
