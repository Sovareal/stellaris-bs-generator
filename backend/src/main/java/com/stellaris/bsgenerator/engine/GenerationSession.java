package com.stellaris.bsgenerator.engine;

/**
 * Tracks the state of a generation session, including the current empire
 * and whether the single reroll has been used.
 */
public class GenerationSession {

    private GeneratedEmpire empire;
    private boolean hasRerolled = false;

    public GenerationSession(GeneratedEmpire empire) {
        this.empire = empire;
    }

    public GeneratedEmpire getEmpire() {
        return empire;
    }

    public void setEmpire(GeneratedEmpire empire) {
        this.empire = empire;
    }

    public boolean canReroll() {
        return !hasRerolled;
    }

    public void markRerolled() {
        hasRerolled = true;
    }

    /**
     * Reset the session for a new generation (clears reroll state).
     */
    public void reset(GeneratedEmpire newEmpire) {
        this.empire = newEmpire;
        this.hasRerolled = false;
    }
}
