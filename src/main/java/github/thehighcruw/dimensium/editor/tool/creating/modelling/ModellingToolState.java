/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;

import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class ModellingToolState implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo {

    public static final ModellingToolState INSTANCE = new ModellingToolState();

    public enum Mode {

        CONVEX_HULL("dimensium.modelling.mode.convex_hull"),
        TRIANGLE_STRIP("dimensium.modelling.mode.triangle_strip"),
        TRIANGLE_FAN("dimensium.modelling.mode.triangle_fan"),
        FLAT("dimensium.modelling.mode.flat"),
        CATMULL_ROM("dimensium.modelling.mode.catmull_rom"),
        BEZIER("dimensium.modelling.mode.bezier"),
        SMART_SURFACE("dimensium.modelling.mode.smart_surface");

        public final String label;

        Mode(String label) {
            this.label = label;
        }

        public boolean usesRows() {
            return this == FLAT || this == CATMULL_ROM || this == BEZIER;
        }
    }

    public enum PasteMode {

        PASTE_COPY("dimensium.modelling.paste.paste_copy"),
        KEEP_EXISTING("dimensium.modelling.paste.keep_existing");

        public final String label;

        PasteMode(String label) {
            this.label = label;
        }
    }

    @Desugar
    public record ModelPoint(Vec3DInt pos) {}

    public Mode mode = Mode.CONVEX_HULL;
    public PasteMode pasteMode = PasteMode.PASTE_COPY;
    public boolean offsetTargetPoint = true;

    public final List<List<ModelPoint>> rows = new ArrayList<>();
    public int currentRowIndex = 0;

    public int selectedRow = -1;
    public int selectedPoint = -1;

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

    public ChangeProposal preview = null;
    private String cachedKey = "";

    public void ensureRow() {
        if (rows.isEmpty()) {
            rows.add(new ArrayList<>());
            currentRowIndex = 0;
        }
    }

    public void addPoint(Vec3DInt pos) {
        ensureRow();
        rows.get(currentRowIndex)
            .add(new ModelPoint(pos));
        invalidate();
    }

    public void addRow() {
        rows.add(new ArrayList<>());
        currentRowIndex = rows.size() - 1;
        invalidate();
    }

    public List<ModelPoint> allPoints() {
        List<ModelPoint> all = new ArrayList<>();
        for (List<ModelPoint> row : rows) all.addAll(row);
        return all;
    }

    public void removeSelectedPoint() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return;
        List<ModelPoint> row = rows.get(selectedRow);
        if (selectedPoint < 0 || selectedPoint >= row.size()) return;
        row.remove(selectedPoint);
        if (row.isEmpty()) {
            rows.remove(selectedRow);
            if (rows.isEmpty()) {
                selectedRow = -1;
                currentRowIndex = 0;
            } else {
                selectedRow = Math.min(selectedRow, rows.size() - 1);
                currentRowIndex = selectedRow;
            }
        }
        selectedPoint = -1;
        invalidate();
    }

    public ModelPoint selectedPointObj() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return null;
        List<ModelPoint> row = rows.get(selectedRow);
        if (selectedPoint < 0 || selectedPoint >= row.size()) return null;
        return row.get(selectedPoint);
    }

    public void invalidate() {
        cachedKey = "";
        preview = null;
    }

    public void clear() {
        rows.clear();
        currentRowIndex = 0;
        selectedRow = -1;
        selectedPoint = -1;
        gizmo.reset();
        planeGizmo.reset();
        invalidate();
    }

    public void rebuildIfNeeded(ItemStack activeBlock) {
        String key = buildKey(activeBlock);
        if (key.equals(cachedKey) && preview != null) return;
        cachedKey = key;

        List<int[]> blocks = ModellingMath.computeBlocks(this, activeBlock);
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
        sb.append(mode.ordinal())
            .append(',')
            .append(pasteMode.ordinal())
            .append(',')
            .append(offsetTargetPoint)
            .append(',');
        if (activeBlock != null) {
            sb.append(
                net.minecraft.block.Block
                    .getIdFromBlock(net.minecraft.block.Block.getBlockFromItem(activeBlock.getItem())))
                .append(',')
                .append(activeBlock.getItemDamage());
        }
        sb.append('|');
        for (List<ModelPoint> row : rows) {
            sb.append('R');
            for (ModelPoint p : row) {
                sb.append(
                    p.pos()
                        .x())
                    .append(',')
                    .append(
                        p.pos()
                            .y())
                    .append(',')
                    .append(
                        p.pos()
                            .z())
                    .append(';');
            }
        }
        return sb.toString();
    }
}
