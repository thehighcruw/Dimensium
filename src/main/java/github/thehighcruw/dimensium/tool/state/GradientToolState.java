package github.thehighcruw.dimensium.tool.state;

public class GradientToolState {

    public static final GradientToolState INSTANCE = new GradientToolState();

    public enum GradientShape {

        PLANE("dimensium.gradient_shape.plane"),
        SPHERE("dimensium.gradient_shape.sphere");

        public final String label;

        GradientShape(String l) {
            label = l;
        }
    }

    public enum GradientInterp {

        NEAREST("dimensium.gradient_interp.nearest"),
        LINEAR("dimensium.gradient_interp.linear"),
        BEZIER("dimensium.gradient_interp.bezier");

        public final String label;

        GradientInterp(String l) {
            label = l;
        }
    }

    public GradientShape gradientShape = GradientShape.PLANE;
    public GradientInterp gradientInterp = GradientInterp.LINEAR;
    public boolean gradientMaskSurface = false;
    public boolean gradientClampToEdge = false;
    public long gradientSeed = java.util.concurrent.ThreadLocalRandom.current()
        .nextLong();
    public boolean gradientHasPos1 = false;
    public int gradientPos1X = 0;
    public int gradientPos1Y = 0;
    public int gradientPos1Z = 0;
    public boolean gradientHasPos2 = false;
    public int gradientPos2X = 0;
    public int gradientPos2Y = 0;
    public int gradientPos2Z = 0;
}
