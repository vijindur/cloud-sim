package com.cloudoptimizer.pso;

import java.util.Random;

public class Particle {
    public final double[] position;
    public final double[] velocity;
    public double[] bestPosition;
    public double bestFitness;

    public Particle(int dimensions, Random random) {
        this.position = new double[dimensions];
        this.velocity = new double[dimensions];
        this.bestPosition = new double[dimensions];
        this.bestFitness = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < dimensions; i++) {
            position[i] = random.nextDouble();
            velocity[i] = random.nextDouble() * 0.1;
            bestPosition[i] = position[i];
        }
    }
}
