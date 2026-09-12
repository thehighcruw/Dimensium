package github.thehighcruw.dimensium.tool.state;

public class MeltToolState {

    public static final MeltToolState INSTANCE = new MeltToolState();

    public int meltSmoothStrength = 2;
    public float meltThreshold = 0.5f;
}
