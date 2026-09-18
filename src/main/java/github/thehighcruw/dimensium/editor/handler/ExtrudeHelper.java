/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.manipulating.extrude.ExtrudeToolState;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class ExtrudeHelper {

    public static final ExtrudeHelper INSTANCE = new ExtrudeHelper();

    public static void applyExtrudeAt(World world, Vec3DInt target, int sideHit) {
        Vec3DInt dir = sideToOutwardDir(sideHit);

        Block targetBlock = WorldUtils.getBlock(world, target);
        int targetMeta = WorldUtils.getBlockMetadata(world, target);
        if (targetBlock == Blocks.air) return;

        ExtrudeToolState s = ExtrudeToolState.INSTANCE;
        boolean expand = s.extrudeMode == ExtrudeToolState.ExtrudeMode.EXPAND;
        int count = Math.max(1, s.extrudeCount);

        List<Vec3DInt> connected =
                floodFillFace(world, target, dir, targetBlock, targetMeta, s.extrudeLimit, s.extrudeCorners);

        List<int[]> ops =
                buildExtrudeOps(world, expand, count, s.extrudeDisplace, connected, dir, targetBlock, targetMeta);

        if (!ops.isEmpty())
            BlockSender.sendChunked(
                    ops,
                    StatCollector.translateToLocal("dimensium.action.extrude") + " ("
                            + StatCollector.translateToLocal(s.extrudeMode.label)
                            + ")");
    }

    public static List<Vec3DInt> floodFillFace(
            World world, Vec3DInt start, Vec3DInt outDir, Block matchBlock, int matchMeta, int limit, boolean corners) {
        Vec3DInt[] perp = perpAxes(outDir);

        Set<Long> visited = new HashSet<>();
        Queue<Vec3DInt> queue = new LinkedList<>();
        List<Vec3DInt> result = new ArrayList<>();

        queue.add(start);
        visited.add(extrudeKey(start));

        while (!queue.isEmpty() && result.size() < limit) {
            Vec3DInt cur = queue.poll();
            result.add(cur);

            Vec3DInt[] steps = corners ? diagonalSteps(perp) : orthogonalSteps(perp);

            for (Vec3DInt step : steps) {
                Vec3DInt next = cur.plus(step);
                long k = extrudeKey(next);
                if (visited.contains(k)) continue;
                visited.add(k);
                Vec3DInt nextFace = next.plus(outDir);
                if (WorldUtils.getBlock(world, next) == matchBlock
                        && WorldUtils.getBlockMetadata(world, next) == matchMeta
                        && WorldUtils.getBlock(world, nextFace) == Blocks.air) {
                    queue.add(next);
                }
            }
        }

        return result;
    }

    private static Vec3DInt[] orthogonalSteps(Vec3DInt[] perp) {
        return new Vec3DInt[] {perp[0], perp[0].negate(), perp[1], perp[1].negate()};
    }

    private static Vec3DInt[] diagonalSteps(Vec3DInt[] perp) {
        Vec3DInt[] ortho = orthogonalSteps(perp);
        return new Vec3DInt[] {
            ortho[0],
            ortho[1],
            ortho[2],
            ortho[3],
            perp[0].plus(perp[1]),
            perp[0].minus(perp[1]),
            perp[0].negate().plus(perp[1]),
            perp[0].negate().minus(perp[1])
        };
    }

    /** Returns ops as List of {x, y, z, blockId, meta}. blockId=0 means erase. */
    private static List<int[]> buildExtrudeOps(
            World world,
            boolean expand,
            int count,
            boolean displace,
            List<Vec3DInt> connected,
            Vec3DInt dir,
            Block targetBlock,
            int targetMeta) {
        List<int[]> ops = new ArrayList<>();
        int targetId = Block.getIdFromBlock(targetBlock);
        if (expand) {
            for (int layer = 1; layer <= count; layer++) {
                for (Vec3DInt pos : connected) {
                    Vec3DInt n = pos.plus(dir.times(layer));
                    if (WorldUtils.getBlock(world, n) == Blocks.air) ops.add(n.toBlockOp(targetId, targetMeta));
                }
            }
        } else {
            for (int layer = 0; layer < count; layer++) {
                for (Vec3DInt pos : connected) {
                    Vec3DInt r = pos.minus(dir.times(layer));
                    if (WorldUtils.getBlock(world, r) != Blocks.air) ops.add(r.toBlockOp(0, 0));
                }
            }
            if (displace) {
                for (Vec3DInt pos : connected) {
                    Vec3DInt l = pos.minus(dir.times(count - 1));
                    Vec3DInt b = pos.minus(dir.times(count));
                    if (WorldUtils.getBlock(world, l) != Blocks.air && WorldUtils.getBlock(world, b) == Blocks.air)
                        ops.add(b.toBlockOp(targetId, targetMeta));
                }
            }
        }
        return ops;
    }

    public static Vec3DInt sideToOutwardDir(int side) {
        return switch (side) {
            case 0 -> Vec3DInt.from(0, -1, 0);
            case 1 -> Vec3DInt.from(0, 1, 0);
            case 2 -> Vec3DInt.from(0, 0, -1);
            case 3 -> Vec3DInt.from(0, 0, 1);
            case 4 -> Vec3DInt.from(-1, 0, 0);
            case 5 -> Vec3DInt.from(1, 0, 0);
            default -> throw new RuntimeException("Unknown side: " + side);
        };
    }

    public static Vec3DInt[] perpAxes(Vec3DInt dir) {
        if (dir.y() != 0) return new Vec3DInt[] {Vec3DInt.from(1, 0, 0), Vec3DInt.from(0, 0, 1)};
        if (dir.z() != 0) return new Vec3DInt[] {Vec3DInt.from(1, 0, 0), Vec3DInt.from(0, 1, 0)};
        return new Vec3DInt[] {Vec3DInt.from(0, 1, 0), Vec3DInt.from(0, 0, 1)};
    }

    public static long extrudeKey(Vec3DInt v) {
        return ChangeProposal.packKey(v);
    }

    private Vec3DInt lastExtrudePos = null;
    private int lastExtrudeSide = -1;

    public void resetExtrudeDedup() {
        lastExtrudePos = null;
    }

    /** Builds the extrude preview proposal for the block under the cursor. Called each render frame. */
    public void buildExtrudeProposal(Minecraft mc) {
        MovingObjectPosition mop = RenderUtils.raycastAtCursor();

        BuilderToolState bts = BuilderToolState.INSTANCE;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            bts.extrudePreview = null;
            lastExtrudePos = null;
            return;
        }

        Vec3DInt mopPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        if (mopPos.equals(lastExtrudePos) && mop.sideHit == lastExtrudeSide) return;
        lastExtrudePos = mopPos;
        lastExtrudeSide = mop.sideHit;

        Vec3DInt target = mopPos;
        Vec3DInt dir = sideToOutwardDir(mop.sideHit);
        Block targetBlock = WorldUtils.getBlock(mc.theWorld, target);
        if (targetBlock == Blocks.air) {
            bts.extrudePreview = null;
            return;
        }
        int targetMeta = WorldUtils.getBlockMetadata(mc.theWorld, target);

        ExtrudeToolState s = ExtrudeToolState.INSTANCE;
        int count = Math.max(1, s.extrudeCount);
        boolean expand = s.extrudeMode == ExtrudeToolState.ExtrudeMode.EXPAND;

        List<Vec3DInt> connected = floodFillFace(
                mc.theWorld, target, dir, targetBlock, targetMeta, Math.min(s.extrudeLimit, 4096), s.extrudeCorners);

        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] op : buildExtrudeOps(
                mc.theWorld, expand, count, s.extrudeDisplace, connected, dir, targetBlock, targetMeta)) {
            Vec3DInt opPos = Vec3DInt.from(op[0], op[1], op[2]);
            if (op[3] == 0) {
                p.proposed.put(ChangeProposal.packKey(opPos), new int[] {0, 0});
            } else {
                p.proposed.put(ChangeProposal.packKey(opPos), new int[] {op[3], op[4]});
            }
        }

        bts.extrudePreview = p;
    }
}
