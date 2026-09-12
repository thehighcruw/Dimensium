package github.thehighcruw.dimensium.tool.state;

public enum BrushShape {

    SPHERE("dimensium.brush_shape.sphere", false),
    CUBE("dimensium.brush_shape.cube", false),
    CYLINDER("dimensium.brush_shape.cylinder", true),
    ELLIPSOID("dimensium.brush_shape.ellipsoid", true),
    CUBOID("dimensium.brush_shape.cuboid", true),
    CAPSULE("dimensium.brush_shape.capsule", true),
    CONE("dimensium.brush_shape.cone", true),
    OCTAHEDRON("dimensium.brush_shape.octahedron", true);

    public final String label;
    public final boolean hasHeight;

    BrushShape(String label, boolean hasHeight) {
        this.label = label;
        this.hasHeight = hasHeight;
    }
}
