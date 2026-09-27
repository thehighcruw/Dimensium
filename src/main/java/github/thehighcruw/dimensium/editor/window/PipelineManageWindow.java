/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.PipelineLibrary;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class PipelineManageWindow extends ToggleableWindow {

    public static final PipelineManageWindow INSTANCE = new PipelineManageWindow();

    private static final String WINDOW_ID = "###pipeline_manage";

    private PipelineGraph renamingGraph = null;
    private boolean renameFocusPending = false;
    private final ImString renameBuffer = new ImString(128);
    private String renamingFolderOriginal = null;
    private final ImString folderRenameBuffer = new ImString(128);

    private PipelineGraph pendingDelete = null;
    private String pendingDeleteFolder = null;
    private final ImString newFolderBuffer = new ImString(128);

    // Drag-drop state
    private PipelineGraph dragPipeline = null;
    private String dragFolder = null;

    private PipelineManageWindow() {}

    public void open() {
        setOpen(true);
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowPipelineManageOpen(value);
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImBoolean pOpen = new ImBoolean(true);

        ImGui.setNextWindowSize(360f * scale, 460f * scale, ImGuiCond.FirstUseEver);
        ImGui.begin(I18n.format("dimensium.pipeline.manage.title") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();
        if (pOpen.get()) renderContent(scale);
        ImGui.end();

        if (!pOpen.get()) setOpen(false);

        applyPendingDeletions();
    }

    private void renderContent(float scale) {
        PipelineLibrary lib = PipelineLibrary.INSTANCE;
        List<PipelineGraph> all = lib.all();

        List<PipelineGraph> rootGraphs = new ArrayList<>();
        List<String> folders = lib.allFolders();
        for (PipelineGraph g : all) {
            if (g.folder == null) rootGraphs.add(g);
        }

        // Root drop target — move pipeline/folder to root
        ImGui.dummy(ImGui.getContentRegionAvailX(), 3f);
        if (ImGui.beginDragDropTarget()) {
            byte[] payload = ImGui.acceptDragDropPayload("PIPELINE");
            if (payload != null && dragPipeline != null) {
                lib.moveToFolder(dragPipeline, null);
                dragPipeline = null;
            }
            ImGui.endDragDropTarget();
        }

        for (PipelineGraph g : rootGraphs) {
            renderRow(g, scale);
        }

        renderFolderLevel(null, folders, all, scale);

        ImGui.separator();
        ImGui.setNextItemWidth(120f * scale);
        ImGui.inputTextWithHint("##newFolder", I18n.format("dimensium.pipeline.manage.folder_name"), newFolderBuffer);
        ImGui.sameLine();
        if (ImGui.button(I18n.format("dimensium.pipeline.manage.new_folder"))) {
            String folderName = newFolderBuffer.get().trim();
            if (!folderName.isEmpty()) {
                lib.createFolder(folderName);
                newFolderBuffer.set("");
            }
        }
    }

    private void renderFolderLevel(
            String parentPath, List<String> allFolderPaths, List<PipelineGraph> all, float scale) {
        PipelineLibrary lib = PipelineLibrary.INSTANCE;
        Set<String> segments = getDirectChildSegments(parentPath, allFolderPaths);
        for (String segment : segments) {
            String fullPath = parentPath == null ? segment : parentPath + "/" + segment;
            int flags = ImGuiTreeNodeFlags.SpanAvailWidth | ImGuiTreeNodeFlags.DefaultOpen;

            if (renamingFolderOriginal != null && renamingFolderOriginal.equals(fullPath)) {
                renderFolderRenameInput(fullPath, all);
            } else {
                boolean node = ImGui.treeNodeEx("##folder_" + fullPath, flags, segment);

                boolean isBuiltinFolder = lib.isBuiltinFolder(fullPath);
                if (!isBuiltinFolder && ImGui.beginDragDropSource()) {
                    dragFolder = fullPath;
                    ImGui.setDragDropPayload("FOLDER", new byte[] {1});
                    ImGui.text(segment);
                    ImGui.endDragDropSource();
                }

                if (ImGui.beginDragDropTarget()) {
                    byte[] pipelinePayload = ImGui.acceptDragDropPayload("PIPELINE");
                    if (pipelinePayload != null && dragPipeline != null) {
                        lib.moveToFolder(dragPipeline, fullPath);
                        dragPipeline = null;
                    }
                    byte[] folderPayload = ImGui.acceptDragDropPayload("FOLDER");
                    if (folderPayload != null && dragFolder != null && !dragFolder.equals(fullPath)) {
                        String sourceFolder = dragFolder;
                        dragFolder = null;
                        for (PipelineGraph g : lib.all()) {
                            if (sourceFolder.equals(g.folder)) lib.moveToFolder(g, fullPath);
                        }
                        lib.removeExplicitFolder(sourceFolder);
                    }
                    ImGui.endDragDropTarget();
                }

                renderFolderContextMenu(fullPath, all);
                if (node) {
                    renderFolderLevel(fullPath, allFolderPaths, all, scale);
                    for (PipelineGraph g : all) {
                        if (fullPath.equals(g.folder)) renderRow(g, scale);
                    }
                    ImGui.treePop();
                }
            }
        }
    }

    private Set<String> getDirectChildSegments(String parentPath, List<String> allFolderPaths) {
        String prefix = parentPath == null ? "" : parentPath + "/";
        Set<String> segments = new LinkedHashSet<>();
        for (String folder : allFolderPaths) {
            if (!folder.startsWith(prefix)) continue;
            String rest = folder.substring(prefix.length());
            int slash = rest.indexOf('/');
            segments.add(slash == -1 ? rest : rest.substring(0, slash));
        }
        return segments;
    }

    private void renderRow(PipelineGraph graph, float scale) {
        PipelineLibrary lib = PipelineLibrary.INSTANCE;
        boolean isBuiltin = lib.isBuiltin(graph);

        if (renamingGraph == graph) {
            renderRenameInput(graph);
            return;
        }

        ImGui.pushID(System.identityHashCode(graph));
        ImGui.selectable(
                graph.name + (isBuiltin ? " [" + I18n.format("dimensium.ui.pipeline.builtin") + "]" : ""), false);
        renderRowContextMenu(graph, isBuiltin);

        if (ImGui.beginDragDropSource()) {
            dragPipeline = graph;
            ImGui.setDragDropPayload("PIPELINE", new byte[] {1});
            ImGui.text(graph.name);
            ImGui.endDragDropSource();
        }

        if (!isBuiltin && ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
            startRename(graph);
        }
        ImGui.popID();
    }

    private void renderRowContextMenu(PipelineGraph graph, boolean isBuiltin) {
        PipelineLibrary lib = PipelineLibrary.INSTANCE;
        if (ImGui.beginPopupContextItem("##rowctx")) {
            if (ImGui.menuItem(I18n.format("dimensium.pipeline.manage.open_in_editor"))) {
                PipelineEditorWindow.INSTANCE.open(graph);
            }
            if (isBuiltin) ImGui.beginDisabled();
            if (ImGui.menuItem(I18n.format("dimensium.pipeline.manage.rename"))) {
                startRename(graph);
            }
            if (ImGui.beginMenu(I18n.format("dimensium.pipeline.manage.move_to_folder"))) {
                if (ImGui.menuItem(I18n.format("dimensium.pipeline.manage.folder_root"))) {
                    lib.moveToFolder(graph, null);
                }
                for (String folder : lib.allFolders()) {
                    if (ImGui.menuItem(folder)) {
                        lib.moveToFolder(graph, folder);
                    }
                }
                ImGui.endMenu();
            }
            if (ImGui.menuItem(I18n.format("dimensium.pipeline.manage.delete"))) {
                pendingDelete = graph;
            }
            if (isBuiltin) ImGui.endDisabled();
            ImGui.endPopup();
        }
    }

    private void renderFolderContextMenu(String folder, List<PipelineGraph> all) {
        if (ImGui.beginPopupContextItem("##folderctx_" + folder)) {
            boolean isBuiltinFolder = PipelineLibrary.INSTANCE.isBuiltinFolder(folder);
            if (isBuiltinFolder) ImGui.beginDisabled();
            if (ImGui.menuItem(I18n.format("dimensium.pipeline.manage.rename_folder"))) {
                renamingFolderOriginal = folder;
                folderRenameBuffer.set(folder);
                renameFocusPending = true;
            }
            if (ImGui.menuItem(I18n.format("dimensium.pipeline.manage.delete_folder"))) {
                pendingDeleteFolder = folder;
            }
            if (isBuiltinFolder) ImGui.endDisabled();
            ImGui.endPopup();
        }
    }

    private void renderRenameInput(PipelineGraph graph) {
        if (renameFocusPending) {
            ImGui.setKeyboardFocusHere();
            renameFocusPending = false;
        }
        boolean done = ImGui.inputText(
                "##rename", renameBuffer, ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.AutoSelectAll);
        if (done || ImGui.isItemDeactivated()) {
            String newName = renameBuffer.get().trim();
            if (!newName.isEmpty()) {
                PipelineLibrary.INSTANCE.rename(graph, newName);
            }
            renamingGraph = null;
        }
    }

    private void renderFolderRenameInput(String folder, List<PipelineGraph> all) {
        if (renameFocusPending) {
            ImGui.setKeyboardFocusHere();
            renameFocusPending = false;
        }
        boolean done = ImGui.inputText(
                "##folderrename",
                folderRenameBuffer,
                ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.AutoSelectAll);
        if (done || ImGui.isItemDeactivated()) {
            String newFolder = folderRenameBuffer.get().trim();
            if (!newFolder.isEmpty() && !newFolder.equals(folder)) {
                PipelineLibrary lib = PipelineLibrary.INSTANCE;
                for (PipelineGraph g : all) {
                    if (folder.equals(g.folder)) lib.moveToFolder(g, newFolder);
                }
            }
            renamingFolderOriginal = null;
        }
    }

    private void startRename(PipelineGraph graph) {
        renamingGraph = graph;
        renameBuffer.set(graph.name);
        renameFocusPending = true;
    }

    private void applyPendingDeletions() {
        PipelineLibrary lib = PipelineLibrary.INSTANCE;
        if (pendingDelete != null) {
            lib.delete(pendingDelete);
            pendingDelete = null;
        }
        if (pendingDeleteFolder != null) {
            for (PipelineGraph g : lib.all()) {
                if (pendingDeleteFolder.equals(g.folder)) lib.moveToFolder(g, null);
            }
            lib.removeExplicitFolder(pendingDeleteFolder);
            pendingDeleteFolder = null;
        }
    }
}
