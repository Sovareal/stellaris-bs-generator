package com.stellaris.bsgenerator.engine;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tracks the state of a generation session, including the current empire
 * and which categories have been rerolled.
 */
public class GenerationSession {

    private GeneratedEmpire empire;
    private final Set<RerollCategory> rerollsUsed = EnumSet.noneOf(RerollCategory.class);

    public GenerationSession(GeneratedEmpire empire) {
        this.empire = empire;
    }

    public GeneratedEmpire getEmpire() {
        return empire;
    }

    public void setEmpire(GeneratedEmpire empire) {
        this.empire = empire;
    }

    public boolean canReroll(RerollCategory category) {
        return !rerollsUsed.contains(category);
    }

    public void markRerolled(RerollCategory category) {
        rerollsUsed.add(category);
    }

    public Set<RerollCategory> getRerollsUsed() {
        return Set.copyOf(rerollsUsed);
    }

    /**
     * Reset the session for a new generation (clears all rerolls).
     */
    public void reset(GeneratedEmpire newEmpire) {
        this.empire = newEmpire;
        this.rerollsUsed.clear();
    }
}
