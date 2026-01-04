package com.github.nalamodikk.common.coreapi.block.mana;

public interface IManaUsingMachine {
    int getManaStored();
    boolean hasSufficientMana(int cost);
    void consumeMana(int cost);
}
