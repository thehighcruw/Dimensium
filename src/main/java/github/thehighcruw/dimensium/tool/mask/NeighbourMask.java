package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class NeighbourMask extends MaskNode {

    public int blockId;
    public int meta;

    private static final int[][] OFFSETS = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    public NeighbourMask(int blockId, int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        for (int[] o : OFFSETS) {
            Block b = world.getBlock(x + o[0], y + o[1], z + o[2]);
            if (Block.getIdFromBlock(b) != blockId) continue;
            if (meta >= 0 && world.getBlockMetadata(x + o[0], y + o[1], z + o[2]) != meta) continue;
            return true;
        }
        return false;
    }

    @Override
    public String displayName() {
        return "Neighbour: " + blockLabel(blockId, meta);
    }
}
