package com.cloudoptimizer.pso;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PsoFitnessTest {

    private static class ExposedPsoScheduler extends PsoScheduler {
        ExposedPsoScheduler() {
            super(5, 2);
        }

        double score(double[] position, List<Double> vmCapacities) {
            return evaluate(position, vmCapacities);
        }
    }

    @Test
    void balancedMappingShouldScoreHigherThanOverloadedMapping() {
        ExposedPsoScheduler scheduler = new ExposedPsoScheduler();
        List<Double> capacities = List.of(4.0, 4.0, 4.0);

        double[] balanced = {0.05, 0.35, 0.72, 0.12, 0.41, 0.84};
        double[] overloaded = {0.01, 0.02, 0.03, 0.04, 0.05, 0.06};

        double balancedScore = scheduler.score(balanced, capacities);
        double overloadedScore = scheduler.score(overloaded, capacities);

        assertTrue(balancedScore > overloadedScore,
            "Balanced placement should have better fitness than overloaded placement");
    }
}
