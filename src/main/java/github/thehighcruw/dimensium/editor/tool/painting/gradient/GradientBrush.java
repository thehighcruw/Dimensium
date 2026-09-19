/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.gradient;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.creating.path.PathMath;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class GradientBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        GradientToolState s = GradientToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        PaletteState ps = PaletteState.INSTANCE;
        if (ps.palette.isEmpty()) return;
        if (!s.gradientHasPos1 || !s.gradientHasPos2) return;

        Vec3DInt coord = WorldUtils.mopToCoord(mop);
        Vec3DDouble pos2 = s.gradientPos2.toDouble();
        Vec3DDouble pos1 = s.gradientPos1.toDouble();
        Vec3DDouble axis = pos1.minus(pos2);
        double len2 = axis.lengthSq();
        double len = axis.length();
        boolean samePos = len < 0.001;

        float bezP1 = 0f, bezP2 = 1f;
        if (s.gradientInterp == GradientToolState.GradientInterp.BEZIER) {
            Random bzr = new Random(s.gradientSeed);
            bezP1 = bzr.nextFloat();
            bezP2 = bzr.nextFloat();
        }
        final float bp1 = bezP1, bp2 = bezP2;
        final int paletteN = ps.palette.size();

        BrushUtil.forBrush(bs, offset -> {
            Vec3DInt pos = coord.plus(offset);
            if (WorldUtils.getBlock(world, pos) == Blocks.air) return;
            if (s.gradientMaskSurface && BrushUtil.hasSolidNeighbor(world, pos)) return;

            float t;
            if (samePos) {
                t = 0f;
            } else if (s.gradientShape == GradientToolState.GradientShape.SPHERE) {
                t = 1f
                        - (float) (Vec3DDouble.from(pos.x(), pos.y(), pos.z())
                                        .minus(pos1)
                                        .length()
                                / len);
            } else {
                t = (float)
                        (Vec3DDouble.from(pos.x(), pos.y(), pos.z()).minus(pos2).dot(axis) / len2);
            }

            if (s.gradientClampToEdge && (t < 0f || t > 1f)) return;

            if (s.gradientInterp == GradientToolState.GradientInterp.LINEAR) {
                t += (float) PathMath.voxelHash(pos.x(), pos.y(), pos.z(), 0x5EEDC0DEL) * (0.5f / paletteN);
            } else if (s.gradientInterp == GradientToolState.GradientInterp.BEZIER) {
                float tc = Math.max(0f, Math.min(1f, t));
                float inv = 1f - tc;
                t = 3f * inv * inv * tc * bp1 + 3f * inv * tc * tc * bp2 + tc * tc * tc;
                t += (float) PathMath.voxelHash(pos.x(), pos.y(), pos.z(), s.gradientSeed) * (0.5f / paletteN);
            }

            int palIdx = paletteIdx(s, ps, t);
            BrushUtil.writeFromItem(world, pos, ps.palette.get(palIdx));
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
