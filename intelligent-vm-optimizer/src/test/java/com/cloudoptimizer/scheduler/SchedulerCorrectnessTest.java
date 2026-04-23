package com.cloudoptimizer.scheduler;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerCorrectnessTest {

    @Test
    void psoSchedulersShouldReturnValidMappings() {
        SchedulerFactory factory = new SchedulerFactory();
        List<Double> capacities = List.of(1000.0, 2000.0, 1500.0);

        for (String name : List.of("PSO_STANDARD", "PSO_MODIFIED")) {
            SchedulerStrategy scheduler = factory.fromName(name);
            int[] mapping = scheduler.schedule(20, 3, capacities);

            assertEquals(20, mapping.length, "mapping must cover all tasks");
            for (int vm : mapping) {
                assertTrue(vm >= 0 && vm < 3, "task mapped to out-of-range VM");
            }
        }
    }
}
