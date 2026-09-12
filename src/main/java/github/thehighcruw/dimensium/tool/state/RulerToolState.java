package github.thehighcruw.dimensium.tool.state;

import java.util.ArrayList;
import java.util.List;

public class RulerToolState {

    public static final RulerToolState INSTANCE = new RulerToolState();

    public enum Mode {

        DEFAULT("dimensium.ruler.mode.default"),
        CIRCLE("dimensium.ruler.mode.circle");

        public final String label;

        Mode(String l) {
            label = l;
        }
    }

    public final List<int[]> points = new ArrayList<>();
    public Mode mode = Mode.DEFAULT;
}
