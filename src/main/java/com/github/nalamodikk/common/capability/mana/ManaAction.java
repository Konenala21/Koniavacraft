package com.github.nalamodikk.common.capability.mana;

/**
 * Enum for representing the type of action to be performed on mana, such as executing or simulating.
 */
public enum ManaAction {
    EXECUTE,
    SIMULATE;

    /**
     * @return {@code true} if this action represents execution.
     */
    public boolean execute() {
        return this == EXECUTE;
    }

    /**
     * @return {@code true} if this action represents simulation.
     */
    public boolean simulate() {
        return this == SIMULATE;
    }

    /**
     * Helper to combine this action with a boolean based execution.
     * This allows easily compounding actions.
     *
     * @param execute {@code true} if it should execute if this action already is an execute action.
     * @return Compounded action.
     */
    public ManaAction combine(boolean execute) {
        return get(execute && execute());
    }

    /**
     * Helper to get an action based on a boolean representing execution.
     *
     * @param execute {@code true} for {@link #EXECUTE}.
     * @return ManaAction.
     */
    public static ManaAction get(boolean execute) {
        return execute ? EXECUTE : SIMULATE;
    }
}
