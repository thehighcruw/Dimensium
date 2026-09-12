package github.thehighcruw.dimensium.render.popup;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.blueprint.Blueprint;
import github.thehighcruw.dimensium.blueprint.BlueprintIO;
import github.thehighcruw.dimensium.blueprint.BlueprintRegistry;
import github.thehighcruw.dimensium.render.world.ClipboardRenderer;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class CreateBlueprintPopup {

    public static final CreateBlueprintPopup INSTANCE = new CreateBlueprintPopup();

    private boolean pendingOpen = false;
    private boolean open = false;
    private final ImString nameField = new ImString(256);
    private final ImString tagsField = new ImString(512);
    private String statusMsg = null;

    private ClipboardRenderer clipRenderer = null;
    private float previewAzim = 225f;
    private float previewElev = 28f;
    private float previewZoom = 1.0f;

    private List<String> suggestedTags = new ArrayList<>();

    private static final float THUMB_SIZE = 220f;
    private static final float FORM_W = 240f;

    public void open(ClipboardRenderer renderer) {
        open = true;
        pendingOpen = true;
        nameField.set("");
        tagsField.set("");
        statusMsg = null;
        clipRenderer = renderer;
        previewAzim = 225f;
        previewElev = 28f;
        previewZoom = 1.0f;
        suggestedTags = loadSuggestedTags();
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        pendingOpen = false;
        clipRenderer = null;
        statusMsg = null;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void renderImGui(Minecraft mc) {
        if (!open) return;

        if (pendingOpen) {
            ImGui.openPopup("create_blueprint_modal");
            pendingOpen = false;
        }

        ImBoolean pOpen = new ImBoolean(true);
        if (ImGui.beginPopupModal(
            I18n.format("dimensium.blueprint.create.title") + "###create_blueprint_modal",
            pOpen,
            ImGuiWindowFlags.AlwaysAutoResize)) {

            if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
                close();
                ImGui.closeCurrentPopup();
                ImGui.endPopup();
                return;
            }

            // ── Left column: thumbnail viewport ──────────────────────────────
            int texId = clipRenderer != null ? clipRenderer.getTexture(SelectionState.INSTANCE) : -1;

            ImVec2 thumbPos = new ImVec2();
            ImGui.getCursorScreenPos(thumbPos);

            // invisibleButton owns the active/hover state so LMB drag works
            ImGui.invisibleButton("##thumb_interact", THUMB_SIZE, THUMB_SIZE);
            boolean thumbHovered = ImGui.isItemHovered();
            boolean thumbActive = ImGui.isItemActive();

            // Draw image or placeholder on top of the invisible button
            if (texId != -1) {
                ImGui.getWindowDrawList()
                    .addImage(
                        texId,
                        thumbPos.x,
                        thumbPos.y,
                        thumbPos.x + THUMB_SIZE,
                        thumbPos.y + THUMB_SIZE,
                        0f,
                        1f,
                        1f,
                        0f);
            } else {
                ImGui.getWindowDrawList()
                    .addRectFilled(
                        thumbPos.x,
                        thumbPos.y,
                        thumbPos.x + THUMB_SIZE,
                        thumbPos.y + THUMB_SIZE,
                        0xFF222233);
            }

            // Rotate hint overlay
            ImGui.getWindowDrawList()
                .addText(
                    thumbPos.x + 4f,
                    thumbPos.y + THUMB_SIZE - 14f,
                    0xAAFFFFFF,
                    I18n.format("dimensium.blueprint.create.thumb.hint"));

            // ── Mouse input ───────────────────────────────────────────────────
            if (thumbActive && ImGui.isMouseDragging(ImGuiMouseButton.Left, 1f)) {
                float dx = ImGui.getIO()
                    .getMouseDeltaX();
                float dy = ImGui.getIO()
                    .getMouseDeltaY();
                previewAzim += dx * 0.5f;
                previewElev = Math.max(-89f, Math.min(89f, previewElev + dy * 0.5f));
                if (clipRenderer != null) {
                    clipRenderer.setCamera(previewAzim, previewElev, previewZoom);
                }
            }

            if (thumbHovered) {
                float wheel = ImGui.getIO()
                    .getMouseWheel();
                if (wheel != 0f) {
                    previewZoom = Math.max(0.2f, Math.min(5f, previewZoom + wheel * 0.1f));
                    if (clipRenderer != null) {
                        clipRenderer.setCamera(previewAzim, previewElev, previewZoom);
                    }
                }
            }

            // ── Right column: form ────────────────────────────────────────────
            ImGui.sameLine();

            ImVec2 formStart = new ImVec2();
            ImGui.getCursorScreenPos(formStart);
            // Push cursor to top of thumbnail row
            ImGui.setCursorScreenPos(formStart.x, thumbPos.y);

            ImGui.beginGroup();

            ImGui.text(I18n.format("dimensium.blueprint.create.name"));
            ImGui.setNextItemWidth(FORM_W);
            ImGui.inputText("##bp_name", nameField);

            ImGui.spacing();

            ImGui.text(I18n.format("dimensium.blueprint.create.tags"));
            ImGui.setNextItemWidth(FORM_W);
            ImGui.inputText("##bp_tags", tagsField);
            ImGui.textDisabled(I18n.format("dimensium.blueprint.create.tags.hint"));

            if (statusMsg != null) {
                ImGui.textColored(1.0f, 0.4f, 0.26f, 1.0f, statusMsg);
            }

            ImGui.endGroup();

            // ── Suggested tag toggles ─────────────────────────────────────────
            if (!suggestedTags.isEmpty()) {
                ImGui.separator();
                ImGui.textDisabled(I18n.format("dimensium.blueprint.create.tags.suggest"));
                List<String> activeTags = parseTags(tagsField.get());
                for (String tag : suggestedTags) {
                    boolean active = activeTags.contains(tag);
                    if (active) {
                        ImGui.pushStyleColor(ImGuiCol.Button, 0.24f, 0.50f, 1.00f, 0.85f);
                    }
                    if (ImGui.smallButton(tag + "##suggest_" + tag)) {
                        toggleTag(tag);
                    }
                    if (active) {
                        ImGui.popStyleColor();
                    }
                    ImGui.sameLine(0, 4f);
                }
                ImGui.newLine();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.blueprint.create.save") + "##bp_save")) {
                trySave();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.format("dimensium.blueprint.create.cancel") + "##bp_cancel")) {
                close();
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        if (!pOpen.get()) {
            close();
        }
    }

    // ── Save logic ────────────────────────────────────────────────────────────

    private void trySave() {
        String name = nameField.get()
            .trim();
        if (name.isEmpty()) {
            statusMsg = I18n.format("dimensium.blueprint.create.error.noname");
            return;
        }
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.clipboard == null || sel.clipboard.isEmpty()) {
            statusMsg = I18n.format("dimensium.blueprint.create.error.noclipboard");
            return;
        }

        Blueprint bp = new Blueprint();
        bp.name = name;
        bp.clipW = sel.clipW;
        bp.clipH = sel.clipH;
        bp.clipD = sel.clipD;

        if (clipRenderer != null) {
            clipRenderer.capturePng();
            bp.thumbnailPng = clipRenderer.getLatestPng();
        }

        bp.tags.addAll(
            parseTags(
                tagsField.get()
                    .trim()));

        for (java.util.Map.Entry<Long, SelectionState.BlockData> e : sel.clipboard.entrySet()) {
            long key = e.getKey();
            int lx = (int) (key >> 20) & 0xFFFFF;
            int ly = (int) (key >> 10) & 0x3FF;
            int lz = (int) key & 0x3FF;
            SelectionState.BlockData bd = e.getValue();
            bp.offsets.add(new int[] { lx, ly, lz, net.minecraft.block.Block.getIdFromBlock(bd.block), bd.meta });
        }

        try {
            BlueprintIO.save(bp, BlueprintIO.getBlueprintsDir());
            BlueprintRegistry.INSTANCE.refresh();
            close();
            ImGui.closeCurrentPopup();
        } catch (Exception ex) {
            statusMsg = "Save failed: " + ex.getMessage();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void toggleTag(String tag) {
        List<String> current = parseTags(tagsField.get());
        if (current.contains(tag)) {
            current.remove(tag);
        } else {
            current.add(tag);
        }
        tagsField.set(String.join(", ", current));
    }

    private static List<String> parseTags(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim()
            .isEmpty()) return result;
        for (String t : raw.split(",")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private static List<String> loadSuggestedTags() {
        try {
            return BlueprintIO.collectAllTags(BlueprintIO.getBlueprintsDir());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
