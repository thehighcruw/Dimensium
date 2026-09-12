package github.thehighcruw.dimensium.tool.state;

public class MagicSelectToolState {

    public static final MagicSelectToolState INSTANCE = new MagicSelectToolState();

    public enum MagicCompareType {

        BLOCK_STATE("dimensium.magic_compare.block_state"),
        BLOCK("dimensium.magic_compare.block"),
        SOLID("dimensium.magic_compare.solid"),
        ANY("dimensium.magic_compare.any");

        public final String label;

        MagicCompareType(String label) {
            this.label = label;
        }
    }

    public enum MagicDirection {

        BOTH("dimensium.magic_dir.both"),
        UP_ONLY("dimensium.magic_dir.up_only"),
        DOWN_ONLY("dimensium.magic_dir.down_only");

        public final String label;

        MagicDirection(String label) {
            this.label = label;
        }
    }

    public int magicSelectLimit = 10000;
    public int magicSelectRange = 1;
    public boolean magicSelectSurface = false;
    public boolean magicSelectCorners = false;
    public MagicCompareType magicCompareType = MagicCompareType.BLOCK_STATE;
    public MagicDirection magicDirection = MagicDirection.BOTH;
}
