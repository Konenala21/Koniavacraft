package com.github.nalamodikk.particle.animation;

import java.util.HashSet;
import java.util.Set;

/**
 * ??∵?倦?? */
public class AnimateNode {
    private final Set<AnimateAction> animates = new HashSet<>();
    private int timestamp = 0;

    public void addAction(AnimateAction action) { animates.add(action); }

    public void onStart() {
        for (AnimateAction action : animates) {
            action.onStart();
        }
        timestamp = 0;
    }

    public void tick() {
        for (AnimateAction action : animates) {
            if (action.check() || action.getTimeStart() > timestamp) {
                continue;
            }
            action.doTick();
            if (action.check()) {
                action.onDone();
            }
        }
        timestamp++;
    }

    public boolean checkDone() {
        for (AnimateAction action : animates) {
            if (!action.check()) return false;
        }
        return true;
    }
}