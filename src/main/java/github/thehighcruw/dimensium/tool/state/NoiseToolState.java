package github.thehighcruw.dimensium.tool.state;

public class NoiseToolState {

    public static final NoiseToolState INSTANCE = new NoiseToolState();

    public enum NoiseType {

        SIMPLEX("dimensium.noise_type.simplex"),
        PERLIN("dimensium.noise_type.perlin"),
        VORONOI_EDGES("dimensium.noise_type.voronoi_edges"),
        WORLEY("dimensium.noise_type.worley"),
        METABALL("dimensium.noise_type.metaball"),
        WHITE("dimensium.noise_type.white"),
        SPLATTER("dimensium.noise_type.splatter");

        public final String label;

        NoiseType(String l) {
            label = l;
        }
    }

    public final NoiseParams noiseParams = new NoiseParams(NoiseType.SIMPLEX);

    public boolean noiseSurfaceOnly = false;
    public boolean noise3D = false;
}
