package github.thehighcruw.dimensium.render.popup;

import java.util.List;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.tool.mask.MaskEntry;
import github.thehighcruw.dimensium.tool.mask.MaskFolder;
import github.thehighcruw.dimensium.tool.mask.ToolMask;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class ToolMaskListWindow extends ImGuiWindow {

    public static final ToolMaskListWindow INSTANCE = new ToolMaskListWindow();

    private static final String WINDOW_ID = "###tool_mask_list";

    private boolean open = false;
    private MaskEntry renamingEntry = null;
    private boolean renameFocusPending = false;
    private final ImString renameBuffer = new ImString(128);
    private MaskEntry pendingDelete = null;

    // List drag-drop state
    private MaskEntry listDragEntry = null;
    private List<MaskEntry> listDragSourceList = null;

    private ToolMaskListWindow() {}

    public void open() {
        setOpen(true);
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowToolMaskListOpen(value);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImBoolean pOpen = new ImBoolean(true);

        ImGui.setNextWindowSize(300f * scale, 400f * scale, ImGuiCond.FirstUseEver);
        ImGui.begin(I18n.format("dimensium.mask.list.title") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();
        if (pOpen.get()) renderContent(scale);
        ImGui.end();

        if (!pOpen.get()) {
            setOpen(false);
            ToolMaskRegistry.INSTANCE.save();
        }

        if (pendingDelete != null) {
            ToolMaskRegistry.INSTANCE.remove(pendingDelete);
            pendingDelete = null;
        }
    }

    private void renderContent(float scale) {
        ToolMaskRegistry registry = ToolMaskRegistry.INSTANCE;

        // None option (also a drop target to move entries out of folders)
        boolean noneActive = registry.getActiveMask() == null;
        if (noneActive) ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.4f, 0.9f, 0.4f, 1f);
        if (ImGui.selectable(I18n.format("dimensium.mask.list.none"), noneActive)) {
            registry.setActiveMask(null);
        }
        if (noneActive) ImGui.popStyleColor();

        // Root-level drop target: move entry out of any folder to top level
        if (ImGui.beginDragDropTarget()) {
            byte[] payload = ImGui.acceptDragDropPayload("MASK_LIST_ENTRY");
            if (payload != null && listDragEntry != null) {
                applyListDrop(registry.entries);
            }
            ImGui.endDragDropTarget();
        }

        ImGui.separator();

        renderEntryList(registry.entries, scale, 0);

        ImGui.separator();

        if (ImGui.button(I18n.format("dimensium.mask.list.new_mask"))) {
            registry.createMask(I18n.format("dimensium.mask.list.default_name"));
        }
        ImGui.sameLine();
        if (ImGui.button(I18n.format("dimensium.mask.list.new_folder"))) {
            registry.createFolder(I18n.format("dimensium.mask.list.default_folder"));
        }
    }

    private void renderEntryList(List<MaskEntry> entries, float scale, int depth) {
        renderPositionalDropTarget(entries, 0);
        for (int i = 0; i < entries.size(); i++) {
            MaskEntry entry = entries.get(i);
            ImGui.pushID(System.identityHashCode(entry));

            if (entry instanceof MaskFolder) {
                renderFolder((MaskFolder) entry, entries, scale, depth);
            } else if (entry instanceof ToolMask) {
                renderMask((ToolMask) entry, entries, scale);
            }

            ImGui.popID();
            renderPositionalDropTarget(entries, i + 1);
        }
    }

    private void renderPositionalDropTarget(List<MaskEntry> targetList, int insertIndex) {
        ImGui.pushID(System.identityHashCode(targetList) * 31 + insertIndex);
        ImGui.dummy(ImGui.getContentRegionAvailX(), 3f);
        if (ImGui.beginDragDropTarget()) {
            byte[] payload = ImGui.acceptDragDropPayload("MASK_LIST_ENTRY");
            if (payload != null && listDragEntry != null) {
                applyPositionalDrop(targetList, insertIndex);
            }
            ImGui.endDragDropTarget();
        }
        ImGui.popID();
    }

    private void applyPositionalDrop(List<MaskEntry> targetList, int insertIndex) {
        int sourceIndex = listDragSourceList != null ? listDragSourceList.indexOf(listDragEntry) : -1;
        if (listDragSourceList != null) listDragSourceList.remove(listDragEntry);
        int adjusted = insertIndex;
        if (listDragSourceList == targetList && sourceIndex >= 0 && sourceIndex < insertIndex) adjusted--;
        adjusted = Math.max(0, Math.min(adjusted, targetList.size()));
        if (!targetList.contains(listDragEntry)) targetList.add(adjusted, listDragEntry);
        listDragEntry = null;
        listDragSourceList = null;
        ToolMaskRegistry.INSTANCE.save();
    }

    private void renderFolder(MaskFolder folder, List<MaskEntry> parentList, float scale, int depth) {
        if (renamingEntry == folder) {
            renderRenameInput(folder);
            return;
        }

        int flags = ImGuiTreeNodeFlags.SpanAvailWidth | ImGuiTreeNodeFlags.DefaultOpen;
        boolean node = ImGui.treeNodeEx(folder.getName(), flags);

        renderDragSource(folder, parentList);
        renderEntryContextMenu(folder);

        // Folder header is also a drop target (move into this folder)
        if (ImGui.beginDragDropTarget()) {
            byte[] payload = ImGui.acceptDragDropPayload("MASK_LIST_ENTRY");
            if (payload != null && listDragEntry != null && listDragEntry != folder) {
                applyListDrop(folder.entries);
            }
            ImGui.endDragDropTarget();
        }

        if (node) {
            renderEntryList(folder.entries, scale, depth + 1);
            ImGui.treePop();
        }
    }

    private void renderMask(ToolMask mask, List<MaskEntry> parentList, float scale) {
        ToolMaskRegistry registry = ToolMaskRegistry.INSTANCE;
        boolean isActive = registry.getActiveMask() == mask;

        if (renamingEntry == mask) {
            renderRenameInput(mask);
            return;
        }

        if (isActive) ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.4f, 0.9f, 0.4f, 1f);
        if (ImGui.selectable(mask.getName(), isActive)) {
            registry.setActiveMask(isActive ? null : mask);
        }
        if (isActive) ImGui.popStyleColor();

        renderDragSource(mask, parentList);
        renderEntryContextMenu(mask);

        if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
            startRename(mask);
        }
    }

    private void renderRenameInput(MaskEntry entry) {
        if (renameFocusPending) {
            ImGui.setKeyboardFocusHere();
            renameFocusPending = false;
        }
        boolean done = ImGui.inputText(
            "##rename",
            renameBuffer,
            ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.AutoSelectAll);
        if (done || ImGui.isItemDeactivated()) {
            String newName = renameBuffer.get()
                .trim();
            if (!newName.isEmpty()) entry.setName(newName);
            renamingEntry = null;
            ToolMaskRegistry.INSTANCE.save();
        }
    }

    private void startRename(MaskEntry entry) {
        renamingEntry = entry;
        renameBuffer.set(entry.getName());
        renameFocusPending = true;
    }

    private void renderDragSource(MaskEntry entry, List<MaskEntry> sourceList) {
        if (ImGui.beginDragDropSource()) {
            listDragEntry = entry;
            listDragSourceList = sourceList;
            ImGui.setDragDropPayload("MASK_LIST_ENTRY", new byte[] { 1 });
            ImGui.text(entry.getName());
            ImGui.endDragDropSource();
        }
    }

    private void applyListDrop(List<MaskEntry> targetList) {
        if (listDragSourceList != null) listDragSourceList.remove(listDragEntry);
        if (!targetList.contains(listDragEntry)) targetList.add(listDragEntry);
        listDragEntry = null;
        listDragSourceList = null;
        ToolMaskRegistry.INSTANCE.save();
    }

    private void renderEntryContextMenu(MaskEntry entry) {
        if (ImGui.beginPopupContextItem("##ctx")) {
            if (ImGui.menuItem(I18n.format("dimensium.mask.list.rename"))) {
                startRename(entry);
            }
            if (ImGui.menuItem(I18n.format("dimensium.mask.list.delete"))) {
                pendingDelete = entry;
            }
            ImGui.endPopup();
        }
    }
}
