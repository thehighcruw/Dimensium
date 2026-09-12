package github.thehighcruw.dimensium.tool.state;

public class ElevationToolState {

    public static final ElevationToolState INSTANCE = new ElevationToolState();

    public enum ElevationMode {

        RAISE("dimensium.elevation_mode.raise"),
        LOWER("dimensium.elevation_mode.lower"),
        FLATTEN("dimensium.elevation_mode.flatten");

        public final String label;

        ElevationMode(String l) {
            label = l;
        }
    }

    public enum ElevationApply {

        ONCE("dimensium.elevation_apply.once"),
        CONTINUOUS("dimensium.elevation_apply.continuous");

        public final String label;

        ElevationApply(String l) {
            label = l;
        }
    }

    public enum ElevationFalloff {

        FLAT("dimensium.elevation_falloff.flat"),
        SPHERICAL("dimensium.elevation_falloff.spherical"),
        LINEAR("dimensium.elevation_falloff.linear"),
        LOGARITHMIC("dimensium.elevation_falloff.logarithmic"),
        NORMAL("dimensium.elevation_falloff.normal"),
        PEAK("dimensium.elevation_falloff.peak");

        public final String label;

        ElevationFalloff(String l) {
            label = l;
        }
    }

    public enum FlattenDirection {

        BOTH("dimensium.flatten_dir.both"),
        UP("dimensium.flatten_dir.up"),
        DOWN("dimensium.flatten_dir.down");

        public final String label;

        FlattenDirection(String l) {
            label = l;
        }
    }

    public static final int RADIUS_MIN = 1;
    public static final int RADIUS_MAX = 32;
    public static final float SMOOTHING_MIN = 0.0f;
    public static final float SMOOTHING_MAX = 1.0f;
    public static final float RATE_MIN = 0.1f;
    public static final float RATE_MAX = 32f;
    public static final int STRENGTH_MIN = 1;
    public static final int STRENGTH_MAX = 32;

    public ElevationMode elevationMode = ElevationMode.RAISE;
    public ElevationApply elevationApply = ElevationApply.CONTINUOUS;
    public ElevationFalloff elevationFalloff = ElevationFalloff.LINEAR;
    public FlattenDirection flattenDirection = FlattenDirection.BOTH;
    public int elevationRadius = 5;
    public float elevationSmoothing = 0.0f;
    public float elevationRate = 8.0f;
    public int elevationStrength = 5;
}
