package github.thehighcruw.dimensium.tool;

public enum BuilderTool {

    MOVE("Move"),
    CLONE("Clone"),
    STACK("Stack"),
    SMEAR("Smear"),
    EXTRUDE("Extrude"),
    ERASE("Erase"),
    SETUP_SYMMETRY("Symmetry");

    public final String label;

    BuilderTool(String label) {
        this.label = label;
    }

    private static final BuilderTool[] VALUES = values();

    public BuilderTool next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public BuilderTool prev() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }
}
