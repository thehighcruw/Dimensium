/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.popup;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.PipelineLibrary;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.window.PipelineEditorWindow;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class PipelineLibraryPopup {

    public static final PipelineLibraryPopup INSTANCE = new PipelineLibraryPopup();

    private static final String POPUP_ID = "pipeline_library_modal";

    private static final float BASE_POPUP_W = 680f;
    private static final float BASE_POPUP_H = 500f;
    private static final float BASE_LIST_W = 220f;
    private static final float BASE_PAD = 8f;
    private static final float BASE_NODE_CARD_W = 130f;
    private static final float BASE_NODE_CARD_H = 28f;
    private static final float BASE_NODE_GAP = 20f;
    private static final float BASE_ARROW_W = 16f;

    // Port colours (ABGR)
    private static final int COL_SKELETON = 0xFF_00_AA_FF;
    private static final int COL_BLOCK_MAP = 0xFF_00_FF_88;
    private static final int COL_FLOAT = 0xFF_AA_AA_AA;
    private static final int COL_VEC3 = 0xFF_00_CC_FF;
    private static final int COL_CARD_BG = 0xFF_3A_3A_3A;
    private static final int COL_CARD_BORDER = 0xFF_66_66_66;
    private static final int COL_ARROW = 0xFF_99_99_99;
    private static final int COL_TEXT = 0xFF_EE_EE_EE;
    private static final int COL_BUILTIN_BADGE = 0xFF_55_66_AA;

    private boolean open = false;
    private boolean pendingOpen = false;
    private Consumer<PipelineGraph> selectionCallback = null;
    private int selectedIndex = -1;
    private final ImString nameFilter = new ImString(256);

    private PipelineLibraryPopup() {}

    /** Opens the popup; {@code callback} receives a deep-copy of the chosen graph. */
    public void open(Consumer<PipelineGraph> callback) {
        open = true;
        pendingOpen = true;
        selectionCallback = callback;
        selectedIndex = -1;
        nameFilter.set("");
    }

    public boolean isOpen() {
        return open;
    }

    public void renderImGui() {
        if (pendingOpen) {
            ImGui.openPopup(POPUP_ID);
            pendingOpen = false;
        }
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float popupW = BASE_POPUP_W * scale;
        float popupH = BASE_POPUP_H * scale;
        float listW = BASE_LIST_W * scale;
        float pad = BASE_PAD * scale;
        float panelH = popupH - 80f * scale;

        ImVec2 display = new ImVec2();
        ImGui.getIO().getDisplaySize(display);
        ImGui.setNextWindowPos((display.x - popupW) * 0.5f, (display.y - popupH) * 0.5f, ImGuiCond.Always);
        ImGui.setNextWindowSize(popupW, popupH, ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove;
        if (!ImGui.beginPopupModal(I18n.format("dimensium.pipeline.library.title") + "###" + POPUP_ID, flags)) {
            open = false;
            return;
        }

        if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
            closePopup();
        }

        List<PipelineGraph> all = PipelineLibrary.INSTANCE.all();
        String filter = nameFilter.get().trim().toLowerCase();

        // ── Left: search + list ───────────────────────────────────────────────
        ImGui.beginChild("##pl_left", listW, panelH, false);

        ImGui.setNextItemWidth(listW - pad);
        ImGui.inputTextWithHint("##plf", I18n.format("dimensium.pipeline.library.search"), nameFilter);
        ImGui.separator();

        int displayIdx = 0;
        for (int i = 0; i < all.size(); i++) {
            PipelineGraph graph = all.get(i);
            if (!filter.isEmpty() && !graph.name.toLowerCase().contains(filter)) continue;
            boolean selected = selectedIndex == i;
            String label = graph.name + "##pli" + i;
            if (ImGui.selectable(label, selected)) selectedIndex = i;
            if (PipelineLibrary.INSTANCE.isBuiltin(graph)) {
                ImGui.sameLine();
                ImGui.textDisabled("[" + I18n.format("dimensium.pipeline.library.builtin") + "]");
            }
            displayIdx++;
        }
        if (displayIdx == 0) {
            ImGui.textDisabled(I18n.format("dimensium.pipeline.library.empty"));
        }

        ImGui.endChild();

        // ── Right: preview ────────────────────────────────────────────────────
        ImGui.sameLine();
        float previewW = popupW - listW - pad * 3;
        ImGui.beginChild("##pl_right", previewW, panelH, true);

        if (selectedIndex >= 0 && selectedIndex < all.size()) {
            PipelineGraph graph = all.get(selectedIndex);
            ImGui.text(graph.name);
            if (PipelineLibrary.INSTANCE.isBuiltin(graph)) {
                ImGui.sameLine();
                ImGui.textDisabled("[" + I18n.format("dimensium.pipeline.library.builtin") + "]");
            }
            ImGui.text(I18n.format(
                    "dimensium.pipeline.library.node_count", graph.nodes().size()));
            ImGui.separator();
            ImGui.spacing();
            drawNodeChainPreview(graph, previewW, scale);
        } else {
            ImGui.textDisabled(I18n.format("dimensium.pipeline.library.select_hint"));
        }

        ImGui.endChild();

        // ── Bottom buttons ────────────────────────────────────────────────────
        ImGui.separator();
        ImGui.spacing();

        boolean hasSelection = selectedIndex >= 0 && selectedIndex < all.size();
        PipelineGraph selectedGraph = hasSelection ? all.get(selectedIndex) : null;

        if (!hasSelection) ImGui.beginDisabled();
        if (ImGui.button(I18n.format("dimensium.pipeline.library.add") + "##pla")) {
            if (selectedGraph != null) {
                if (selectionCallback != null) selectionCallback.accept(selectedGraph.deepCopy());
                closePopup();
            }
        }
        if (!hasSelection) ImGui.endDisabled();

        ImGui.sameLine();

        if (ImGui.button(I18n.format("dimensium.pipeline.library.new") + "##pln")) {
            PipelineGraph fresh =
                    PipelineLibrary.INSTANCE.createNew(I18n.format("dimensium.pipeline.library.new_name"));
            PipelineEditorWindow.INSTANCE.open(fresh);
            closePopup();
        }

        ImGui.sameLine();

        if (ImGui.button(I18n.format("dimensium.pipeline.library.cancel") + "##plc")) {
            closePopup();
        }

        ImGui.endPopup();
    }

    private void drawNodeChainPreview(PipelineGraph graph, float availW, float scale) {
        List<PipelineGraph.NodeInstance> nodes = graph.nodes();
        if (nodes.isEmpty()) {
            ImGui.textDisabled(I18n.format("dimensium.pipeline.library.no_nodes"));
            return;
        }

        float cardW = BASE_NODE_CARD_W * scale;
        float cardH = BASE_NODE_CARD_H * scale;
        float gap = BASE_NODE_GAP * scale;
        float arrowW = BASE_ARROW_W * scale;
        float pad = BASE_PAD * scale;
        float stepX = cardW + arrowW + gap;
        float stepY = cardH + 12f * scale;

        ImDrawList draw = ImGui.getWindowDrawList();
        ImVec2 cursor = new ImVec2();
        ImGui.getCursorScreenPos(cursor);

        float startX = cursor.x + pad;
        float startY = cursor.y;
        float x = startX;
        float y = startY;

        for (int i = 0; i < nodes.size(); i++) {
            PipelineGraph.NodeInstance inst = nodes.get(i);
            boolean isLast = i == nodes.size() - 1;
            boolean wouldOverflow = (x + cardW + (isLast ? 0 : arrowW + gap)) > (cursor.x + availW - pad);

            if (i > 0 && wouldOverflow) {
                x = startX;
                y += stepY + 4f * scale;
            }

            draw.addRectFilled(x, y, x + cardW, y + cardH, COL_CARD_BG, 4f * scale);
            draw.addRect(x, y, x + cardW, y + cardH, COL_CARD_BORDER, 4f * scale);

            int portColor = portColor(inst.typeId, true);
            draw.addRectFilled(x, y + 4f * scale, x + 3f * scale, y + cardH - 4f * scale, portColor, 2f * scale);

            String displayName = displayName(inst.typeId);
            draw.addText(x + 8f * scale, y + (cardH - ImGui.getFontSize()) * 0.5f, COL_TEXT, displayName);

            if (!isLast) {
                float arrowX = x + cardW + gap * 0.4f;
                float arrowY = y + cardH * 0.5f;
                draw.addLine(x + cardW, arrowY, arrowX + 6f * scale, arrowY, COL_ARROW, 1.5f * scale);
                draw.addTriangleFilled(
                        arrowX + 6f * scale,
                        arrowY - 4f * scale,
                        arrowX + 6f * scale,
                        arrowY + 4f * scale,
                        arrowX + 6f * scale + 7f * scale,
                        arrowY,
                        COL_ARROW);
            }

            x += stepX;
        }

        float totalRows = (float) Math.ceil(nodes.size() * stepX / (availW - pad * 2));
        ImGui.dummy(availW - pad * 2, Math.max(totalRows, 1) * stepY + 4f * scale);
    }

    private static int portColor(String typeId, boolean isInput) {
        try {
            NodeSchema schema = NodeRegistry.create(typeId).schema();
            PortType type = isInput
                    ? (schema.primaryInput() != null ? schema.primaryInput().type : null)
                    : (schema.primaryOutput() != null ? schema.primaryOutput().type : null);
            if (type == null) return COL_CARD_BORDER;
            switch (type) {
                case SKELETON:
                    return COL_SKELETON;
                case BLOCK_MAP:
                    return COL_BLOCK_MAP;
                case FLOAT:
                    return COL_FLOAT;
                case VEC3:
                    return COL_VEC3;
                default:
                    return COL_CARD_BORDER;
            }
        } catch (Exception ignored) {
        }
        return COL_CARD_BORDER;
    }

    private static String displayName(String typeId) {
        String[] parts = typeId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        // Truncate to fit card
        String result = sb.toString();
        return result.length() > 14 ? result.substring(0, 13) + "…" : result;
    }

    private void closePopup() {
        open = false;
        selectionCallback = null;
        ImGui.closeCurrentPopup();
    }
}
