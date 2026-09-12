package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.world.World;

public class OrNode extends LogicNode {

    @Override
    public boolean test(World world, int x, int y, int z) {
        for (MaskNode child : children) {
            if (child.test(world, x, y, z)) return true;
        }
        return false;
    }

    @Override
    public String displayName() {
        return "OR";
    }
}
