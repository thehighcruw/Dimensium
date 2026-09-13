/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface BrushInput {

    /**
     * Handle a mouse button press.
     * 
     * @return true to consume the event (cancels default painting).
     */
    default boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        return false;
    }

    /**
     * Called each render tick while {@code button} is held, after block-position dedup.
     * Only invoked when the tool has a registered BrushInput and a block is under the cursor.
     */
    default void onMouseHeld(int button, MovingObjectPosition mop) {}

    /** Whether this tool uses the RMB drag-paint loop in TickHandler. */
    default boolean usesDragLoop() {
        return false;
    }

    /**
     * Called each render tick inside TickHandler's drag loop, before standard processing.
     * Return true to signal the tool fully handled this tick (skips standard loop).
     * Used for screen-space drags (LASSO_SELECT) or tools that bypass block-dedup (ELEVATION).
     */
    default boolean onDragTick(Minecraft mc, int sw, int sh) {
        return false;
    }

    /**
     * Called on the first new-block position of a drag, immediately after ChangeProposal.startDrag().
     */
    default void onBrushDragStart(Minecraft mc, MovingObjectPosition mop) {}

    /**
     * Called per new block position during RMB drag (after block-position dedup).
     * Return true if the tool handled it; false to fall back to BrushApplicator.applyTool.
     */
    default boolean onBrushHeld(Minecraft mc, MovingObjectPosition mop) {
        return false;
    }

    /**
     * Called when RMB is released after a drag.
     */
    default void onBrushRelease(Minecraft mc) {}

    /**
     * Called each render tick to update gizmo drag state and write results to tool state.
     * Runs before the paint loop so state is fresh for world renderers.
     */
    default void onGizmoDrag(int mx, int my, boolean snap) {}
}
