/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory.gradientHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.world.inventory.CreativeGuiUtils;
import github.thehighcruw.dimensium.world.inventory.GuiToggleButton;

@SideOnly(Side.CLIENT)
public class GuiGradientHelper extends GuiContainer {

    private static final int PANEL_W = GradientHelperContainer.PANEL_W;
    private static final int PANEL_H = GradientHelperContainer.PANEL_H;
    private static final int SLOT_SIZE = GradientHelperContainer.SLOT_SIZE;
    private static final int INPUT_SLOTS = GradientHelperContainer.INPUT_SLOTS;
    private static final int OUTPUT_SLOTS = GradientHelperContainer.OUTPUT_SLOTS;
    private static final int CONTENT_X = GradientHelperContainer.CONTENT_X;
    private static final int INPUT_Y = GradientHelperContainer.INPUT_Y;
    private static final int OUTPUT_Y = GradientHelperContainer.OUTPUT_Y;
    private static final int HOTBAR_Y = GradientHelperContainer.HOTBAR_Y;

    private static final int C_PANEL = 0xFFC6C6C6;
    private static final int C_PANEL_HI = 0xFFFFFFFF;
    private static final int C_PANEL_SH = 0xFF555555;
    private static final int C_SLOT = 0xFF8B8B8B;
    private static final int C_SLOT_HI = 0xFFFFFFFF;
    private static final int C_SLOT_SH = 0xFF373737;
    private static final int C_TEXT = 0xFF404040;
    private static final int C_LABEL = 0xFF707070;

    private static final int BTN_F = 20;
    private static final int BTN_S = 21;
    private static final int BTN_O = 22;
    private static final int BTN_T = 23;
    private static final int BTN_COPY_HOTBAR = 24;
    private static final int BTN_CYCLE_PREV_BASE = 30;
    private static final int BTN_CYCLE_NEXT_BASE = 40;

    private static final int CYCLE_POOL = 16;

    private boolean filterFullCube, filterSolid, filterOpaque, filterSameTexture;
    private boolean dirty = true;

    @SuppressWarnings("unchecked")
    private final List<ItemStack>[] candidates = new List[OUTPUT_SLOTS];
    private final int[] cycleIndex = new int[OUTPUT_SLOTS];
    private final ItemStack[] lastInputSnapshot = new ItemStack[INPUT_SLOTS];

    public GuiGradientHelper(EntityPlayer player) {
        super(new GradientHelperContainer(player.inventory));
        this.xSize = PANEL_W;
        this.ySize = PANEL_H;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            candidates[i] = new ArrayList<>();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        // FSOT — top-right, aligned with title row
        CreativeGuiUtils.addFsotButtons(buttonList, guiLeft, guiTop, PANEL_W);

        // Copy to Hotbar — centered between output row and hotbar
        int copyW = 80;
        int copyX = guiLeft + (PANEL_W - copyW) / 2;
        int copyY = guiTop + OUTPUT_Y + SLOT_SIZE + 12;
        buttonList.add(new GuiButton(BTN_COPY_HOTBAR, copyX, copyY, copyW, 12, "Copy to Hotbar"));

        // Cycle buttons — < > below each output slot
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            int slotX = guiLeft + CONTENT_X + i * SLOT_SIZE;
            int cycleY = guiTop + OUTPUT_Y + SLOT_SIZE + 1;
            buttonList.add(new GuiButton(BTN_CYCLE_PREV_BASE + i, slotX, cycleY, 8, 8, "<"));
            buttonList.add(new GuiButton(BTN_CYCLE_NEXT_BASE + i, slotX + 9, cycleY, 8, 8, ">"));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        checkInputChanged();
        if (dirty && BlockColorCache.INSTANCE.isInitialized()) {
            recompute();
            dirty = false;
        }

        drawMcPanel(guiLeft, guiTop);

        fontRendererObj.drawString("Gradient Helper", guiLeft + 6, guiTop + 6, C_TEXT);
        fontRendererObj.drawString("Inputs", guiLeft + CONTENT_X, guiTop + INPUT_Y - 9, C_LABEL);
        fontRendererObj.drawString("Outputs", guiLeft + CONTENT_X, guiTop + OUTPUT_Y - 9, C_LABEL);

        // Arrow between rows
        fontRendererObj.drawString("v", guiLeft + PANEL_W / 2 - 2, guiTop + INPUT_Y + SLOT_SIZE + 1, C_LABEL);

        for (int i = 0; i < INPUT_SLOTS; i++) {
            drawMcSlot(guiLeft + CONTENT_X + i * SLOT_SIZE, guiTop + INPUT_Y);
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            drawMcSlot(guiLeft + CONTENT_X + i * SLOT_SIZE, guiTop + OUTPUT_Y);
        }

        // Separator above hotbar
        int sepY = guiTop + HOTBAR_Y - 5;
        drawRect(guiLeft + 4, sepY, guiLeft + PANEL_W - 4, sepY + 1, C_PANEL_SH);
        drawRect(guiLeft + 4, sepY + 1, guiLeft + PANEL_W - 4, sepY + 2, C_PANEL_HI);

        int hotbarStartX = guiLeft + (PANEL_W - 9 * 18) / 2;
        for (int i = 0; i < 9; i++) {
            drawMcSlot(hotbarStartX + i * 18, guiTop + HOTBAR_Y);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawFsotTooltip(mouseX, mouseY);
    }

    private void drawFsotTooltip(int mouseX, int mouseY) {
        for (GuiButton btn : buttonList) {
            if (mouseX >= btn.xPosition && mouseX < btn.xPosition + btn.width
                && mouseY >= btn.yPosition
                && mouseY < btn.yPosition + btn.height) {
                String key = fsotTooltipKey(btn.id);
                if (key != null) {
                    drawHoveringText(Collections.singletonList(I18n.format(key)), mouseX, mouseY, fontRendererObj);
                }
                return;
            }
        }
    }

    private String fsotTooltipKey(int id) {
        return switch (id) {
            case BTN_F -> "dimensium.colour_picker.filter.full_cube";
            case BTN_S -> "dimensium.colour_picker.filter.solid";
            case BTN_O -> "dimensium.colour_picker.filter.opaque";
            case BTN_T -> "dimensium.colour_picker.filter.same_texture";
            default -> null;
        };
    }

    // ── Gradient computation ───────────────────────────────────────────────────

    private void recompute() {
        GradientHelperContainer container = gradientContainer();

        List<int[]> anchors = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = container.getInputInv()
                .getStackInSlot(i);
            if (stack == null || stack.getItem() == null) continue;
            int blockId = Block.getIdFromBlock(Block.getBlockFromItem(stack.getItem()));
            int meta = stack.getItemDamage();
            int rgb = BlockColorCache.INSTANCE.blockColor(blockId, meta);
            if (rgb < 0) continue;
            anchors.add(new int[] { i, rgb });
        }

        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            candidates[i] = new ArrayList<>();
            cycleIndex[i] = 0;
        }

        if (anchors.size() < 2) {
            if (anchors.size() == 1) {
                int rgb = anchors.get(0)[1];
                List<ItemStack> matches = BlockColorCache.INSTANCE
                    .findSimilarBlocks(rgb, filterFullCube, filterSolid, filterOpaque, filterSameTexture, CYCLE_POOL);
                for (int i = 0; i < OUTPUT_SLOTS; i++) {
                    candidates[i] = new ArrayList<>(matches);
                }
            }
            updateOutputSlots(container);
            return;
        }

        for (int j = 0; j < OUTPUT_SLOTS; j++) {
            double t = (double) j / (OUTPUT_SLOTS - 1);
            int[] left = null, right = null;
            for (int a = 0; a < anchors.size() - 1; a++) {
                int[] lo = anchors.get(a);
                int[] hi = anchors.get(a + 1);
                double tlo = (double) lo[0] / (INPUT_SLOTS - 1);
                double thi = (double) hi[0] / (INPUT_SLOTS - 1);
                if (t >= tlo && t <= thi) {
                    left = lo;
                    right = hi;
                    break;
                }
            }
            if (left == null) {
                left = right = t < (double) anchors.get(0)[0] / (INPUT_SLOTS - 1) ? anchors.get(0)
                    : anchors.get(anchors.size() - 1);
            }

            double tLocal = left == right ? 0.5
                : (t - (double) left[0] / (INPUT_SLOTS - 1))
                    / ((double) right[0] / (INPUT_SLOTS - 1) - (double) left[0] / (INPUT_SLOTS - 1));
            int rgb = interpolateLab(left[1], right[1], tLocal);

            candidates[j] = BlockColorCache.INSTANCE
                .findSimilarBlocks(rgb, filterFullCube, filterSolid, filterOpaque, filterSameTexture, CYCLE_POOL);
        }

        updateOutputSlots(container);
    }

    private void updateOutputSlots(GradientHelperContainer container) {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            List<ItemStack> list = candidates[i];
            int idx = cycleIndex[i];
            ItemStack pick = (list != null && !list.isEmpty()) ? list.get(idx % list.size()) : null;
            container.getOutputInv()
                .setInventorySlotContents(i, pick);
        }
    }

    private static int interpolateLab(int rgbA, int rgbB, double t) {
        double[] labA = BlockColorCache.rgbToLab(rgbA);
        double[] labB = BlockColorCache.rgbToLab(rgbB);
        return labToRgb(
            labA[0] + (labB[0] - labA[0]) * t,
            labA[1] + (labB[1] - labA[1]) * t,
            labA[2] + (labB[2] - labA[2]) * t);
    }

    private static int labToRgb(double l, double a, double b) {
        double fy = (l + 16.0) / 116.0;
        double fx = a / 500.0 + fy;
        double fz = fy - b / 200.0;
        double x = labFInv(fx) * 0.95047;
        double y = labFInv(fy);
        double z = labFInv(fz) * 1.08883;
        double r = delinearize(3.2404542 * x - 1.5371385 * y - 0.4985314 * z);
        double g = delinearize(-0.9692660 * x + 1.8760108 * y + 0.0415560 * z);
        double bv = delinearize(0.0556434 * x - 0.2040259 * y + 1.0572252 * z);
        return (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(bv);
    }

    private static double labFInv(double t) {
        return t > 0.20689655 ? t * t * t : (t - 16.0 / 116.0) / 7.787;
    }

    private static double delinearize(double c) {
        c = Math.max(0.0, Math.min(1.0, c));
        return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
    }

    private static int clampByte(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v * 255.0)));
    }

    // ── Input change detection ─────────────────────────────────────────────────

    private void checkInputChanged() {
        GradientHelperContainer container = gradientContainer();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack current = container.getInputInv()
                .getStackInSlot(i);
            ItemStack last = lastInputSnapshot[i];
            if (!ItemStack.areItemStacksEqual(current, last)) {
                lastInputSnapshot[i] = current == null ? null : current.copy();
                dirty = true;
            }
        }
    }

    // ── Button actions ─────────────────────────────────────────────────────────

    @Override
    protected void actionPerformed(GuiButton button) {
        GradientHelperContainer container = gradientContainer();
        int id = button.id;

        if (id == BTN_F) {
            filterFullCube = toggle(button, filterFullCube);
            dirty = true;
            return;
        }
        if (id == BTN_S) {
            filterSolid = toggle(button, filterSolid);
            dirty = true;
            return;
        }
        if (id == BTN_O) {
            filterOpaque = toggle(button, filterOpaque);
            dirty = true;
            return;
        }
        if (id == BTN_T) {
            filterSameTexture = toggle(button, filterSameTexture);
            dirty = true;
            return;
        }

        if (id == BTN_COPY_HOTBAR) {
            copyToHotbar(container);
            return;
        }
        if (id >= BTN_CYCLE_PREV_BASE && id < BTN_CYCLE_PREV_BASE + OUTPUT_SLOTS) {
            int slot = id - BTN_CYCLE_PREV_BASE;
            if (candidates[slot] != null && !candidates[slot].isEmpty()) {
                cycleIndex[slot] = (cycleIndex[slot] - 1 + candidates[slot].size()) % candidates[slot].size();
                updateOutputSlots(container);
            }
            return;
        }
        if (id >= BTN_CYCLE_NEXT_BASE && id < BTN_CYCLE_NEXT_BASE + OUTPUT_SLOTS) {
            int slot = id - BTN_CYCLE_NEXT_BASE;
            if (candidates[slot] != null && !candidates[slot].isEmpty()) {
                cycleIndex[slot] = (cycleIndex[slot] + 1) % candidates[slot].size();
                updateOutputSlots(container);
            }
        }
    }

    private void copyToHotbar(GradientHelperContainer container) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = container.getOutputInv()
                .getStackInSlot(i);
            if (stack == null) continue;
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            player.inventory.setInventorySlotContents(i, copy);
            player.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(36 + i, copy));
        }
    }

    private boolean toggle(GuiButton btn, boolean current) {
        boolean next = !current;
        ((GuiToggleButton) btn).setActive(next);
        return next;
    }

    // ── MC drawing helpers ─────────────────────────────────────────────────────

    private void drawMcPanel(int x, int y) {
        drawRect(x, y, x + GuiGradientHelper.PANEL_W, y + GuiGradientHelper.PANEL_H, C_PANEL);
        drawRect(x, y, x + GuiGradientHelper.PANEL_W, y + 1, C_PANEL_HI);
        drawRect(x, y, x + 1, y + GuiGradientHelper.PANEL_H, C_PANEL_HI);
        drawRect(
            x,
            y + GuiGradientHelper.PANEL_H - 1,
            x + GuiGradientHelper.PANEL_W,
            y + GuiGradientHelper.PANEL_H,
            C_PANEL_SH);
        drawRect(
            x + GuiGradientHelper.PANEL_W - 1,
            y,
            x + GuiGradientHelper.PANEL_W,
            y + GuiGradientHelper.PANEL_H,
            C_PANEL_SH);
    }

    // 18×18 inset — item renders at (x+1, y+1) inside this area
    private void drawMcSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, C_SLOT);
        drawRect(x, y, x + 18, y + 1, C_SLOT_SH);
        drawRect(x, y, x + 1, y + 18, C_SLOT_SH);
        drawRect(x, y + 17, x + 18, y + 18, C_SLOT_HI);
        drawRect(x + 17, y, x + 18, y + 18, C_SLOT_HI);
    }

    // Output slots: server's outputInv is always empty (server doesn't compute the gradient),
    // so intercept output clicks and give a copy to the cursor via C10(-1).
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == KeyConstants.LMB || mouseButton == KeyConstants.RMB) {
            Slot slot = getSlotUnderMouse(mouseX, mouseY);
            if (slot instanceof SlotGradientOutput) {
                ItemStack inSlot = slot.getStack();
                if (inSlot != null) {
                    ItemStack copy = inSlot.copy();
                    copy.stackSize = (mouseButton == KeyConstants.LMB) ? inSlot.getMaxStackSize() : 1;
                    mc.thePlayer.inventory.setItemStack(copy);
                    mc.thePlayer.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(-1, copy));
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private Slot getSlotUnderMouse(int mouseX, int mouseY) {
        for (Slot s : inventorySlots.inventorySlots) {
            if (mouseX >= guiLeft + s.xDisplayPosition && mouseX < guiLeft + s.xDisplayPosition + 16
                && mouseY >= guiTop + s.yDisplayPosition
                && mouseY < guiTop + s.yDisplayPosition + 16) {
                return s;
            }
        }
        return null;
    }

    private GradientHelperContainer gradientContainer() {
        return (GradientHelperContainer) inventorySlots;
    }
}
