package com.github.nalamodikk.research.grid;

import com.github.nalamodikk.research.aspect.Aspect;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The live state of a research puzzle grid (12x12 cells in axial coordinates).
 *
 * Internally stores cells in a Map<HexCoord, GridCell> so only valid
 * axial positions within the rectangular bounds are tracked.
 */
public class ResearchGrid {

    public static final int COLS = 12;
    public static final int ROWS = 12;

    private final Map<HexCoord, GridCell> cells;

    ResearchGrid(Map<HexCoord, GridCell> cells) {
        this.cells = new HashMap<>(cells);
    }

    // ── Cell access ──────────────────────────────────────────────────────────

    @Nullable
    public GridCell getCell(HexCoord coord) {
        return cells.get(coord);
    }

    public boolean inBounds(HexCoord coord) {
        return cells.containsKey(coord);
    }

    /** Returns all coordinates in the grid. */
    public Set<HexCoord> allCoords() {
        return Collections.unmodifiableSet(cells.keySet());
    }

    // ── Player actions ───────────────────────────────────────────────────────

    /**
     * Place an aspect on an EMPTY cell. Returns the updated grid.
     * Throws if the cell is not EMPTY.
     */
    public ResearchGrid placeAspect(HexCoord coord, Aspect aspect) {
        GridCell cell = cells.get(coord);
        if (cell == null || !cell.isEmpty())
            throw new IllegalArgumentException("Cell " + coord + " is not empty");
        Map<HexCoord, GridCell> updated = new HashMap<>(cells);
        updated.put(coord, GridCell.placed(aspect));
        return new ResearchGrid(updated);
    }

    /**
     * Remove a player-placed aspect from a cell. Returns the updated grid.
     * Throws if the cell is not PLACED.
     */
    public ResearchGrid removeAspect(HexCoord coord) {
        GridCell cell = cells.get(coord);
        if (cell == null || !cell.isPlaced())
            throw new IllegalArgumentException("Cell " + coord + " has no player-placed aspect");
        Map<HexCoord, GridCell> updated = new HashMap<>(cells);
        updated.put(coord, GridCell.empty());
        return new ResearchGrid(updated);
    }

    // ── Completion check ─────────────────────────────────────────────────────

    /**
     * Returns true when all FIXED nodes are connected in a single component
     * via cells that have aspects, where adjacent cells connect only if their
     * aspects satisfy {@link Aspect#canConnectTo(Aspect)}.
     */
    public boolean isComplete() {
        List<HexCoord> fixedCoords = cells.entrySet().stream()
                .filter(e -> e.getValue().isFixed())
                .map(Map.Entry::getKey)
                .toList();

        if (fixedCoords.size() < 2) return false;

        Set<HexCoord> reachable = bfsAspectConnected(fixedCoords.get(0));
        return reachable.containsAll(fixedCoords);
    }

    /**
     * BFS through cells that have aspects, following the canConnectTo rule
     * for adjacent pairs.
     */
    private Set<HexCoord> bfsAspectConnected(HexCoord start) {
        Set<HexCoord> visited = new HashSet<>();
        Deque<HexCoord> queue = new ArrayDeque<>();

        GridCell startCell = cells.get(start);
        if (startCell == null || !startCell.hasAspect()) return visited;

        queue.add(start);
        while (!queue.isEmpty()) {
            HexCoord curr = queue.poll();
            if (!visited.add(curr)) continue;

            Aspect currAspect = cells.get(curr).getAspect();
            for (HexCoord neighbor : curr.neighbors()) {
                if (visited.contains(neighbor)) continue;
                GridCell neighborCell = cells.get(neighbor);
                if (neighborCell == null || !neighborCell.hasAspect()) continue;

                if (currAspect.canConnectTo(neighborCell.getAspect())) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    // ── Solvability check (used during generation) ───────────────────────────

    /**
     * Checks whether a solution exists: all FIXED nodes are in the same
     * connected component of non-HOLE cells (ignoring aspect constraints,
     * since the player can choose any aspect for EMPTY cells).
     */
    public boolean isSolvable() {
        List<HexCoord> fixedCoords = cells.entrySet().stream()
                .filter(e -> e.getValue().isFixed())
                .map(Map.Entry::getKey)
                .toList();

        if (fixedCoords.size() < 2) return true;

        Set<HexCoord> reachable = bfsPassable(fixedCoords.get(0));
        return reachable.containsAll(fixedCoords);
    }

    private Set<HexCoord> bfsPassable(HexCoord start) {
        Set<HexCoord> visited = new HashSet<>();
        Deque<HexCoord> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            HexCoord curr = queue.poll();
            if (!visited.add(curr)) continue;
            for (HexCoord neighbor : curr.neighbors()) {
                if (visited.contains(neighbor)) continue;
                GridCell cell = cells.get(neighbor);
                if (cell != null && cell.isPassable()) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }
}
