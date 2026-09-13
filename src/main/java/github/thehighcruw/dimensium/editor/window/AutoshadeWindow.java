/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class AutoshadeWindow extends ToggleableWindow {

    public static final AutoshadeWindow INSTANCE = new AutoshadeWindow();

    private static final String WINDOW_ID = "###autoshade_window";

    private static final int LIGHT_MODE_PLAYER = 0;
    private static final int LIGHT_MODE_SUN = 1;

    private boolean useSun = true;
    private final ImInt lightMode = new ImInt(LIGHT_MODE_PLAYER);
    private final float[] sunYaw = { 45f };
    private final float[] sunElevation = { 45f };
    private final float[] aoStrength = { 0.8f };
    private final float[] giStrength = { 0.2f };
    private boolean useDither = false;

    private final List<ItemStack> palette = new ArrayList<>();
    private final List<float[]> weights = new ArrayList<>();

    private final Map<String, List<ItemStack>> presets = new HashMap<>();
    private final ImString presetName = new ImString(64);

    private AutoshadeWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowAutoshadeOpen(value);
    }

    public void open() {
        setOpen(true);
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui
            .begin(I18n.format("dimensium.op.autoshade.title") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();
        if (visible && pOpen.get()) {
            if (!SelectionState.INSTANCE.hasSelection()) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.end();
                if (!pOpen.get()) setOpen(false);
                return;
            }

            // Sun checkbox
            ImBoolean cbSun = new ImBoolean(useSun);
            if (ImGui.checkbox(I18n.format("dimensium.op.autoshade.sun") + "##as_sun", cbSun)) useSun = cbSun.get();

            if (useSun) {
                float comboW = 160f * scale;
                ImGui.text(I18n.format("dimensium.op.autoshade.light_from") + ":");
                ImGui.sameLine();
                ImGui.setNextItemWidth(comboW);
                String[] lightModes = { I18n.format("dimensium.op.autoshade.player_pos"),
                    I18n.format("dimensium.op.autoshade.sun_angle") };
                ImGui.combo("##as_lightmode", lightMode, lightModes, lightModes.length);

                if (lightMode.get() == LIGHT_MODE_SUN) {
                    ImGui.setNextItemWidth(comboW);
                    ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_x") + "##as_yaw", sunYaw, 0f, 360f);
                    ImGui.setNextItemWidth(comboW);
                    ImGui.sliderFloat(
                        I18n.format("dimensium.ui.distort.distance_y") + "##as_elev",
                        sunElevation,
                        0f,
                        90f);
                }
            }

            float sliderW = 160f * scale;
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(I18n.format("dimensium.op.autoshade.ao") + "##as_ao", aoStrength, 0f, 1f);
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(I18n.format("dimensium.op.autoshade.gi") + "##as_gi", giStrength, 0f, 1f);

            ImBoolean cbDither = new ImBoolean(useDither);
            if (ImGui.checkbox(I18n.format("dimensium.op.autoshade.dither") + "##as_dither", cbDither))
                useDither = cbDither.get();

            ImGui.separator();
            ImGui.text(I18n.format("dimensium.op.autoshade.palette"));

            float cellSize = 20f * scale;
            for (int i = 0; i < palette.size(); i++) {
                DeferredItemRender.placeButton("##as_pal_" + i, palette.get(i), cellSize * 0.5f, false);
                ImGui.sameLine();
                ImGui.setNextItemWidth(120f * scale);
                ImGui.sliderFloat("##as_w_" + i, weights.get(i), 0f, 1f);
                ImGui.sameLine();
                if (ImGui.button("X##as_rem_" + i)) {
                    palette.remove(i);
                    weights.remove(i);
                    break;
                }
            }
            if (ImGui.button(I18n.format("dimensium.op.autoshade.add_block") + "##as_add")) {
                OverlayRenderer.picker.open(stack -> {
                    palette.add(stack);
                    weights.add(new float[] { 1f });
                });
            }

            ImGui.separator();
            ImGui.text(I18n.format("dimensium.op.autoshade.presets"));

            ImGui.setNextItemWidth(120f * scale);
            ImGui.inputText("##as_preset_name", presetName);
            ImGui.sameLine();
            if (ImGui.button(I18n.format("dimensium.op.autoshade.save_preset") + "##as_save")) {
                String name = presetName.get()
                    .trim();
                if (!name.isEmpty()) {
                    presets.put(name, new ArrayList<>(palette));
                }
            }

            for (Map.Entry<String, List<ItemStack>> entry : new ArrayList<>(presets.entrySet())) {
                ImGui.text(entry.getKey());
                ImGui.sameLine();
                if (ImGui.button("Load##as_load_" + entry.getKey())) {
                    palette.clear();
                    weights.clear();
                    for (ItemStack s : entry.getValue()) {
                        palette.add(s);
                        weights.add(new float[] { 1f });
                    }
                }
                ImGui.sameLine();
                if (ImGui.button("Del##as_del_" + entry.getKey())) {
                    presets.remove(entry.getKey());
                    break;
                }
            }

            ImGui.separator();

            boolean canApply = !palette.isEmpty() && SelectionState.INSTANCE.hasSelection();
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.autoshade.do") + "##as_apply")) {
                applyAutoshade();
            }
            if (!canApply) ImGui.endDisabled();
        }
        ImGui.end();
        if (!pOpen.get()) setOpen(false);
    }

    private void applyAutoshade() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || palette.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        Set<Long> selected = sel.getSelectedBlocks();

        // Build sun direction
        double sunDirX, sunDirY, sunDirZ;
        if (useSun && lightMode.get() == LIGHT_MODE_SUN) {
            double yawRad = Math.toRadians(sunYaw[0]);
            double elevRad = Math.toRadians(sunElevation[0]);
            sunDirX = Math.cos(elevRad) * Math.sin(yawRad);
            sunDirY = Math.sin(elevRad);
            sunDirZ = Math.cos(elevRad) * Math.cos(yawRad);
        } else if (useSun) {
            EntityPlayer p = mc.thePlayer;
            if (p == null) return;
            double yawRad = Math.toRadians(p.rotationYaw);
            double elevRad = Math.toRadians(-p.rotationPitch);
            sunDirX = Math.cos(elevRad) * -Math.sin(yawRad);
            sunDirY = Math.sin(elevRad);
            sunDirZ = Math.cos(elevRad) * Math.cos(yawRad);
        } else {
            sunDirX = 0;
            sunDirY = 1;
            sunDirZ = 0;
        }

        // Build cumulative weights for palette mapping
        float totalWeight = 0f;
        for (float[] w : weights) totalWeight += w[0];
        if (totalWeight <= 0f) return;

        float[] cumulative = new float[weights.size()];
        float running = 0f;
        for (int i = 0; i < weights.size(); i++) {
            running += weights.get(i)[0] / totalWeight;
            cumulative[i] = running;
        }

        int[][] NEIGHBORS_26;
        {
            List<int[]> nb = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                nb.add(new int[] { dx, dy, dz });
            }
            NEIGHBORS_26 = nb.toArray(new int[0][]);
        }

        int[][] FACE_DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

        List<int[]> ops = new ArrayList<>();

        for (long key : selected) {
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            Block worldBlock = mc.theWorld.getBlock(x, y, z);
            if (worldBlock == null || worldBlock == Blocks.air) continue;

            // Compute surface normal from empty face-neighbors
            double nx = 0, ny = 0, nz = 0;
            for (int[] f : FACE_DIRS) {
                long neighborKey = SelectionState.pack(x + f[0], y + f[1], z + f[2]);
                if (!selected.contains(neighborKey)) {
                    nx += f[0];
                    ny += f[1];
                    nz += f[2];
                }
            }
            double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nLen > 0) {
                nx /= nLen;
                ny /= nLen;
                nz /= nLen;
            }

            // Diffuse lighting
            double diffuse = Math.max(0, nx * sunDirX + ny * sunDirY + nz * sunDirZ) * aoStrength[0];

            // Ambient occlusion: fraction of 26 neighbors that are empty in selection
            int emptyNeighbors = 0;
            for (int[] nb : NEIGHBORS_26) {
                if (!selected.contains(SelectionState.pack(x + nb[0], y + nb[1], z + nb[2]))) {
                    emptyNeighbors++;
                }
            }
            double ao = (double) emptyNeighbors / NEIGHBORS_26.length;

            double shade = diffuse + (1.0 - diffuse) * giStrength[0] * ao;
            shade = Math.max(0.0, Math.min(1.0, shade));

            // Map shade to palette block
            int paletteIdx = palette.size() - 1;
            for (int i = 0; i < cumulative.length; i++) {
                if (shade <= cumulative[i]) {
                    paletteIdx = i;
                    break;
                }
            }

            ItemStack chosenStack = palette.get(paletteIdx);
            Block chosenBlock = Block.getBlockFromItem(chosenStack.getItem());
            if (chosenBlock == null) continue;
            ops.add(new int[] { x, y, z, Block.getIdFromBlock(chosenBlock), chosenStack.getItemDamage() });
        }

        BlockSender.sendChunked(ops, I18n.format("dimensium.op.autoshade.do"));
    }
}
