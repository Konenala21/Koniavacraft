package com.github.nalamodikk.common.inventory.sort;

public enum SortMode {
    BY_TYPE, BY_NAME, BY_COUNT;

    public SortMode next() {
        SortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
