/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamEntity;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;

@SideOnly(Side.CLIENT)
public final class ViewportRegistry {

    public static final ViewportRegistry INSTANCE = new ViewportRegistry();

    private ViewportRegistry() {}

    public final List<ViewportState> viewports = new ArrayList<>();
    private int activeIndex = 0;

    // Texture IDs queued for deletion on the render thread (may be populated from any thread).
    private final List<Integer> pendingTextureDeletions = new CopyOnWriteArrayList<>();

    public int activeIndex() {
        return activeIndex;
    }

    public ViewportState active() {
        return viewports.isEmpty() ? null : viewports.get(activeIndex);
    }

    /**
     * Initialize with a single viewport using the given camera entity.
     * Called from FreecamState.activate().
     */
    public void init(FreecamEntity initialCamera) {
        clear();
        viewports.add(new ViewportState("Viewport 1", initialCamera));
        activeIndex = 0;
    }

    /**
     * Add a new viewport, cloning the active camera position, and switch to it immediately.
     */
    public ViewportState addViewport() {
        Minecraft mc = Minecraft.getMinecraft();
        FreecamState fs = FreecamState.INSTANCE;

        FreecamEntity newCam = new FreecamEntity(mc.theWorld);
        if (fs.cameraEntity != null) {
            FreecamEntity src = fs.cameraEntity;
            newCam.setPosition(src.posX, src.posY, src.posZ);
            newCam.rotationYaw = src.rotationYaw;
            newCam.rotationPitch = src.rotationPitch;
            newCam.prevRotationYaw = src.rotationYaw;
            newCam.prevRotationPitch = src.rotationPitch;
            newCam.prevPosX = src.posX;
            newCam.prevPosY = src.posY;
            newCam.prevPosZ = src.posZ;
            newCam.lastTickPosX = src.posX;
            newCam.lastTickPosY = src.posY;
            newCam.lastTickPosZ = src.posZ;
        }

        String label = "Viewport " + (viewports.size() + 1);
        ViewportState vp = new ViewportState(label, newCam);
        viewports.add(vp);
        setActive(viewports.size() - 1);
        return vp;
    }

    /** Switch active viewport, saving current FreecamState fields and restoring the target's. */
    public void setActive(int index) {
        if (index < 0 || index >= viewports.size() || index == activeIndex) return;

        FreecamState fs = FreecamState.INSTANCE;

        // Save per-viewport state from FreecamState into the old viewport.
        ViewportState old = viewports.get(activeIndex);
        old.speed = fs.speed;
        old.orbiting = fs.orbiting;
        old.pivotX = fs.pivotX;
        old.pivotY = fs.pivotY;
        old.pivotZ = fs.pivotZ;
        old.orbitDist = fs.orbitDist;

        activeIndex = index;
        ViewportState next = viewports.get(index);

        // Restore per-viewport state into FreecamState.
        fs.speed = next.speed;
        fs.orbiting = next.orbiting;
        fs.pivotX = next.pivotX;
        fs.pivotY = next.pivotY;
        fs.pivotZ = next.pivotZ;
        fs.orbitDist = next.orbitDist;

        // Clear drag state — don't carry over an in-progress drag from the old viewport.
        fs.lmbPressing = false;
        fs.lmbDragging = false;
        fs.rmbPressing = false;
        fs.rmbDragging = false;
        fs.cameraLmbDragActive = false;
        fs.cameraRmbDragActive = false;

        fs.cameraEntity = next.cameraEntity;
        Minecraft.getMinecraft().renderViewEntity = next.cameraEntity;
    }

    /**
     * Remove viewport at index. No-op if only one viewport remains.
     */
    public void removeViewport(int index) {
        if (viewports.size() <= 1 || index < 0 || index >= viewports.size()) return;

        viewports.get(index)
            .destroy();
        viewports.remove(index);

        if (index == activeIndex || activeIndex >= viewports.size()) {
            // Active viewport was removed (or activeIndex is now out of bounds) —
            // switch to the adjacent viewport without trying to save the old state.
            int newActive = Math.min(Math.max(index, 0), viewports.size() - 1);
            activeIndex = newActive;
            FreecamState fs = FreecamState.INSTANCE;
            ViewportState next = viewports.get(newActive);
            fs.speed = next.speed;
            fs.orbiting = next.orbiting;
            fs.pivotX = next.pivotX;
            fs.pivotY = next.pivotY;
            fs.pivotZ = next.pivotZ;
            fs.orbitDist = next.orbitDist;
            fs.lmbPressing = false;
            fs.lmbDragging = false;
            fs.rmbPressing = false;
            fs.rmbDragging = false;
            fs.cameraEntity = next.cameraEntity;
            Minecraft.getMinecraft().renderViewEntity = next.cameraEntity;
        } else if (index < activeIndex) {
            // A non-active viewport before the active one was removed — fix up the index.
            // FreecamState already reflects the correct still-active viewport.
            activeIndex--;
        }
    }

    /**
     * Save current FreecamState fields into the active viewport's snapshot.
     * Called by FreecamState.deactivate() so camera position survives a toggle.
     */
    public void saveActive() {
        if (viewports.isEmpty()) return;
        FreecamState fs = FreecamState.INSTANCE;
        ViewportState vp = viewports.get(activeIndex);
        vp.speed = fs.speed;
        vp.orbiting = false; // don't persist mid-orbit
        vp.pivotX = fs.pivotX;
        vp.pivotY = fs.pivotY;
        vp.pivotZ = fs.pivotZ;
        vp.orbitDist = fs.orbitDist;
    }

    /**
     * Restore active viewport's snapshot into FreecamState and set renderViewEntity.
     * Called by FreecamState.activate() when viewports already exist from a prior session.
     */
    public void restore() {
        if (viewports.isEmpty()) return;
        FreecamState fs = FreecamState.INSTANCE;
        ViewportState vp = viewports.get(activeIndex);
        fs.speed = vp.speed;
        fs.orbiting = false;
        fs.pivotX = vp.pivotX;
        fs.pivotY = vp.pivotY;
        fs.pivotZ = vp.pivotZ;
        fs.orbitDist = vp.orbitDist;
        fs.lmbPressing = false;
        fs.lmbDragging = false;
        fs.rmbPressing = false;
        fs.rmbDragging = false;
        fs.cameraLmbDragActive = false;
        fs.cameraRmbDragActive = false;
        fs.cameraEntity = vp.cameraEntity;
        Minecraft.getMinecraft().renderViewEntity = vp.cameraEntity;
    }

    /**
     * Destroy all viewports and reset. Safe to call from any thread — GL texture deletions
     * are deferred to the render thread via {@link #flushPendingDeletions()}.
     */
    public void clear() {
        for (ViewportState vp : viewports) vp.scheduleDestroy(pendingTextureDeletions);
        viewports.clear();
        activeIndex = 0;
    }

    /** Called from the render thread each frame to delete textures that were queued off-thread. */
    public void flushPendingDeletions() {
        if (pendingTextureDeletions.isEmpty()) return;
        for (Integer texId : pendingTextureDeletions) {
            org.lwjgl.opengl.GL11.glDeleteTextures(texId);
        }
        pendingTextureDeletions.clear();
    }
}
