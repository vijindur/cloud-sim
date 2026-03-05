package com.cloudoptimizer.decision;

import com.cloudoptimizer.workload.WorkloadAnalyzer.WorkloadProfile;

public class AdaptiveDecisionEngine {

    public enum Decision {
        HEURISTIC,
        PSO,
        HYBRID
    }

    public Decision decide(WorkloadProfile profile) {
        if (profile.loadFactor() < 0.3 && profile.variability() < 0.2) {
            return Decision.HEURISTIC;
        }
        if (profile.loadFactor() > 0.7 || profile.variability() > 0.5) {
            return Decision.PSO;
        }
        return Decision.HYBRID;
    }
}
