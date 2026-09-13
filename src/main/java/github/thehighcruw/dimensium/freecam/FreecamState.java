/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;

import github.thehighcruw.dimensium.render.ViewportRegistry;

public class FreecamState {

    public static final FreecamState INSTANCE = new FreecamState();

    public boolean active = false;

    // Camera zoom speed (blocks per scroll step).
    public float speed = 0.5f;

    // Software cursor position in scaled pixels.
    public float cursorX = 0, cursorY = 0;

    // Actual perspective projection tangents captured from GL each frame.
    // Defaults match 70° vertical FOV at 4:3 until first frame is rendered.
    public float projTanHX = (float) (Math.tan(Math.toRadians(35)) * 4.0 / 3.0);
    public float projTanHY = (float) Math.tan(Math.toRadians(35));

    // Camera drag state set by InputHandler; read by TickHandler each render frame.
    // "pressing" = button held but drag threshold not yet reached.
    // "dragging" = threshold exceeded, camera operation is active.
    public boolean lmbPressing = false;
    public boolean lmbDragging = false;
    public float lmbPressX, lmbPressY;
    public boolean rmbPressing = false;
    public boolean rmbDragging = false;
    public float rmbPressX, rmbPressY;

    // True from CameraMod+LMB/RMB press until the button is physically released.
    // Persists even if the modifier key is released mid-drag.
    public boolean cameraLmbDragActive = false;
    public boolean cameraRmbDragActive = false;

    // Orbit state — CameraMod+LMB or orbit-crosshair keybinding.
    public boolean orbiting = false;
    public double pivotX, pivotY, pivotZ;
    public double orbitDist;
    // Angular offset (degrees) from the camera look direction to the pivot direction at orbit start.
    // Kept constant throughout the orbit so the pivot stays at the same screen position.
    public float pivotOffsetYaw = 0;
    public float pivotOffsetPitch = 0;

    private EntityLivingBase savedViewEntity = null;
    public FreecamEntity cameraEntity = null;

    public void activate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (active || mc.thePlayer == null || mc.theWorld == null) return;

        // Reset software cursor to screen centre on every open.
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        cursorX = sr.getScaledWidth() / 2f;
        cursorY = sr.getScaledHeight() / 2f;

        lmbPressing = false;
        lmbDragging = false;
        rmbPressing = false;
        rmbDragging = false;
        cameraLmbDragActive = false;
        cameraRmbDragActive = false;
        savedViewEntity = mc.renderViewEntity;

        if (!ViewportRegistry.INSTANCE.viewports.isEmpty()) {
            // Viewports persisted from the previous session — restore them as-is.
            ViewportRegistry.INSTANCE.restore();
        } else {
            // First open (or after a world disconnect cleared everything) — create a fresh viewport.
            cameraEntity = new FreecamEntity(mc.theWorld);
            cameraEntity
                .setPosition(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
            cameraEntity.rotationYaw = mc.thePlayer.rotationYaw;
            cameraEntity.rotationPitch = mc.thePlayer.rotationPitch;
            cameraEntity.prevRotationYaw = cameraEntity.rotationYaw;
            cameraEntity.prevRotationPitch = cameraEntity.rotationPitch;
            cameraEntity.prevPosX = cameraEntity.posX;
            cameraEntity.prevPosY = cameraEntity.posY;
            cameraEntity.prevPosZ = cameraEntity.posZ;
            cameraEntity.lastTickPosX = cameraEntity.posX;
            cameraEntity.lastTickPosY = cameraEntity.posY;
            cameraEntity.lastTickPosZ = cameraEntity.posZ;

            orbiting = false;

            mc.renderViewEntity = cameraEntity;
            ViewportRegistry.INSTANCE.init(cameraEntity);
        }

        active = true;
    }

    public void deactivate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!active) return;

        // Save current camera control state into the active viewport so it survives the toggle.
        ViewportRegistry.INSTANCE.saveActive();

        if (savedViewEntity != null) mc.renderViewEntity = savedViewEntity;
        savedViewEntity = null;
        cameraEntity = null;
        // Do NOT clear ViewportRegistry — viewports persist across toggles.
        // ViewportRegistry.INSTANCE.clear() is called on world disconnect (DimensiumMode.fullReset).
        lmbPressing = false;
        lmbDragging = false;
        rmbPressing = false;
        rmbDragging = false;
        cameraLmbDragActive = false;
        cameraRmbDragActive = false;
        orbiting = false;
        active = false;
    }

    /** Zoom: move along look vector. Used by scroll in CAD mode. */
    public void zoom(float amount) {
        if (cameraEntity == null) return;
        FreecamEntity cam = cameraEntity;
        double yaw = Math.toRadians(cam.rotationYaw);
        double pitch = Math.toRadians(cam.rotationPitch);
        cam.posX += -Math.sin(yaw) * Math.cos(pitch) * amount;
        cam.posY += -Math.sin(pitch) * amount;
        cam.posZ += Math.cos(yaw) * Math.cos(pitch) * amount;
    }

    public boolean isMoving() {
        return orbiting || lmbDragging || rmbDragging || cameraLmbDragActive || cameraRmbDragActive;
    }
}
