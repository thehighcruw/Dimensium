package github.thehighcruw.dimensium.tool.state;

import java.util.ArrayList;
import java.util.List;

public class LassoSelectToolState {

    public static final LassoSelectToolState INSTANCE = new LassoSelectToolState();

    public int lassoDepth = 8;
    public boolean lassoIncludeNonSolid = false;

    /** Screen-space polygon points [screenX, screenY] accumulated during RMB drag. */
    public final List<float[]> polygonPoints = new ArrayList<>();
    public boolean dragging = false;

    private LassoSelectToolState() {}
}
