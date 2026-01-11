package com.github.nalamodikk.particle.control;

public enum ControlType {
    CREATE(0),
    CHANGE(1),
    REMOVE(2);

    private final int id;

    ControlType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ControlType getTypeById(int id) {
        return switch (id) {
            case 0 -> CREATE;
            case 1 -> CHANGE;
            case 2 -> REMOVE;
            default -> CHANGE;
        };
    }
}
