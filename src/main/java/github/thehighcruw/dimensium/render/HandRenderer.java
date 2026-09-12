/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Suppresses the first-person arm/item render while the Dimensium editor is active.
 *
 * EntityRenderer.itemRenderer is public final — direct reassignment is illegal.
 * EntityRenderer.renderHand() has no Forge hook in 1.7.10.
 * Java 8 allows bypassing final via reflection (Field.modifiers trick).
 *
 * Usage:
 * HandRenderer.suppress(mc) — called in RenderTickEvent.START, before renderHand
 * HandRenderer.restore(mc) — called in RenderGameOverlayEvent.ALL, after renderHand
 */
@SideOnly(Side.CLIENT)
public final class HandRenderer {

    public static final HandRenderer INSTANCE = new HandRenderer();

    private HandRenderer() {}

    private Field itemRendererField = null;
    private ItemRenderer saved = null;
    private ItemRenderer noOp = null;

    private Field getField() {
        if (itemRendererField != null) return itemRendererField;
        try {
            Field f = EntityRenderer.class.getDeclaredField("itemRenderer");
            f.setAccessible(true);
            // Strip final so the field can be written via reflection (Java 8 only).
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            itemRendererField = f;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If reflection fails, suppress silently — arm will still render but won't crash.
        }
        return itemRendererField;
    }

    /** Replace entityRenderer.itemRenderer with a no-op. Call before renderHand fires. */
    public void suppress(Minecraft mc) {
        if (saved != null) return; // already suppressed this frame
        Field f = getField();
        if (f == null) return;
        try {
            if (noOp == null) noOp = new NoOpItemRenderer(mc);
            saved = (ItemRenderer) f.get(mc.entityRenderer);
            f.set(mc.entityRenderer, noOp);
        } catch (IllegalAccessException ignored) {}
    }

    /** Restore the original itemRenderer. Call after renderHand, before gameplay uses it. */
    public void restore(Minecraft mc) {
        if (saved == null) return;
        Field f = getField();
        if (f == null) {
            saved = null;
            return;
        }
        try {
            f.set(mc.entityRenderer, saved);
        } catch (IllegalAccessException ignored) {}
        saved = null;
    }

    private static final class NoOpItemRenderer extends ItemRenderer {

        NoOpItemRenderer(Minecraft mc) {
            super(mc);
        }

        @Override
        public void renderItemInFirstPerson(float partialTicks) {
            // Arm and held item suppressed: Dimensium editor viewport is active.
        }
    }
}
