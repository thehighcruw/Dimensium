package github.thehighcruw.dimensium.tool.state;

public class ExtrudeToolState {

    public static final ExtrudeToolState INSTANCE = new ExtrudeToolState();

    public enum ExtrudeMode {

        EXPAND("dimensium.extrude_mode.expand"),
        SHRINK("dimensium.extrude_mode.shrink");

        public final String label;

        ExtrudeMode(String l) {
            label = l;
        }
    }

    public ExtrudeMode extrudeMode = ExtrudeMode.EXPAND;
    public boolean extrudeDisplace = true;
    public int extrudeLimit = 100000;
    public boolean extrudeCorners = false;
    public int extrudeCount = 1;
}
