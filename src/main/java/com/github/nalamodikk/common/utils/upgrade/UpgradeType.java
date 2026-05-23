package com.github.nalamodikk.common.utils.upgrade;

public enum UpgradeType {
    SPEED("speed"),
    EFFICIENCY("efficiency"),
    // Mana Generator 升級類型
    ACCELERATED_PROCESSING("accelerated_processing"),
    EXPANDED_FUEL_CHAMBER("expanded_fuel_chamber"),
    CATALYTIC_CONVERTER("catalytic_converter"),
    DIAGNOSTIC_DISPLAY("diagnostic_display"),
    MANA_OUTPUT("mana_output"),
    ENERGY_OUTPUT("energy_output");

    private final String name;

    UpgradeType(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return name;
    }
}
