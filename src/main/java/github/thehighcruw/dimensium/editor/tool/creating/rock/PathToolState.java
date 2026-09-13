/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.rock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.item.ItemStack;

import github.thehighcruw.dimensium.editor.tool.creating.path.PathMath;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class PathToolState implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo {

    public static final PathToolState INSTANCE = new PathToolState();

    public static class PathPoint {

        public int x, y, z;
        public int radius;
        public ItemStack block;

        public PathPoint(int x, int y, int z, int radius, ItemStack block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.block = block;
        }
    }

    public enum CurveType {

        BRESENHAM("Bresenham"),
        DDA("DDA"),
        CATENARY("Catenary"),
        CATMULL_ROM("Catmull-Rom"),
        BEZIER("Bezier");

        public final String label;

        CurveType(String label) {
            this.label = label;
        }
    }

    public enum PathInterp {

        NEAREST("Nearest"),
        LINEAR("Linear"),
        BEZIER("Bezier");

        public final String label;

        PathInterp(String label) {
            this.label = label;
        }
    }

    public final List<PathPoint> points = new ArrayList<>();
    public int selectedIndex = -1;
    public CurveType curveType = CurveType.BRESENHAM;
    public boolean looped = false;
    public float catenarySlack = 0.5f;
    public PathInterp interp = PathInterp.NEAREST;
    public long interpSeed = ThreadLocalRandom.current()
        .nextLong();

    public ChangeProposal preview = null;

    private final TranslationGizmo gizmo = new TranslationGizmo();
    private final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();

    @Override
    public TranslationGizmo getAxisTranslationGizmo() {
        return gizmo;
    }

    @Override
    public PlaneTranslationGizmo getPlaneTranslationGizmo() {
        return planeGizmo;
    }

    private String cachedKey = "";

    public void invalidatePath() {
        cachedKey = "";
        preview = null;
    }

    public boolean hasMultipleBlocks() {
        if (points.size() < 2) return false;
        ItemStack ref = points.get(0).block;
        for (int i = 1; i < points.size(); i++) {
            ItemStack b = points.get(i).block;
            if (ref == null && b != null) return true;
            if (ref != null && b == null) return true;
            if (ref != null) {
                if (net.minecraft.block.Block.getBlockFromItem(ref.getItem())
                    != net.minecraft.block.Block.getBlockFromItem(b.getItem())) return true;
                if (ref.getItemDamage() != b.getItemDamage()) return true;
            }
        }
        return false;
    }

    public PathPoint selectedPoint() {
        if (selectedIndex < 0 || selectedIndex >= points.size()) return null;
        return points.get(selectedIndex);
    }

    public void clear() {
        points.clear();
        selectedIndex = -1;
        gizmo.reset();
        planeGizmo.reset();
        invalidatePath();
    }

    public void rebuildIfNeeded(ItemStack activeBlock) {
        if (points.size() < 2) {
            preview = null;
            cachedKey = "";
            return;
        }
        String key = buildKey(activeBlock);
        if (key.equals(cachedKey) && preview != null) return;
        cachedKey = key;

        List<int[]> blocks = PathMath.computePathBlocks(this, activeBlock);
        if (blocks.isEmpty()) {
            preview = null;
            return;
        }
        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] b : blocks) {
            p.proposed.put(ChangeProposal.packKey(b[0], b[1], b[2]), new int[] { b[3], b[4] });
        }
        preview = p;
    }

    private String buildKey(ItemStack activeBlock) {
        StringBuilder sb = new StringBuilder();
        sb.append(curveType.ordinal())
            .append(',')
            .append(looped)
            .append(',')
            .append(catenarySlack)
            .append(',')
            .append(interp.ordinal())
            .append(',')
            .append(interpSeed)
            .append(',');
        for (PathPoint pt : points) {
            sb.append(pt.x)
                .append(',')
                .append(pt.y)
                .append(',')
                .append(pt.z)
                .append(',')
                .append(pt.radius)
                .append(',');
            if (pt.block != null) {
                sb.append(
                    net.minecraft.block.Block
                        .getIdFromBlock(net.minecraft.block.Block.getBlockFromItem(pt.block.getItem())))
                    .append(',')
                    .append(pt.block.getItemDamage())
                    .append(';');
            } else if (activeBlock != null) {
                sb.append(
                    net.minecraft.block.Block
                        .getIdFromBlock(net.minecraft.block.Block.getBlockFromItem(activeBlock.getItem())))
                    .append(',')
                    .append(activeBlock.getItemDamage())
                    .append(';');
            } else {
                sb.append("null;");
            }
        }
        return sb.toString();
    }
}
