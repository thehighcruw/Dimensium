/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render;

import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.handler.EditorActions;
import github.thehighcruw.dimensium.history.ClientEditHistory;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.popup.AnalyzeWindow;
import github.thehighcruw.dimensium.render.popup.AutoshadeWindow;
import github.thehighcruw.dimensium.render.popup.BlockInfoWindow;
import github.thehighcruw.dimensium.render.popup.ClipboardWindow;
import github.thehighcruw.dimensium.render.popup.ColourFieldWindow;
import github.thehighcruw.dimensium.render.popup.DistortSelectionWindow;
import github.thehighcruw.dimensium.render.popup.FillSelectionWindow;
import github.thehighcruw.dimensium.render.popup.FilterSelectionWindow;
import github.thehighcruw.dimensium.render.popup.OperationsWindow;
import github.thehighcruw.dimensium.render.popup.PaletteEditorWindow;
import github.thehighcruw.dimensium.render.popup.PaletteWindow;
import github.thehighcruw.dimensium.render.popup.ReplaceSelectionWindow;
import github.thehighcruw.dimensium.render.popup.SelectionWindow;
import github.thehighcruw.dimensium.render.popup.SettingsModal;
import github.thehighcruw.dimensium.render.popup.SmoothSelectionWindow;
import github.thehighcruw.dimensium.render.popup.ToolMaskEditorWindow;
import github.thehighcruw.dimensium.render.popup.ToolMaskListWindow;
import github.thehighcruw.dimensium.render.popup.TypeReplaceSelectionWindow;
import github.thehighcruw.dimensium.render.sidebar.HistoryWindow;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.SelectionTransforms;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.mask.MaskEntry;
import github.thehighcruw.dimensium.tool.mask.MaskFolder;
import github.thehighcruw.dimensium.tool.mask.ToolMask;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.state.BooleanOp;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public final class MenuBar {

    public static final MenuBar INSTANCE = new MenuBar();

    private float renderedHeight = 0f;
    private final float[] pendingViewScale = { 1.0f };

    public boolean containsMouse(float mx, float my, float screenW) {
        return my >= 0 && my < renderedHeight && mx >= 0 && mx < screenW;
    }

    private MenuBar() {}

    /** Height of the last rendered menu bar, in physical pixels. Zero before first frame. */
    public float height() {
        return renderedHeight;
    }

    public void render() {
        renderedHeight = ImGui.getFrameHeight();
        if (!ImGui.beginMainMenuBar()) return;

        if (ImGui.beginMenu(I18n.format("dimensium.menu.edit"))) {
            renderEditMenu();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.menu.select"))) {
            renderSelectMenu();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.menu.view"))) {
            renderViewMenu();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.menu.operations"))) {
            renderOperationsMenu();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.menu.tool_masks"))) {
            renderToolMasksMenu();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.menu.window"))) {
            if (ImGui
                .menuItem(I18n.format("dimensium.menu.window.tool_panel"), null, OverlayRenderer.toolPanel.isOpen())) {
                OverlayRenderer.toolPanel.setOpen(!OverlayRenderer.toolPanel.isOpen());
            }
            ImGui.separator();
            if (ImGui.menuItem(
                I18n.format("dimensium.menu.window.tool_mask_list"),
                null,
                ToolMaskListWindow.INSTANCE.isOpen())) {
                ToolMaskListWindow.INSTANCE.setOpen(!ToolMaskListWindow.INSTANCE.isOpen());
            }
            if (ImGui.menuItem(
                I18n.format("dimensium.menu.window.tool_mask_editor"),
                null,
                ToolMaskEditorWindow.INSTANCE.isOpen())) {
                ToolMaskEditorWindow.INSTANCE.setOpen(!ToolMaskEditorWindow.INSTANCE.isOpen());
            }
            if (ImGui.menuItem(I18n.format("dimensium.menu.window.history"), null, HistoryWindow.INSTANCE.isOpen())) {
                HistoryWindow.INSTANCE.setOpen(!HistoryWindow.INSTANCE.isOpen());
            }
            if (ImGui.menuItem(I18n.format("dimensium.menu.window.palette"), null, PaletteWindow.INSTANCE.isOpen())) {
                PaletteWindow.INSTANCE.setOpen(!PaletteWindow.INSTANCE.isOpen());
            }
            if (ImGui.menuItem(
                I18n.format("dimensium.menu.window.palette_editor"),
                null,
                PaletteEditorWindow.INSTANCE.isOpen())) {
                PaletteEditorWindow.INSTANCE.setOpen(!PaletteEditorWindow.INSTANCE.isOpen());
            }
            if (ImGui
                .menuItem(I18n.format("dimensium.menu.window.block_info"), null, BlockInfoWindow.INSTANCE.isOpen())) {
                BlockInfoWindow.INSTANCE.setOpen(!BlockInfoWindow.INSTANCE.isOpen());
            }
            if (ImGui
                .menuItem(I18n.format("dimensium.menu.window.selection"), null, SelectionWindow.INSTANCE.isOpen())) {
                SelectionWindow.INSTANCE.setOpen(!SelectionWindow.INSTANCE.isOpen());
            }
            if (ImGui
                .menuItem(I18n.format("dimensium.menu.window.clipboard"), null, ClipboardWindow.INSTANCE.isOpen())) {
                ClipboardWindow.INSTANCE.setOpen(!ClipboardWindow.INSTANCE.isOpen());
            }
            ImGui.separator();
            if (ImGui.beginMenu(I18n.format("dimensium.menu.window.operations"))) {
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.operations_panel"),
                    null,
                    OperationsWindow.INSTANCE.isOpen())) {
                    OperationsWindow.INSTANCE.setOpen(!OperationsWindow.INSTANCE.isOpen());
                }
                if (ImGui
                    .menuItem(I18n.format("dimensium.menu.window.analyze"), null, AnalyzeWindow.INSTANCE.isOpen())) {
                    AnalyzeWindow.INSTANCE.setOpen(!AnalyzeWindow.INSTANCE.isOpen());
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.autoshade"),
                    null,
                    AutoshadeWindow.INSTANCE.isOpen())) {
                    AutoshadeWindow.INSTANCE.setOpen(!AutoshadeWindow.INSTANCE.isOpen());
                }
                ImGui.separator();
                if (ImGui
                    .menuItem(I18n.format("dimensium.menu.window.fill"), null, FillSelectionWindow.INSTANCE.isOpen())) {
                    if (FillSelectionWindow.INSTANCE.isOpen()) FillSelectionWindow.INSTANCE.close();
                    else FillSelectionWindow.INSTANCE.open();
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.replace"),
                    null,
                    ReplaceSelectionWindow.INSTANCE.isOpen())) {
                    if (ReplaceSelectionWindow.INSTANCE.isOpen()) ReplaceSelectionWindow.INSTANCE.close();
                    else ReplaceSelectionWindow.INSTANCE.open();
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.type_replace"),
                    null,
                    TypeReplaceSelectionWindow.INSTANCE.isOpen())) {
                    if (TypeReplaceSelectionWindow.INSTANCE.isOpen()) TypeReplaceSelectionWindow.INSTANCE.close();
                    else TypeReplaceSelectionWindow.INSTANCE.open();
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.colour_field"),
                    null,
                    ColourFieldWindow.INSTANCE.isOpen())) {
                    if (ColourFieldWindow.INSTANCE.isOpen()) ColourFieldWindow.INSTANCE.close();
                    else ColourFieldWindow.INSTANCE.open();
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.filter"),
                    null,
                    FilterSelectionWindow.INSTANCE.isOpen())) {
                    if (FilterSelectionWindow.INSTANCE.isOpen()) FilterSelectionWindow.INSTANCE.close();
                    else FilterSelectionWindow.INSTANCE.open();
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.distort"),
                    null,
                    DistortSelectionWindow.INSTANCE.isOpen())) {
                    if (DistortSelectionWindow.INSTANCE.isOpen()) DistortSelectionWindow.INSTANCE.close();
                    else DistortSelectionWindow.INSTANCE.open();
                }
                if (ImGui.menuItem(
                    I18n.format("dimensium.menu.window.smooth"),
                    null,
                    SmoothSelectionWindow.INSTANCE.isOpen())) {
                    if (SmoothSelectionWindow.INSTANCE.isOpen()) SmoothSelectionWindow.INSTANCE.close();
                    else SmoothSelectionWindow.INSTANCE.open();
                }
                ImGui.endMenu();
            }
            ImGui.separator();
            if (ImGui.menuItem(I18n.format("dimensium.menu.window.reset_layout"))) {
                OverlayRenderer.requestResetLayout();
            }
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.menu.help"))) {
            if (ImGui.menuItem(I18n.format("dimensium.menu.help.settings"))) {
                SettingsModal.INSTANCE.open();
            }
            ImGui.endMenu();
        }

        ImGui.endMainMenuBar();
    }

    private void renderToolMasksMenu() {
        ToolMaskRegistry registry = ToolMaskRegistry.INSTANCE;
        ToolMask active = registry.getActiveMask();

        if (ImGui.menuItem(I18n.format("dimensium.mask.list.none"), null, active == null)) {
            registry.setActiveMask(null);
        }

        if (!registry.entries.isEmpty()) {
            ImGui.separator();
            renderMaskEntries(registry.entries, active);
        }
    }

    private void renderMaskEntries(java.util.List<MaskEntry> entries, ToolMask active) {
        for (MaskEntry entry : entries) {
            if (entry instanceof ToolMask) {
                ToolMask mask = (ToolMask) entry;
                if (ImGui.menuItem(mask.getName(), null, active == mask)) {
                    ToolMaskRegistry.INSTANCE.setActiveMask(active == mask ? null : mask);
                }
            } else if (entry instanceof MaskFolder) {
                MaskFolder folder = (MaskFolder) entry;
                if (ImGui.beginMenu(folder.getName())) {
                    renderMaskEntries(folder.entries, active);
                    ImGui.endMenu();
                }
            }
        }
    }

    private void renderOperationsMenu() {
        boolean hasSel = SelectionState.INSTANCE.hasSelection();

        if (ImGui.menuItem(I18n.format("dimensium.op.fill"), null, false, hasSel)) {
            FillSelectionWindow.INSTANCE.open();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.fill_nearest"), null, false, hasSel)) {
            EditorActions.fillNearest();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.replace"), null, false, hasSel)) {
            ReplaceSelectionWindow.INSTANCE.open();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.type_replace"), null, false, hasSel)) {
            TypeReplaceSelectionWindow.INSTANCE.open();
        }
        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.op.autoshade"), null, false, hasSel)) {
            AutoshadeWindow.INSTANCE.open();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.op.drain"), null, false, hasSel)) {
            EditorActions.drain();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.simulate_gravity"), null, false, hasSel)) {
            EditorActions.simulateGravity();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.trigger_updates"), null, false, hasSel)) {
            EditorActions.triggerUpdates();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.op.hollow"), null, false, hasSel)) {
            EditorActions.hollow();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.fill_gaps"), null, false, hasSel)) {
            EditorActions.fillGaps();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.op.generate_colour_field"), null, false, hasSel)) {
            ColourFieldWindow.INSTANCE.open();
        }
        if (ImGui.menuItem(I18n.format("dimensium.op.analyze"), null, false, hasSel)) {
            AnalyzeWindow.INSTANCE.open();
        }
    }

    private void renderViewMenu() {
        if (ImGui.menuItem(I18n.format("dimensium.menu.view.new_view"))) {
            ViewportRegistry.INSTANCE.addViewport();
            ViewportPanel.INSTANCE.requestSelectIndex(ViewportRegistry.INSTANCE.viewports.size() - 1);
        }

        ImGui.separator();

        ViewState vs = ViewState.INSTANCE;
        String notBound = I18n.format("dimensium.menu.view.not_bound");
        if (ImGui.menuItem(I18n.format("dimensium.menu.view.show_selection"), notBound, vs.showSelection)) {
            vs.showSelection = !vs.showSelection;
        }
        if (ImGui.menuItem(I18n.format("dimensium.menu.view.show_key_presses"), notBound, vs.showKeyPresses)) {
            vs.showKeyPresses = !vs.showKeyPresses;
        }
        if (ImGui.menuItem(I18n.format("dimensium.menu.view.flip_canvas"), notBound, vs.flipCanvas)) {
            vs.flipCanvas = !vs.flipCanvas;
        }

        ImGui.separator();

        ImGui.text(I18n.format("dimensium.menu.view.ui_scale"));
        ImGui.setNextItemWidth(160f * ImGuiManager.INSTANCE.getUIScale());
        ImGui.sliderFloat("##view_ui_scale", pendingViewScale, 0.5f, 3.0f, "%.2f");
        if (ImGui.isItemDeactivatedAfterEdit()) {
            ImGuiManager.INSTANCE.setUIScale(pendingViewScale[0]);
        } else if (!ImGui.isItemActive()) {
            // Not being dragged — keep in sync with actual current scale.
            pendingViewScale[0] = ImGuiManager.INSTANCE.getUIScale();
        }
    }

    private void renderEditMenu() {
        ClientEditHistory history = ClientEditHistory.INSTANCE;

        String undoName = history.peekUndoName();
        String undoLabel = undoName != null ? I18n.format("dimensium.settings.keybind.undo") + " " + undoName
            : I18n.format("dimensium.settings.keybind.undo");
        if (ImGui.menuItem(
            undoLabel,
            shortcut(Dimensium.actionUndo.getKeyCode(), Dimensium.actionUndoMods),
            false,
            undoName != null)) {
            EditorActions.undo();
        }

        String redoName = history.peekRedoName();
        String redoLabel = redoName != null ? I18n.format("dimensium.settings.keybind.redo") + " " + redoName
            : I18n.format("dimensium.settings.keybind.redo");
        if (ImGui.menuItem(
            redoLabel,
            shortcut(Dimensium.actionRedo.getKeyCode(), Dimensium.actionRedoMods),
            false,
            redoName != null)) {
            EditorActions.redo();
        }

        ImGui.separator();

        boolean hasSel = SelectionState.INSTANCE.hasSelection();

        if (ImGui.menuItem(
            I18n.format("dimensium.settings.keybind.cut"),
            shortcut(Dimensium.actionCut.getKeyCode(), Dimensium.actionCutMods),
            false,
            hasSel)) {
            EditorActions.cut();
        }

        if (ImGui.menuItem(
            I18n.format("dimensium.settings.keybind.copy"),
            shortcut(Dimensium.actionCopy.getKeyCode(), Dimensium.actionCopyMods),
            false,
            hasSel)) {
            EditorActions.copy();
        }

        ImGui.separator();

        SelectionState sel = SelectionState.INSTANCE;
        boolean hasClipboard = sel.clipboard != null && !sel.clipboard.isEmpty();
        if (ImGui.menuItem(
            I18n.format("dimensium.blueprint.save"),
            shortcut(Dimensium.actionSaveBlueprint.getKeyCode(), Dimensium.actionSaveBlueprintMods),
            false,
            hasClipboard)) {
            EditorActions.saveBlueprint();
        }
    }

    private void renderSelectMenu() {
        SelectionState sel = SelectionState.INSTANCE;
        boolean hasSel = sel.hasSelection();

        if (ImGui.menuItem(I18n.format("dimensium.select.clear"), null, false, hasSel)) {
            sel.clearSelection();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.select.move"), null, false, hasSel)) {
            DimensiumMode.INSTANCE.selectedTool = Tool.MOVE;
        }
        if (ImGui.menuItem(I18n.format("dimensium.select.filter"), null, false, hasSel)) {
            FilterSelectionWindow.INSTANCE.open();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.select.expand"), null, false, hasSel)) {
            sel.applyOp(SelectionTransforms.expand(sel.getSelectedBlocks(), 1), BooleanOp.REPLACE);
        }
        if (ImGui.menuItem(I18n.format("dimensium.select.shrink"), null, false, hasSel)) {
            sel.applyOp(SelectionTransforms.shrink(sel.getSelectedBlocks(), 1), BooleanOp.REPLACE);
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.select.distort"), null, false, hasSel)) {
            DistortSelectionWindow.INSTANCE.open();
        }
        if (ImGui.menuItem(I18n.format("dimensium.select.smooth"), null, false, hasSel)) {
            SmoothSelectionWindow.INSTANCE.open();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.format("dimensium.select.bounding_box"), null, false, hasSel)) {
            sel.applyOp(
                SelectionState.aabbBlocks(sel.minX(), sel.minY(), sel.minZ(), sel.maxX(), sel.maxY(), sel.maxZ()),
                BooleanOp.REPLACE);
        }
        if (ImGui.menuItem(I18n.format("dimensium.select.convex_hull"), null, false, hasSel)) {
            sel.applyOp(SelectionTransforms.convexHull(sel.getSelectedBlocks()), BooleanOp.REPLACE);
        }
    }

    private static String shortcut(int key, int mods) {
        StringBuilder sb = new StringBuilder();
        if ((mods & Dimensium.MOD_CTRL) != 0) sb.append("Ctrl+");
        if ((mods & Dimensium.MOD_SHIFT) != 0) sb.append("Shift+");
        if ((mods & Dimensium.MOD_ALT) != 0) sb.append("Alt+");
        sb.append(Keyboard.getKeyName(key));
        return sb.toString();
    }
}
