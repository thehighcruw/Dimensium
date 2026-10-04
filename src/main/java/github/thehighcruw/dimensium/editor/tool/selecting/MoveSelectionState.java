/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ViewPlaneGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/**
 * State for the "Move Selection" operation — translates the selection block-set without moving
 * the underlying blocks.
 *
 * <p>
 * Activation stores the original min/max and exposes gizmos:
 * <ul>
 * <li>Center gizmos — always present; translate the whole selection uniformly.</li>
 * <li>Min/max gizmos — only when the selection is a full AABB; allow per-corner adjustments.</li>
 * </ul>
 *
 * <p>
 * Dragging any gizmo updates {@code proposedMin}/{@code proposedMax} live. Committing applies
 * the proposed bounds to {@link SelectionState} without touching world blocks.
 */
@SideOnly(Side.CLIENT)
public class MoveSelectionState {

    public static final MoveSelectionState INSTANCE = new MoveSelectionState();

    public boolean active = false;
    /** True when the selection was a full AABB at activation — enables per-corner gizmos. */
    public boolean isCuboid = false;

    public long capturedSelVersion = -1;

    /** Bounds captured at activation. */
    public Vec3DInt originalMin = Vec3DInt.ZERO;

    public Vec3DInt originalMax = Vec3DInt.ZERO;

    /** Live-updated proposed bounds driven by gizmo drags. */
    public Vec3DInt proposedMin = Vec3DInt.ZERO;

    public Vec3DInt proposedMax = Vec3DInt.ZERO;

    /** Corner positions at the start of a center-gizmo drag. */
    public Vec3DInt centerDragBaseMin = Vec3DInt.ZERO;

    public Vec3DInt centerDragBaseMax = Vec3DInt.ZERO;

    // ── Gizmos ────────────────────────────────────────────────────────────────

    public final ViewPlaneGizmo centerViewPlane = new ViewPlaneGizmo();
    public final TranslationGizmo centerAxis = new TranslationGizmo();
    public final PlaneTranslationGizmo centerPlane = new PlaneTranslationGizmo();

    /** Gizmo at the min corner — only rendered/interacted when {@code isCuboid}. */
    public final TranslationGizmo minAxis = new TranslationGizmo();

    public final PlaneTranslationGizmo minPlane = new PlaneTranslationGizmo();

    /** Gizmo at the max corner — only rendered/interacted when {@code isCuboid}. */
    public final TranslationGizmo maxAxis = new TranslationGizmo();

    public final PlaneTranslationGizmo maxPlane = new PlaneTranslationGizmo();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void activate(SelectionState sel) {
        if (!sel.hasSelection()) return;
        originalMin = sel.min();
        originalMax = sel.max();
        proposedMin = originalMin;
        proposedMax = originalMax;
        isCuboid = sel.size() == (long) sel.width() * sel.height() * sel.depth();
        capturedSelVersion = sel.renderVersion;
        resetGizmos();
        active = true;
    }

    public void cancel() {
        active = false;
        resetGizmos();
    }

    // ── Gizmo positions ───────────────────────────────────────────────────────

    public Vec3DDouble centerWorldPos() {
        return proposedMin.toDouble().plus(proposedMax.toDouble()).plus(1.0).divide(2.0);
    }

    public Vec3DDouble minWorldPos() {
        return proposedMin.toDouble().plus(0.5);
    }

    public Vec3DDouble maxWorldPos() {
        return proposedMax.toDouble().plus(0.5);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isAnyGizmoDragging() {
        return centerViewPlane.isDragging()
                || centerAxis.isDragging()
                || centerPlane.isDragging()
                || minAxis.isDragging()
                || minPlane.isDragging()
                || maxAxis.isDragging()
                || maxPlane.isDragging();
    }

    /**
     * Apply an integer translation to proposedMin/Max, derived from a center-gizmo anchor update.
     * Clamps Y to [0, 255].
     */
    public void applyCenterDrag(Vec3DDouble anchor, boolean snap) {
        Vec3DDouble centerBase = centerDragBaseMin
                .toDouble()
                .plus(centerDragBaseMax.toDouble())
                .plus(1.0)
                .divide(2.0);
        Vec3DDouble delta = anchor.minus(centerBase);
        Vec3DInt intDelta = snap ? Vec3DInt.round(delta) : Vec3DInt.floor(delta);
        proposedMin = centerDragBaseMin.plus(intDelta);
        proposedMax = centerDragBaseMax.plus(intDelta);
    }

    public void applyMinDrag(Vec3DDouble anchor, boolean snap) {
        Vec3DInt newMin = snap ? Vec3DInt.round(anchor) : Vec3DInt.floor(anchor);
        proposedMin = newMin;
    }

    public void applyMaxDrag(Vec3DDouble anchor, boolean snap) {
        Vec3DInt newMax = snap ? Vec3DInt.round(anchor) : Vec3DInt.floor(anchor);
        proposedMax = newMax;
    }

    /** Axis-flip hints so corner gizmos point outward from the selection. */
    public void configureMinGizmoFlips() {
        minAxis.axisFlip[0] = proposedMin.x() <= proposedMax.x() ? -1f : 1f;
        minAxis.axisFlip[1] = proposedMin.y() <= proposedMax.y() ? -1f : 1f;
        minAxis.axisFlip[2] = proposedMin.z() <= proposedMax.z() ? -1f : 1f;
        System.arraycopy(minAxis.axisFlip, 0, minPlane.axisFlip, 0, 3);
    }

    public void configureMaxGizmoFlips() {
        maxAxis.axisFlip[0] = proposedMax.x() >= proposedMin.x() ? 1f : -1f;
        maxAxis.axisFlip[1] = proposedMax.y() >= proposedMin.y() ? 1f : -1f;
        maxAxis.axisFlip[2] = proposedMax.z() >= proposedMin.z() ? 1f : -1f;
        System.arraycopy(maxAxis.axisFlip, 0, maxPlane.axisFlip, 0, 3);
    }

    private void resetGizmos() {
        centerViewPlane.reset();
        centerAxis.reset();
        centerPlane.reset();
        minAxis.reset();
        minPlane.reset();
        maxAxis.reset();
        maxPlane.reset();
    }
}
