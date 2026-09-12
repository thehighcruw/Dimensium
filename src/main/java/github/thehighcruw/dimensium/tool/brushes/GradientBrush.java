package github.thehighcruw.dimensium.tool.brushes;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.math.PathMath;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.GradientToolState;
import github.thehighcruw.dimensium.tool.state.PaletteState;

public class GradientBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        GradientToolState s = GradientToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        PaletteState ps = PaletteState.INSTANCE;
        if (ps.palette.isEmpty()) return;
        if (!s.gradientHasPos1 || !s.gradientHasPos2) return;

        int bx = mop.blockX, by = mop.blockY, bz = mop.blockZ;
        int x = s.gradientPos2X, y = s.gradientPos2Y, z = s.gradientPos2Z;
        int px = s.gradientPos1X, py = s.gradientPos1Y, pz = s.gradientPos1Z;

        double axisX = px - x, axisY = py - y, axisZ = pz - z;
        double len2 = axisX * axisX + axisY * axisY + axisZ * axisZ;
        double len = Math.sqrt(len2);
        boolean samePos = len < 0.001;

        float bezP1 = 0f, bezP2 = 1f;
        if (s.gradientInterp == GradientToolState.GradientInterp.BEZIER) {
            java.util.Random bzr = new java.util.Random(s.gradientSeed);
            bezP1 = bzr.nextFloat();
            bezP2 = bzr.nextFloat();
        }
        final float bp1 = bezP1, bp2 = bezP2;
        final int paletteN = ps.palette.size();

        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = bx + dx, wy = by + dy, wz = bz + dz;
            if (world.getBlock(wx, wy, wz) == Blocks.air) return;
            if (s.gradientMaskSurface && !BrushUtil.hasAirNeighbor(world, wx, wy, wz)) return;

            float t;
            if (samePos) {
                t = 0f;
            } else if (s.gradientShape == GradientToolState.GradientShape.SPHERE) {
                double ex = wx - px, ey = wy - py, ez = wz - pz;
                t = 1f - (float) (Math.sqrt(ex * ex + ey * ey + ez * ez) / len);
            } else {
                double dot = (wx - x) * axisX + (wy - y) * axisY + (wz - z) * axisZ;
                t = (float) (dot / len2);
            }

            if (s.gradientClampToEdge && (t < 0f || t > 1f)) return;

            if (s.gradientInterp == GradientToolState.GradientInterp.LINEAR) {
                t += (float) PathMath.voxelHash(wx, wy, wz, 0x5EEDC0DEL) * (0.5f / paletteN);
            } else if (s.gradientInterp == GradientToolState.GradientInterp.BEZIER) {
                float tc = Math.max(0f, Math.min(1f, t));
                float inv = 1f - tc;
                t = 3f * inv * inv * tc * bp1 + 3f * inv * tc * tc * bp2 + tc * tc * tc;
                t += (float) PathMath.voxelHash(wx, wy, wz, s.gradientSeed) * (0.5f / paletteN);
            }

            int palIdx = paletteIdx(s, ps, t);
            ItemStack item = ps.palette.get(palIdx);
            Block blk = Block.getBlockFromItem(item.getItem());
            int meta = item.getItemDamage();
            if (blk != null && blk != Blocks.air) ChangeProposal.write(world, wx, wy, wz, blk, meta);
        });
    }

    private static int paletteIdx(GradientToolState s, PaletteState ps, float t) {
        int total = ps.totalPaletteWeight();
        if (total == 0) return 0;
        int target;
        if (s.gradientInterp == GradientToolState.GradientInterp.NEAREST) {
            target = Math.round(t * (total - 1));
        } else {
            target = (int) (t * total);
        }
        target = Math.max(0, Math.min(total - 1, target));
        int cum = 0;
        for (int i = 0; i < ps.palette.size(); i++) {
            cum += ps.getWeight(i);
            if (target < cum) return i;
        }
        return ps.palette.size() - 1;
    }
}
