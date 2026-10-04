/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory.colorPicker;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.world.inventory.AbstractFsotGuiContainer;
import github.thehighcruw.dimensium.world.inventory.CreativeGuiUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiColourPicker extends AbstractFsotGuiContainer {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int PANEL_W = ColourPickerContainer.PANEL_W;
    private static final int PANEL_H = 160;
    private static final int CELL = ColourPickerContainer.CELL;
    private static final int GRID_COLS = ColourPickerContainer.GRID_COLS;
    private static final int GRID_ROWS = ColourPickerContainer.GRID_ROWS;
    private static final int SV_SIZE = 84;
    private static final int HUE_W = 14;
    private static final int SLIDER_W = 55;
    private static final int SLIDER_H = 10;
    private static final int HOTBAR_Y = ColourPickerContainer.SLOT_Y;

    // Relative content anchors (relative to guiLeft/guiTop, set after initGui)
    private static final int GRID_REL_X = 8;
    private static final int CONTENT_REL_Y = 20;
    private static final int SV_REL_X = GRID_REL_X + GRID_COLS * CELL + 4; // 44
    private static final int HUE_REL_X = SV_REL_X + SV_SIZE + 3; // 135
    private static final int SLID_REL_X = HUE_REL_X + HUE_W + 4; // 148
    // SV picker ends at y=104 (CONTENT_REL_Y + SV_SIZE); swatch lives in SV column (x=44+)
    // which does not overlap with palette grid column (x=8-40), so can sit at y=106
    private static final int BELOW_REL_Y = 106;
    // 6 sliders × (SLIDER_H=10 + gap=4) + 4px RGB/HSB separator = 108
    private static final int HEX_REL_Y = CONTENT_REL_Y + 6 * (SLIDER_H + 4) + 4;

    // Colors — MC light-gray panel style
    private static final int C_PANEL = 0xFFC6C6C6; // panel fill
    private static final int C_PANEL_HI = 0xFFFFFFFF; // border highlight (top-left)
    private static final int C_PANEL_SH = 0xFF555555; // border shadow (bottom-right)
    private static final int C_TEXT = 0xFF404040; // dark text
    private static final int C_LABEL = 0xFF707070; // secondary text

    // ── State ─────────────────────────────────────────────────────────────────
    private float hue = 0.254f, sat = 0.814f, bri = 0.675f;
    private ColourPickerSlider sliderR, sliderG, sliderB;
    private ColourPickerSlider sliderH, sliderS, sliderBr;
    private GuiTextField fieldR, fieldG, fieldB;
    private GuiTextField fieldH, fieldS, fieldBr;
    private GuiTextField hexField;

    private boolean filterFullCube, filterSolid, filterOpaque, filterSameTexture;
    private List<ItemStack> results = new ArrayList<>();
    private int scrollOffset;
    private boolean dirty = true;

    private boolean draggingSV, draggingHue;

    public GuiColourPicker(EntityPlayer player) {
        super(new ColourPickerContainer(player.inventory));
        this.xSize = PANEL_W;
        this.ySize = PANEL_H;
    }

    @Override
    public void initGui() {
        super.initGui(); // sets guiLeft, guiTop

        int rgb = hsbToRgb(hue, sat, bri);
        sliderR = new ColourPickerSlider(0, 255, (rgb >> 16) & 0xFF, this::onSliderRgbChanged);
        sliderG = new ColourPickerSlider(0, 255, (rgb >> 8) & 0xFF, this::onSliderRgbChanged);
        sliderB = new ColourPickerSlider(0, 255, rgb & 0xFF, this::onSliderRgbChanged);
        sliderH = new ColourPickerSlider(0, 360, hue * 360f, this::onSliderHsbChanged);
        sliderS = new ColourPickerSlider(0, 100, sat * 100f, this::onSliderHsbChanged);
        sliderBr = new ColourPickerSlider(0, 100, bri * 100f, this::onSliderHsbChanged);
        sliderH.setRainbowHue();
        // Static gradients — pure channel display, independent of current selection.
        sliderR.setGradient(0xFF000000, 0xFFFF0000);
        sliderG.setGradient(0xFF000000, 0xFF00FF00);
        sliderB.setGradient(0xFF000000, 0xFF0000FF);
        sliderS.setGradient(0xFF808080, 0xFFFF0000);
        sliderBr.setGradient(0xFF000000, 0xFFFFFFFF);

        // Editable value fields — positioned to the right of each slider track.
        int fieldX = guiLeft + SLID_REL_X + 7 + SLIDER_W + 4;
        int fieldW = 26;
        int fsy = guiTop + CONTENT_REL_Y;
        fieldR = new GuiTextField(fontRendererObj, fieldX, fsy, fieldW, 10);
        fieldR.setMaxStringLength(3);
        fsy += SLIDER_H + 4;
        fieldG = new GuiTextField(fontRendererObj, fieldX, fsy, fieldW, 10);
        fieldG.setMaxStringLength(3);
        fsy += SLIDER_H + 4;
        fieldB = new GuiTextField(fontRendererObj, fieldX, fsy, fieldW, 10);
        fieldB.setMaxStringLength(3);
        fsy += SLIDER_H + 4 + 4;
        fieldH = new GuiTextField(fontRendererObj, fieldX, fsy, fieldW, 10);
        fieldH.setMaxStringLength(5);
        fsy += SLIDER_H + 4;
        fieldS = new GuiTextField(fontRendererObj, fieldX, fsy, fieldW, 10);
        fieldS.setMaxStringLength(5);
        fsy += SLIDER_H + 4;
        fieldBr = new GuiTextField(fontRendererObj, fieldX, fsy, fieldW, 10);
        fieldBr.setMaxStringLength(5);
        syncFieldTexts();

        // Hex field: right column, same x as slider labels, spanning full slider+value width
        hexField = new GuiTextField(
                fontRendererObj, guiLeft + SLID_REL_X + 7, guiTop + HEX_REL_Y, PANEL_W - SLID_REL_X - 7 - 8, 12);
        hexField.setMaxStringLength(8);
        hexField.setText(String.format("#%06x", rgb & 0xFFFFFF));
        hexField.setTextColor(0xFFFFFFFF);
        hexField.setDisabledTextColour(0xFFAAAAAA);

        buttonList.clear();
        CreativeGuiUtils.addFsotButtons(buttonList, guiLeft, guiTop, PANEL_W);
    }

    // ── Background rendering (called before slots/buttons) ────────────────────

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (dirty && BlockColorCache.INSTANCE.isInitialized()) {
            results = BlockColorCache.INSTANCE.findSimilarBlocks(
                    hsbToRgb(hue, sat, bri), filterFullCube, filterSolid, filterOpaque, filterSameTexture, 40);
            dirty = false;
            colourPickerContainer().updatePalette(results, scrollOffset);
        }

        // MC-style raised panel
        drawMcPanel(guiLeft, guiTop);

        // Title
        fontRendererObj.drawString(I18n.format("dimensium.colour_picker.title"), guiLeft + 6, guiTop + 6, C_TEXT);

        int contentY = guiTop + CONTENT_REL_Y;
        int svX = guiLeft + SV_REL_X;
        int hueX = guiLeft + HUE_REL_X;
        int slidX = guiLeft + SLID_REL_X;
        int gridX = guiLeft + GRID_REL_X;
        int belowY = guiTop + BELOW_REL_Y;

        // Palette grid — inset border; items are rendered by GuiContainer.drawSlot
        int gridW = GRID_COLS * CELL;
        drawMcInset(gridX - 2, contentY - 2, gridW + 4, GRID_ROWS * CELL + 4);

        // SV picker — inset border
        drawMcInset(svX - 2, contentY - 2, SV_SIZE + 4, SV_SIZE + 4);
        drawSvSquare(svX, contentY);

        // Hue bar — inset border
        drawMcInset(hueX - 2, contentY - 2, HUE_W + 4, SV_SIZE + 4);
        drawHueBar(hueX, contentY);

        // Sliders
        int sy = contentY;
        sy = drawSliderRow(sliderR, slidX, sy, "R");
        sy = drawSliderRow(sliderG, slidX, sy, "G");
        sy = drawSliderRow(sliderB, slidX, sy, "B");
        sy += 4;
        sy = drawSliderRow(sliderH, slidX, sy, "H");
        sy = drawSliderRow(sliderS, slidX, sy, "S");
        drawSliderRow(sliderBr, slidX, sy, "B");

        // Color preview swatch — inset, 16×16
        int rgb = hsbToRgb(hue, sat, bri);
        drawMcInset(svX - 2, belowY - 2, 20, 20);
        drawRect(svX, belowY, svX + 16, belowY + 16, 0xFF000000 | rgb);

        // Eye dropper slot (always empty — user holds item on cursor to sample)
        int dropX = svX + 20;
        drawMcSlot(dropX, belowY);

        // Slider value fields
        fieldR.drawTextBox();
        fieldG.drawTextBox();
        fieldB.drawTextBox();
        fieldH.drawTextBox();
        fieldS.drawTextBox();
        fieldBr.drawTextBox();

        // Hex label — at slider-label column (SLID_REL_X), same pattern as R/G/B/H/S/B
        fontRendererObj.drawString("#", guiLeft + SLID_REL_X, hexField.yPosition + 2, C_LABEL);
        hexField.drawTextBox();

        // Hotbar separator
        int sepY = guiTop + HOTBAR_Y - 8;
        drawRect(guiLeft + 6, sepY, guiLeft + PANEL_W - 6, sepY + 1, C_PANEL_SH);
        drawRect(guiLeft + 6, sepY + 1, guiLeft + PANEL_W - 6, sepY + 2, C_PANEL_HI);

        int startX = guiLeft + (PANEL_W - 9 * 18) / 2;
        for (int i = 0; i < 9; i++) {
            drawMcSlot(startX + i * 18, guiTop + HOTBAR_Y);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawFsotTooltip(mouseX, mouseY);
    }

    @Override
    protected String fsotTooltipKey(int id) {
        return switch (id) {
            case CreativeGuiUtils.BTN_F -> "dimensium.colour_picker.filter.full_cube";
            case CreativeGuiUtils.BTN_S -> "dimensium.colour_picker.filter.solid";
            case CreativeGuiUtils.BTN_O -> "dimensium.colour_picker.filter.opaque";
            case CreativeGuiUtils.BTN_T -> "dimensium.colour_picker.filter.same_texture";
            default -> null;
        };
    }

    /** Classic MC raised panel (highlight top-left, shadow bottom-right). */
    private void drawMcPanel(int x, int y) {
        drawRect(x, y, x + GuiColourPicker.PANEL_W, y + GuiColourPicker.PANEL_H, C_PANEL);
        drawRect(x, y, x + GuiColourPicker.PANEL_W, y + 2, C_PANEL_HI);
        drawRect(x, y, x + 2, y + GuiColourPicker.PANEL_H, C_PANEL_HI);
        drawRect(
                x,
                y + GuiColourPicker.PANEL_H - 2,
                x + GuiColourPicker.PANEL_W,
                y + GuiColourPicker.PANEL_H,
                C_PANEL_SH);
        drawRect(
                x + GuiColourPicker.PANEL_W - 2,
                y,
                x + GuiColourPicker.PANEL_W,
                y + GuiColourPicker.PANEL_H,
                C_PANEL_SH);
    }

    private void drawMcInset(int x, int y, int w, int h) {
        AbstractFsotGuiContainer.drawBeveledRect(x, y, w, h, C_PANEL, C_PANEL_SH, C_PANEL_HI);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // intentionally empty — all text drawn in background layer at absolute coords
    }

    // ── SV square ─────────────────────────────────────────────────────────────

    private void drawSvSquare(int x, int y) {
        int hueRgb = hsbToRgb(hue, 1f, 1f);
        float hr = ((hueRgb >> 16) & 0xFF) / 255f;
        float hg = ((hueRgb >> 8) & 0xFF) / 255f;
        float hb = (hueRgb & 0xFF) / 255f;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        Tessellator t = Tessellator.instance;

        t.startDrawingQuads();
        t.setColorRGBA_F(hr, hg, hb, 1f);
        t.addVertex(x + SV_SIZE, y + SV_SIZE, 0);
        t.setColorRGBA_F(hr, hg, hb, 1f);
        t.addVertex(x + SV_SIZE, y, 0);
        t.setColorRGBA_F(1f, 1f, 1f, 1f);
        t.addVertex(x, y, 0);
        t.setColorRGBA_F(1f, 1f, 1f, 1f);
        t.addVertex(x, y + SV_SIZE, 0);
        t.draw();

        t.startDrawingQuads();
        t.setColorRGBA_F(0f, 0f, 0f, 1f);
        t.addVertex(x + SV_SIZE, y + SV_SIZE, 0);
        t.setColorRGBA_F(0f, 0f, 0f, 0f);
        t.addVertex(x + SV_SIZE, y, 0);
        t.setColorRGBA_F(0f, 0f, 0f, 0f);
        t.addVertex(x, y, 0);
        t.setColorRGBA_F(0f, 0f, 0f, 1f);
        t.addVertex(x, y + SV_SIZE, 0);
        t.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        int cx = x + (int) (sat * SV_SIZE);
        int cy = y + (int) ((1f - bri) * SV_SIZE);
        drawRect(cx - 3, cy, cx + 4, cy + 1, 0xFFFFFFFF);
        drawRect(cx, cy - 3, cx + 1, cy + 4, 0xFFFFFFFF);
        drawRect(cx - 2, cy, cx + 3, cy + 1, 0xFF000000);
        drawRect(cx, cy - 2, cx + 1, cy + 3, 0xFF000000);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    // ── Hue bar ───────────────────────────────────────────────────────────────

    private void drawHueBar(int x, int y) {
        float[][] stops = {{1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0}};
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.instance;
        int segmentCount = stops.length - 1;
        tessellator.startDrawingQuads();
        for (int i = 0; i < segmentCount; i++) {
            int y0 = y + i * SV_SIZE / segmentCount;
            int y1 = y + (i + 1) * SV_SIZE / segmentCount;
            float[] top = stops[i], bot = stops[i + 1];
            tessellator.setColorRGBA_F(bot[0], bot[1], bot[2], 1f);
            tessellator.addVertex(x + HUE_W, y1, 0);
            tessellator.setColorRGBA_F(top[0], top[1], top[2], 1f);
            tessellator.addVertex(x + HUE_W, y0, 0);
            tessellator.setColorRGBA_F(top[0], top[1], top[2], 1f);
            tessellator.addVertex(x, y0, 0);
            tessellator.setColorRGBA_F(bot[0], bot[1], bot[2], 1f);
            tessellator.addVertex(x, y1, 0);
        }
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        int markerY = y + (int) (hue * SV_SIZE);
        drawRect(x, markerY - 1, x + HUE_W, markerY, 0x80000000);
        drawRect(x, markerY, x + HUE_W, markerY + 2, 0xFFFFFFFF);
        drawRect(x, markerY + 2, x + HUE_W, markerY + 3, 0x80000000);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    // ── Slider rows ───────────────────────────────────────────────────────────

    private int drawSliderRow(ColourPickerSlider slider, int x, int y, String label) {
        fontRendererObj.drawString(label, x, y + 1, C_LABEL);
        slider.draw(x + 7, y, SLIDER_W);
        return y + SLIDER_H + 4;
    }

    // ── Mouse input ───────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == KeyConstants.LMB) {
            int svX = guiLeft + SV_REL_X;
            int hueX = guiLeft + HUE_REL_X;
            int contentY = guiTop + CONTENT_REL_Y;
            int belowY = guiTop + BELOW_REL_Y;

            if (inBox(mouseX, mouseY, svX, contentY, SV_SIZE, SV_SIZE)) {
                draggingSV = true;
                updateSV(mouseX - svX, mouseY - contentY);
                return;
            }
            if (inBox(mouseX, mouseY, hueX, contentY, HUE_W, SV_SIZE)) {
                draggingHue = true;
                updateHue(mouseY - contentY);
                return;
            }
            if (trySliderPress(mouseX, mouseY)) return;

            // Eye dropper
            if (inBox(mouseX, mouseY, svX + 20, belowY, CELL, CELL)) {
                ItemStack held = mc.thePlayer.inventory.getItemStack();
                if (held != null) {
                    Block b = Block.getBlockFromItem(held.getItem());
                    if (b != null) {
                        int rgb = BlockColorCache.INSTANCE.blockColor(Block.getIdFromBlock(b), held.getItemDamage());
                        if (rgb >= 0) applyRgb(rgb);
                    }
                }
                return;
            }
        }

        hexField.mouseClicked(mouseX, mouseY, button);
        fieldR.mouseClicked(mouseX, mouseY, button);
        fieldG.mouseClicked(mouseX, mouseY, button);
        fieldB.mouseClicked(mouseX, mouseY, button);
        fieldH.mouseClicked(mouseX, mouseY, button);
        fieldS.mouseClicked(mouseX, mouseY, button);
        fieldBr.mouseClicked(mouseX, mouseY, button);

        // Intercept slot clicks before GuiContainer.mouseClicked so it never sends
        // C0EPacketClickWindow — we use C10PacketCreativeInventoryAction in handleMouseClick.
        if (button == KeyConstants.LMB || button == KeyConstants.RMB) {
            Slot slot = slotAtPosition(mouseX, mouseY);
            if (slot != null) {
                handleMouseClick(slot, inventorySlots.inventorySlots.indexOf(slot), button, 0);
                return;
            }
            // Click outside GUI with cursor item — drop it.
            if (mc.thePlayer.inventory.getItemStack() != null) {
                handleMouseClick(null, -999, button, 0);
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Creative-mode slot interaction without standard container packets.
     *
     * C10PacketCreativeInventoryAction semantics:
     * slot >= 0 → set that inventory slot to the given stack (server authoritative)
     * slot == -1, null → clear server cursor (no drop)
     * slot == -1, item → spawn item as world drop entity on server
     *
     * Cursor state is client-only — picking up items does not require a server packet.
     * Only slot mutations (placing into hotbar) and world drops need C10.
     */
    @Override
    protected void handleMouseClick(Slot slot, int slotId, int button, int clickType) {
        if (slot == null) {
            // Outside-GUI click: drop cursor item into world.
            if (slotId == -999) {
                ItemStack cursor = mc.thePlayer.inventory.getItemStack();
                if (cursor != null) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(-1, cursor));
                    mc.thePlayer.inventory.setItemStack(null);
                }
            }
            return;
        }

        int paletteCount = ColourPickerContainer.GRID_PALETTE;
        ItemStack cursor = mc.thePlayer.inventory.getItemStack();
        ItemStack slotItem = slot.getStack();

        if (slotId < paletteCount) {
            // Palette: give one copy to cursor client-side, slot is infinite supply.
            if (cursor == null && slotItem != null) {
                ItemStack copy = slotItem.copy();
                copy.stackSize = 1;
                mc.thePlayer.inventory.setItemStack(copy);
            }
            return;
        }

        // Hotbar slot (index 0-8 in player inventory).
        int hotbarIndex = slotId - paletteCount;

        // C10 slot index: server calls putStackInSlot(slotId, item) directly on ContainerPlayer.
        // ContainerPlayer hotbar slots are at container indices 36-44, so C10(36+i) = hotbar[i].
        if (clickType == 4) {
            // Q key (button=0) drop one; Ctrl+Q (button=1) drop all.
            if (slotItem != null && cursor == null) {
                int amount = (button == 1) ? slotItem.stackSize : 1;
                ItemStack drop = slotItem.copy();
                drop.stackSize = amount;
                mc.thePlayer.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(-1, drop));
                slotItem.stackSize -= amount;
                ItemStack remaining = slotItem.stackSize > 0 ? slotItem : null;
                slot.putStack(remaining);
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C10PacketCreativeInventoryAction(36 + hotbarIndex, remaining));
            }
            return;
        }

        if (cursor == null) {
            // Pick up from hotbar: move item to cursor, empty the slot.
            if (slotItem != null) {
                mc.thePlayer.inventory.setItemStack(slotItem);
                slot.putStack(null);
                mc.thePlayer.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(36 + hotbarIndex, null));
            }
        } else {
            // Place cursor into hotbar slot (or swap).
            ItemStack toPlace = cursor.copy();
            slot.putStack(toPlace);
            mc.thePlayer.inventory.setItemStack(slotItem); // null if slot was empty
            mc.thePlayer.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(36 + hotbarIndex, toPlace));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // Clear cursor item on close so it doesn't ghost on re-open
        if (mc.thePlayer.inventory.getItemStack() != null) {
            mc.thePlayer.inventory.setItemStack(null);
            mc.thePlayer.sendQueue.addToSendQueue(new C10PacketCreativeInventoryAction(-1, null));
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state == 0) {
            draggingSV = draggingHue = false;
            sliderR.mouseReleased();
            sliderG.mouseReleased();
            sliderB.mouseReleased();
            sliderH.mouseReleased();
            sliderS.mouseReleased();
            sliderBr.mouseReleased();
        }
        // Do NOT call super — GuiContainer.mouseMovedOrUp fires handleMouseClick on the hovered
        // slot for non-drag releases, which would immediately reverse our pickup (double-fire).
        // We don't use drag-splitting, so there is nothing in super we need.
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long held) {
        if (draggingSV) {
            updateSV(mouseX - (guiLeft + SV_REL_X), mouseY - (guiTop + CONTENT_REL_Y));
            return;
        }
        if (draggingHue) {
            updateHue(mouseY - (guiTop + CONTENT_REL_Y));
            return;
        }
        trySliderDrag(mouseX);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            int totalRows = (results.size() + GRID_COLS - 1) / GRID_COLS;
            scrollOffset = Math.max(0, Math.min(totalRows - GRID_ROWS, scrollOffset + (delta < 0 ? 1 : -1)));
            colourPickerContainer().updatePalette(results, scrollOffset);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        if (fieldKey(fieldR, typedChar, keyCode)) {
            parseIntField(fieldR, sliderR);
            onSliderRgbChanged();
            return;
        }
        if (fieldKey(fieldG, typedChar, keyCode)) {
            parseIntField(fieldG, sliderG);
            onSliderRgbChanged();
            return;
        }
        if (fieldKey(fieldB, typedChar, keyCode)) {
            parseIntField(fieldB, sliderB);
            onSliderRgbChanged();
            return;
        }
        if (fieldKey(fieldH, typedChar, keyCode)) {
            parseFloatField(fieldH, sliderH, 360);
            onSliderHsbChanged();
            return;
        }
        if (fieldKey(fieldS, typedChar, keyCode)) {
            parseFloatField(fieldS, sliderS, 100);
            onSliderHsbChanged();
            return;
        }
        if (fieldKey(fieldBr, typedChar, keyCode)) {
            parseFloatField(fieldBr, sliderBr, 100);
            onSliderHsbChanged();
            return;
        }
        if (hexField.isFocused()) {
            hexField.textboxKeyTyped(typedChar, keyCode);
            String txt = hexField.getText().replaceAll("[^0-9a-fA-F]", "");
            if (txt.length() == 6) {
                try {
                    applyRgb(Integer.parseInt(txt, 16));
                } catch (NumberFormatException ignored) {
                }
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean fieldKey(GuiTextField field, char character, int keyCode) {
        if (!field.isFocused()) return false;
        field.textboxKeyTyped(character, keyCode);
        return true;
    }

    private void parseIntField(GuiTextField field, ColourPickerSlider slider) {
        try {
            slider.setValue(
                    Math.max(0, Math.min(255, Integer.parseInt(field.getText().trim()))));
        } catch (NumberFormatException ignored) {
        }
    }

    private void parseFloatField(GuiTextField field, ColourPickerSlider slider, float max) {
        try {
            slider.setValue(Math.max(
                    (float) 0, Math.min(max, Float.parseFloat(field.getText().trim()))));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case CreativeGuiUtils.BTN_F -> filterFullCube = toggle(button, filterFullCube);
            case CreativeGuiUtils.BTN_S -> filterSolid = toggle(button, filterSolid);
            case CreativeGuiUtils.BTN_O -> filterOpaque = toggle(button, filterOpaque);
            case CreativeGuiUtils.BTN_T -> filterSameTexture = toggle(button, filterSameTexture);
            default -> {}
        }
        dirty = true;
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    private void updateSV(int relativeX, int relativeY) {
        sat = clamp01((float) relativeX / SV_SIZE);
        bri = clamp01(1f - (float) relativeY / SV_SIZE);
        syncSlidersFromHSB();
        dirty = true;
    }

    private void updateHue(int relativeY) {
        hue = clamp01((float) relativeY / SV_SIZE);
        syncSlidersFromHSB();
        dirty = true;
    }

    private void applyRgb(int rgb) {
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        syncSlidersFromHSB();
        hexField.setText(String.format("#%06x", rgb & 0xFFFFFF));
        dirty = true;
    }

    private void onSliderRgbChanged() {
        int r = (int) sliderR.getValue(), g = (int) sliderG.getValue(), b = (int) sliderB.getValue();
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        sliderH.setValue(hue * 360f);
        sliderS.setValue(sat * 100f);
        sliderBr.setValue(bri * 100f);
        hexField.setText(String.format("#%06x", (r << 16) | (g << 8) | b));
        dirty = true;
        syncFieldTexts();
    }

    private void onSliderHsbChanged() {
        hue = sliderH.getValue() / 360f;
        sat = sliderS.getValue() / 100f;
        bri = sliderBr.getValue() / 100f;
        int rgb = hsbToRgb(hue, sat, bri);
        sliderR.setValue((rgb >> 16) & 0xFF);
        sliderG.setValue((rgb >> 8) & 0xFF);
        sliderB.setValue(rgb & 0xFF);
        hexField.setText(String.format("#%06x", rgb & 0xFFFFFF));
        dirty = true;
        syncFieldTexts();
    }

    private void syncSlidersFromHSB() {
        int rgb = hsbToRgb(hue, sat, bri);
        sliderR.setValue((rgb >> 16) & 0xFF);
        sliderG.setValue((rgb >> 8) & 0xFF);
        sliderB.setValue(rgb & 0xFF);
        sliderH.setValue(hue * 360f);
        sliderS.setValue(sat * 100f);
        sliderBr.setValue(bri * 100f);
        hexField.setText(String.format("#%06x", rgb & 0xFFFFFF));
        syncFieldTexts();
    }

    private void syncFieldTexts() {
        if (fieldR == null) return; // guard before initGui
        if (!fieldR.isFocused()) fieldR.setText(String.valueOf((int) sliderR.getValue()));
        if (!fieldG.isFocused()) fieldG.setText(String.valueOf((int) sliderG.getValue()));
        if (!fieldB.isFocused()) fieldB.setText(String.valueOf((int) sliderB.getValue()));
        if (!fieldH.isFocused()) fieldH.setText(String.valueOf(round1(sliderH.getValue())));
        if (!fieldS.isFocused()) fieldS.setText(String.valueOf(round1(sliderS.getValue())));
        if (!fieldBr.isFocused()) fieldBr.setText(String.valueOf(round1(sliderBr.getValue())));
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private boolean trySliderPress(int mouseX, int mouseY) {
        return sliderR.mousePressed(mouseX, mouseY)
                || sliderG.mousePressed(mouseX, mouseY)
                || sliderB.mousePressed(mouseX, mouseY)
                || sliderH.mousePressed(mouseX, mouseY)
                || sliderS.mousePressed(mouseX, mouseY)
                || sliderBr.mousePressed(mouseX, mouseY);
    }

    private void trySliderDrag(int mouseX) {
        sliderR.mouseDragged(mouseX);
        sliderG.mouseDragged(mouseX);
        sliderB.mouseDragged(mouseX);
        sliderH.mouseDragged(mouseX);
        sliderS.mouseDragged(mouseX);
        sliderBr.mouseDragged(mouseX);
    }

    private static boolean inBox(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static int hsbToRgb(float h, float s, float b) {
        return Color.HSBtoRGB(h, s, b) & 0xFFFFFF;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    private Slot slotAtPosition(int mouseX, int mouseY) {
        for (int slotIndex = 0; slotIndex < inventorySlots.inventorySlots.size(); slotIndex++) {
            Slot slot = inventorySlots.inventorySlots.get(slotIndex);
            // Palette slots: exact 16×16 cell. Hotbar slots: registered +1 inside 18×18 frame,
            // so subtract 1 to align hitbox with the visual frame.
            boolean isHotbar = slotIndex >= ColourPickerContainer.GRID_PALETTE;
            int offsetX = isHotbar ? -1 : 0;
            int offsetY = isHotbar ? -1 : 0;
            int hitWidth = isHotbar ? 18 : 16;
            int hitHeight = isHotbar ? 18 : 16;
            if (mouseX >= guiLeft + slot.xDisplayPosition + offsetX
                    && mouseX < guiLeft + slot.xDisplayPosition + offsetX + hitWidth
                    && mouseY >= guiTop + slot.yDisplayPosition + offsetY
                    && mouseY < guiTop + slot.yDisplayPosition + offsetY + hitHeight) {
                return slot;
            }
        }
        return null;
    }

    private ColourPickerContainer colourPickerContainer() {
        return (ColourPickerContainer) inventorySlots;
    }
}
