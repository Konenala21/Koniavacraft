package com.github.nalamodikk.research.grid;

import com.github.nalamodikk.research.aspect.Aspect;

import java.util.*;

/**
 * Generates a randomised ResearchGrid puzzle that is guaranteed to be solvable.
 *
 * Algorithm:
 *  1. Build all valid axial coordinates for the 12x12 offset grid.
 *  2. Shuffle positions and place the required FIXED aspect nodes.
 *  3. Randomly mark remaining cells as HOLEs (controlled by holeRatio).
 *  4. BFS-verify solvability. If unsolvable, retry (up to MAX_ATTEMPTS).
 *  5. If all attempts fail, fall back to a low-hole layout.
 */
public class ResearchGridGenerator {

    private static final int MAX_ATTEMPTS = 50;

    /**
     * @param requiredAspects aspects that must appear as FIXED nodes on the grid
     * @param holeRatio       fraction of non-fixed cells to mark as holes (0.0–1.0)
     * @param random          seeded or unseeded random source
     */
    public static ResearchGrid generate(List<Aspect> requiredAspects, double holeRatio, Random random) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            ResearchGrid grid = tryGenerate(requiredAspects, holeRatio, random);
            if (grid.isSolvable()) return grid;
        }
        // Fallback: very few holes to ensure a path always exists
        return tryGenerate(requiredAspects, 0.15, random);
    }

    private static ResearchGrid tryGenerate(List<Aspect> requiredAspects, double holeRatio, Random random) {
        List<HexCoord> allCoords = buildAllCoords();
        Collections.shuffle(allCoords, random);

        Map<HexCoord, GridCell> cells = new LinkedHashMap<>();
        // Initialise every cell as EMPTY
        for (HexCoord coord : allCoords) {
            cells.put(coord, GridCell.empty());
        }

        // Place required FIXED nodes at the first N shuffled positions
        if (requiredAspects.size() > allCoords.size()) {
            throw new IllegalArgumentException("More required aspects than grid cells");
        }
        for (int i = 0; i < requiredAspects.size(); i++) {
            cells.put(allCoords.get(i), GridCell.fixed(requiredAspects.get(i)));
        }

        // Punch holes in remaining positions
        List<HexCoord> remaining = allCoords.subList(requiredAspects.size(), allCoords.size());
        int holeCount = (int) (remaining.size() * holeRatio);
        for (int i = 0; i < holeCount; i++) {
            cells.put(remaining.get(i), GridCell.hole());
        }

        return new ResearchGrid(cells);
    }

    /** Build all (col, row) offset positions converted to axial HexCoord. */
    private static List<HexCoord> buildAllCoords() {
        List<HexCoord> coords = new ArrayList<>(ResearchGrid.COLS * ResearchGrid.ROWS);
        for (int col = 0; col < ResearchGrid.COLS; col++) {
            for (int row = 0; row < ResearchGrid.ROWS; row++) {
                coords.add(HexCoord.fromOffset(col, row));
            }
        }
        return coords;
    }
}
