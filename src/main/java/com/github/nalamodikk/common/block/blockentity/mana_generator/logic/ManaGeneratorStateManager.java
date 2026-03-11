package com.github.nalamodikk.common.block.blockentity.mana_generator.logic;

import com.github.nalamodikk.KoniavacraftMod;

import java.util.function.Consumer;

public class ManaGeneratorStateManager {

    public enum Mode {
        MANA,
        ENERGY
    }

    private boolean isWorking = false;
    private Mode currentMode = Mode.MANA;

    public boolean isWorking() {
        return isWorking;
    }

    public void setWorking(boolean working, Consumer<Boolean> onChange) {
        if (this.isWorking != working) {
            this.isWorking = working;
            onChange.accept(working);
        }
    }

    public boolean setWorking(boolean working) {
        boolean changed = this.isWorking != working;
        this.isWorking = working;
        return changed;  // ✅ 返回是否有變化
    }

    public int getCurrentModeIndex() {
        return currentMode == Mode.MANA ? 0 : 1;
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    public boolean toggleMode(int burnTime) {
        if (isWorking || burnTime > 0) {
            KoniavacraftMod.LOGGER.debug("Cannot toggle mana generator mode while it is running.");
            return false;
        }
        currentMode = (currentMode == Mode.MANA) ? Mode.ENERGY : Mode.MANA;
        return true;
    }

    public void setModeIndex(int index) {
        this.currentMode = (index == 0) ? Mode.MANA : Mode.ENERGY;
    }


    public boolean isModeMana() {
        return currentMode == Mode.MANA;
    }

    public void reset() {
        isWorking = false;
    }
}
