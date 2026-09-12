package github.thehighcruw.dimensium.render.popup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.render.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class FillSelectionWindow extends ImGuiWindow {

    public static final FillSelectionWindow INSTANCE = new FillSelectionWindow();

    private static final String WINDOW_ID = "###fill_selection_window";

    private static final int MODE_FILL_ALL = 0;
    private static final int MODE_FILL_OUTLINE = 1;
    private static final int MODE_FILL_WALLS = 2;
    private static final int MODE_FILL_TOP = 3;
    private static final int MODE_FILL_BOTTOM = 4;

    private static final int[][] FACE_DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private boolean open = false;

    private ItemStack selectedBlock = null;
    private final ImInt fillMode = new ImInt(MODE_FILL_ALL);

    private FillSelectionWindow() {}

    public void open() {
        selectedBlock = null;
        fillMode.set(MODE_FILL_ALL);
        open = true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float vpW = ImGui.getIO()
            .getDisplaySizeX(),
            vpH = ImGui.getIO()
                .getDisplaySizeY();
        float w = 320f * scale;
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.3f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 240f * scale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.op.fill.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean canApply = selectedBlock != null && hasSel;

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * scale;
            float footerH = ImGui.getStyle()
                .getItemSpacingY() + 1f
                + ImGui.getStyle()
                    .getItemSpacingY()
                + ImGui.getFrameHeight()
                + ImGui.getStyle()
                    .getWindowPaddingY();
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerH);
            ImGui.beginChild("##fill_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float cellSize = 20f * scale;

            if (selectedBlock != null) {
                if (DeferredItemRender.placeButton("##fill_block", selectedBlock, cellSize * 0.5f, false)) {
                    openPicker();
                }
                ImGui.sameLine();
                ImGui.text(selectedBlock.getDisplayName());
            } else {
                if (ImGui
                    .button(I18n.format("dimensium.op.replace.no_block") + "##fill_pick", cellSize * 2f, cellSize)) {
                    openPicker();
                }
            }

            ImGui.spacing();

            String[] modeLabels = { I18n.format("dimensium.op.fill.mode.fill"),
                I18n.format("dimensium.op.fill.mode.outline"), I18n.format("dimensium.op.fill.mode.walls"),
                I18n.format("dimensium.op.fill.mode.top"), I18n.format("dimensium.op.fill.mode.bottom") };
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() * 0.7f);
            ImGui.combo("##fill_mode", fillMode, modeLabels, modeLabels.length);

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(
                windowW - ImGui.getStyle()
                    .getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.apply") + "##fill_apply", btnW, 0)) {
                applyFill();
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }

    private void openPicker() {
        OverlayRenderer.picker.open(stack -> selectedBlock = stack);
    }

    private void applyFill() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || selectedBlock == null) return;
        Block block = Block.getBlockFromItem(selectedBlock.getItem());
        if (block == null) return;
        int blockId = Block.getIdFromBlock(block);
        int blockMeta = selectedBlock.getItemDamage();
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();

        for (long key : selected) {
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            if (matchesFillMode(selected, x, y, z, fillMode.get())) {
                ops.add(new int[] { x, y, z, blockId, blockMeta });
            }
        }

        String modeName = getModeKey(fillMode.get());
        BlockSender.sendChunked(ops, I18n.format("dimensium.action.op.fill", I18n.format(modeName)));
    }

    private boolean matchesFillMode(Set<Long> selected, int x, int y, int z, int mode) {
        switch (mode) {
            case MODE_FILL_ALL:
                return true;
            case MODE_FILL_OUTLINE:
                for (int[] f : FACE_DIRS) {
                    if (!selected.contains(SelectionState.pack(x + f[0], y + f[1], z + f[2]))) return true;
                }
                return false;
            case MODE_FILL_WALLS:
                return !selected.contains(SelectionState.pack(x + 1, y, z))
                    || !selected.contains(SelectionState.pack(x - 1, y, z))
                    || !selected.contains(SelectionState.pack(x, y, z + 1))
                    || !selected.contains(SelectionState.pack(x, y, z - 1));
            case MODE_FILL_TOP:
                return !selected.contains(SelectionState.pack(x, y + 1, z));
            case MODE_FILL_BOTTOM:
                return !selected.contains(SelectionState.pack(x, y - 1, z));
            default:
                return true;
        }
    }

    private String getModeKey(int mode) {
        switch (mode) {
            case MODE_FILL_OUTLINE:
                return "dimensium.op.fill.mode.outline";
            case MODE_FILL_WALLS:
                return "dimensium.op.fill.mode.walls";
            case MODE_FILL_TOP:
                return "dimensium.op.fill.mode.top";
            case MODE_FILL_BOTTOM:
                return "dimensium.op.fill.mode.bottom";
            default:
                return "dimensium.op.fill.mode.fill";
        }
    }
}
