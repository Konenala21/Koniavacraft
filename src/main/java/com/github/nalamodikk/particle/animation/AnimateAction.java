package com.github.nalamodikk.particle.animation;

/**
 * ??∵????蝎?
 */
public abstract class AnimateAction {
    protected boolean done = false;
    protected int timeStart = 0;
    protected int tickCount = 0;

    public void setTimeStart(int timeStart) { this.timeStart = timeStart; }
    public int getTimeStart() { return timeStart; }

    public abstract boolean checkDone();
    public abstract void tick();

    public void doTick() {
        tick();
        tickCount++;
    }

    public abstract void onStart();
    public abstract void onDone();

    public boolean check() {
        if (!done && checkDone()) {
            done = true;
        }
        return done;
    }
    
    public boolean isDone() { return done; }
}