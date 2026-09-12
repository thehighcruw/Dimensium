package github.thehighcruw.dimensium.tool.state;

public class WeldToolState {

    public static final WeldToolState INSTANCE = new WeldToolState();

    public int weldSmoothStrength = 2;
    public float weldThreshold = 0.5f;
    public boolean weldReplaceSolid = false;
}
