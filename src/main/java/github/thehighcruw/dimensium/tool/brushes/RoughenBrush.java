package github.thehighcruw.dimensium.tool.brushes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.RoughenToolState;

public class RoughenBrush implements BrushStrategy {

    private static final int[][] FACE_DIRS = BrushUtil.FACE_DIRS;
    private static final Random RNG = new Random();

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        RoughenToolState s = RoughenToolState.INSTANCE;
        int ox = mop.blockX, oy = mop.blockY, oz = mop.blockZ;

        List<int[]> candidates = new ArrayList<>();
        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = ox + dx, wy = oy + dy, wz = oz + dz;
            if (world.getBlock(wx, wy, wz) == Blocks.air) return;
            int exposed = countAirFaces(world, wx, wy, wz);
            if (exposed < s.faces) {
                candidates.add(new int[] { wx, wy, wz });
            }
        });

        Collections.shuffle(candidates, RNG);
        int count = Math.round(candidates.size() * s.rougheningRatio);
        for (int i = 0; i < count; i++) {
            int[] pos = candidates.get(i);
            ChangeProposal.write(world, pos[0], pos[1], pos[2], Blocks.air, 0);
        }
    }

    private static int countAirFaces(World world, int wx, int wy, int wz) {
        int count = 0;
        for (int[] d : FACE_DIRS) {
            if (world.getBlock(wx + d[0], wy + d[1], wz + d[2]) == Blocks.air) count++;
        }
        return count;
    }
}
