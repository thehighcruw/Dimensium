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

    // CAD camera speed (blocks per scroll step / walk tick).
    public float speed = 0.5f;

    // Walk mode (C): WASD + always-on mouse look. Default (false) = CAD viewport mode.
    public boolean walkMode = false;

    // Software cursor position in scaled pixels.
    public float cursorX = 0, cursorY = 0;

    // Actual perspective projection tangents captured from GL each frame.
    // Defaults match 70° vertical FOV at 4:3 until first frame is rendered.
    public float projTanHX = (float) (Math.tan(Math.toRadians(35)) * 4.0 / 3.0);
    public float projTanHY = (float) Math.tan(Math.toRadians(35));

    // Camera drag state set by InputHandler; read by TickHandler each render frame.
    public boolean lmbDragging = false;
    public boolean rmbDragging = false;
    public boolean mmbDragging = false;
    // True for the one frame after an alt+RMB camera-pan drag ends, so handleRelease
    // can distinguish a paint release from a drag release.
    public boolean rmbWasDragging = false;

    // Orbit state — Ctrl+LMB.
    public boolean orbiting = false;
    public double pivotX, pivotY, pivotZ;
    public double orbitDist;

    private EntityLivingBase savedViewEntity = null;
    public FreecamEntity cameraEntity = null;

    public void activate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (active || mc.thePlayer == null || mc.theWorld == null) return;

        // Reset software cursor to screen centre on every open.
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        cursorX = sr.getScaledWidth() / 2f;
        cursorY = sr.getScaledHeight() / 2f;

        lmbDragging = false;
        rmbDragging = false;
        rmbWasDragging = false;
        mmbDragging = false;

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

            walkMode = false;
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
        walkMode = false;
        lmbDragging = false;
        rmbDragging = false;
        rmbWasDragging = false;
        mmbDragging = false;
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

    public void adjustSpeed(boolean faster) {
        speed = faster ? Math.min(speed * 1.5f, 20.0f) : Math.max(speed / 1.5f, 0.05f);
    }
}
