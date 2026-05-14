package com.cloudoptimizer.pso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudoptimizer.core.SimulationConfig;
import org.junit.jupiter.api.Test;

class FitnessModelTest {
    @Test
    void normalizesConfiguredWeights() {
        FitnessModel model = new FitnessModel(new SimulationConfig.FitnessWeights(2, 2, 2, 2, 1, 1));

        double score = model.score(1.0, 0.8, 0.6, 0.4, 0.2, 0.0);

        assertTrue(score > 0.0 && score < 1.0);
        assertEquals(0.58, score, 0.001);
    }
}
