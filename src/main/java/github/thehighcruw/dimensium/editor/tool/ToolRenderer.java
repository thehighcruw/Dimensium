/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.BrushPreviewRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;

/**
 * Per-tool world-space rendering contract. Every tool must register one in
 * {@link ToolRegistry}.
 * Use {@link #NONE} for tools with no in-world preview and {@link #DEFAULT_BRUSH} for standard solid-block brush
 * highlighting.
 */
@SideOnly(Side.CLIENT)
public interface ToolRenderer {

    /** No in-world rendering — renderHover suppresses the default pipeline immediately. */
    ToolRenderer NONE = new ToolRenderer() {

        @Override
        public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
            return false;
        }

        @Override
        public boolean renderHover(MovingObjectPosition mop, double rx, double ry, double rz) {
            return true;
        }
    };

    /** Standard brush preview: solid non-air blocks are affected, no custom hover draw. */
    ToolRenderer DEFAULT_BRUSH = new ToolRenderer() {

        @Override
        public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
            return mc.theWorld.getBlock(wx, wy, wz) != Blocks.air;
        }

        @Override
        public void renderWorldPreview(Minecraft mc, double rx, double ry, double rz) {
            BrushPreviewRenderer.INSTANCE.render(this, mc, rx, ry, rz);
        }
    };

    /** Which world-space voxels should glow in the active-paint preview. */
    boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz);

    /**
     * Optional extra in-world render (ruler lines, elevation circle, etc.).
     * Return true to suppress the standard brush wireframe entirely.
     */
    default boolean renderHover(MovingObjectPosition mop, double rx, double ry, double rz) {
        return false;
    }

    /**
     * Called from {@link SelectionRenderer} once per world-render tick
     * while this tool is active. Default is a no-op. Tools with a brush preview override this to call
     * {@link BrushPreviewRenderer#render(ToolRenderer, net.minecraft.client.Minecraft, double, double, double)}.
     */
    default void renderWorldPreview(Minecraft mc, double rx, double ry, double rz) {}

    /**
     * Per-frame overlay update called from {@link OverlayRenderer} once per render
     * tick while this tool is active. Handles gizmo hover/drag state and 2D screen overlays (e.g. lasso polygon).
     * Default is a no-op — only tools that need per-frame overlay work override this.
     */
    default void renderOverlay(Minecraft mc, int mx, int my, int mx3d, int my3d) {}
}
