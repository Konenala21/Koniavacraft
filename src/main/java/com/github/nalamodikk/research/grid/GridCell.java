package com.github.nalamodikk.research.grid;

import com.github.nalamodikk.research.aspect.Aspect;
import org.jetbrains.annotations.Nullable;

/**
 * State of a single cell in the research grid.
 *
 * EMPTY  — player can place an aspect here
 * HOLE   — blocked, cannot be used
 * FIXED  — pre-placed aspect node (defined by the research template, cannot be moved)
 * PLACED — aspect placed by the player during the puzzle
 */
public final class GridCell {

    public enum Type { EMPTY, HOLE, FIXED, PLACED }

    private final Type type;
    @Nullable
    private final Aspect aspect;

    private GridCell(Type type, @Nullable Aspect aspect) {
        this.type = type;
        this.aspect = aspect;
    }

    public static GridCell empty() { return new GridCell(Type.EMPTY, null); }
    public static GridCell hole()  { return new GridCell(Type.HOLE, null); }
    public static GridCell fixed(Aspect aspect) { return new GridCell(Type.FIXED, aspect); }
    public static GridCell placed(Aspect aspect) { return new GridCell(Type.PLACED, aspect); }

    public Type getType() { return type; }

    @Nullable
    public Aspect getAspect() { return aspect; }

    public boolean isEmpty()    { return type == Type.EMPTY; }
    public boolean isHole()     { return type == Type.HOLE; }
    public boolean isFixed()    { return type == Type.FIXED; }
    public boolean isPlaced()   { return type == Type.PLACED; }
    public boolean hasAspect()  { return aspect != null; }
    public boolean isBlocked()  { return type == Type.HOLE; }
    public boolean isPassable() { return type != Type.HOLE; }

    public GridCell withAspect(Aspect newAspect) {
        if (type != Type.EMPTY) throw new IllegalStateException("Can only place on EMPTY cells");
        return placed(newAspect);
    }

    public GridCell cleared() {
        if (type != Type.PLACED) throw new IllegalStateException("Can only clear PLACED cells");
        return empty();
    }
}
