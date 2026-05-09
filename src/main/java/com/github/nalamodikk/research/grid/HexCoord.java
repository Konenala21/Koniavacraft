package com.github.nalamodikk.research.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Axial coordinates for a hexagonal grid.
 * Uses the standard (q, r) axial system from redblobgames.com/grids/hexagons/.
 *
 * Neighbours: (±1,0), (0,±1), (±1,∓1) — exactly 6 per cell.
 */
public record HexCoord(int q, int r) {

    private static final int[][] NEIGHBOR_DELTAS = {
            {1, 0}, {-1, 0},
            {0, 1}, {0, -1},
            {1, -1}, {-1, 1}
    };

    public List<HexCoord> neighbors() {
        List<HexCoord> result = new ArrayList<>(6);
        for (int[] d : NEIGHBOR_DELTAS) {
            result.add(new HexCoord(q + d[0], r + d[1]));
        }
        return result;
    }

    /** Convert axial (q,r) → offset col/row for a rectangular grid (odd-q layout). */
    public static HexCoord fromOffset(int col, int row) {
        int q = col;
        int r = row - (col - (col & 1)) / 2;
        return new HexCoord(q, r);
    }

    public int toOffsetCol() { return q; }

    public int toOffsetRow() { return r + (q - (q & 1)) / 2; }

    public int distanceTo(HexCoord other) {
        return (Math.abs(q - other.q)
                + Math.abs(q + r - other.q - other.r)
                + Math.abs(r - other.r)) / 2;
    }
}
