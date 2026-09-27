/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.Curve;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineExecutor;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.editor.window.viewport.world.PipelinePreviewRenderer;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class PipelinePreviewWindow extends ToggleableWindow {

    private static final float DEFAULT_AZIM = 225f;
    private static final float DEFAULT_ELEV = 28f;
    private static final float DEFAULT_ZOOM = 1.0f;

    public static final PipelinePreviewWindow INSTANCE = new PipelinePreviewWindow();

    private final PipelinePreviewRenderer renderer = new PipelinePreviewRenderer();
    private PipelineGraph graph;
    private long seed = ThreadLocalRandom.current().nextLong();
    private int lastRenderedRevision = -1;

    private float previewAzim = DEFAULT_AZIM;
    private float previewElev = DEFAULT_ELEV;
    private float previewZoom = DEFAULT_ZOOM;

    private boolean previewUpToSelected = false;

    private PipelinePreviewWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowPipelinePreviewOpen(value);
    }

    public void setGraph(PipelineGraph graph) {
        this.graph = graph;
    }

    /** Must be called each frame before ImGui rendering begins (while GL state is usable). */
    public void prebake() {
        String selectedNodeId = PipelineEditorWindow.INSTANCE.getSelectedNodeId();
        boolean needsRebake = graph != null
                && (graph.revision() != lastRenderedRevision
                        || (previewUpToSelected && selectedNodeIdChanged(selectedNodeId)));
        if (needsRebake) {
            lastRenderedRevision = graph.revision();
            lastPreviewedNodeId = selectedNodeId;
            runPreview(selectedNodeId);
        }
        renderer.maybeRebake();
    }

    private String lastPreviewedNodeId = null;

    private boolean selectedNodeIdChanged(String current) {
        if (current == null) return lastPreviewedNodeId != null;
        return !current.equals(lastPreviewedNodeId);
    }

    private void runPreview(String upToNodeId) {
        if (graph == null) return;
        try {
            PipelineContext ctx = new PipelineContext(Vec3DInt.ZERO, seed);
            if (previewUpToSelected && upToNodeId != null) {
                Object result = PipelineExecutor.executeUpTo(graph, ctx, upToNodeId);
                applyPreviewResult(result);
            } else {
                Object result = PipelineExecutor.executeForPreview(graph, ctx);
                applyPreviewResult(result);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyPreviewResult(Object result) {
        if (result instanceof BlockMap) {
            renderer.setBlockMap((BlockMap) result);
        } else if (result instanceof Skeleton) {
            renderer.setSkeleton((Skeleton) result);
        } else if (result instanceof Curve) {
            renderer.setCurve((Curve) result);
        } else {
            renderer.setBlockMap(null);
        }
    }

    public void renderImGui() {
        if (!open) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSize(340 * uiScale, 380 * uiScale, ImGuiCond.FirstUseEver);

        ImBoolean pOpen = new ImBoolean(true);
        if (ImGui.begin(
                I18n.format("dimensium.menu.window.pipeline_preview") + "##pipelinePreview",
                pOpen,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
            if (!pOpen.get()) {
                open = false;
                ImGui.end();
                return;
            }
            renderContent(uiScale);
        }
        ImGui.end();
    }

    private void renderContent(float uiScale) {
        String selectedNodeId = PipelineEditorWindow.INSTANCE.getSelectedNodeId();

        if (ImGui.button(I18n.format("dimensium.ui.pipeline.preview.refresh") + "##ppRefresh")) {
            lastRenderedRevision = graph != null ? graph.revision() : lastRenderedRevision;
            lastPreviewedNodeId = selectedNodeId;
            runPreview(selectedNodeId);
        }
        ImGui.sameLine();
        if (ImGui.button(I18n.format("dimensium.ui.pipeline.preview.randomize") + "##ppRandomize")) {
            seed = ThreadLocalRandom.current().nextLong();
            lastRenderedRevision = graph != null ? graph.revision() : lastRenderedRevision;
            lastPreviewedNodeId = selectedNodeId;
            runPreview(selectedNodeId);
        }
        ImGui.sameLine();
        ImBoolean upToSelected = new ImBoolean(previewUpToSelected);
        if (ImGui.checkbox(I18n.format("dimensium.ui.pipeline.preview.up_to_selected") + "##ppUTS", upToSelected)) {
            previewUpToSelected = upToSelected.get();
            lastPreviewedNodeId = null; // force rebake
        }

        ImGui.separator();

        int texId = renderer.getTexture();
        if (texId == -1) {
            if (renderer.fboFailed) {
                ImGui.textDisabled(renderer.failReason != null ? renderer.failReason : "FBO unavailable");
            } else {
                ImGui.textDisabled(I18n.format("dimensium.ui.pipeline.preview.empty"));
            }
            return;
        }

        float avail = ImGui.getContentRegionAvailX();
        float size = Math.min(avail, ImGui.getContentRegionAvailY());

        ImVec2 imagePos = new ImVec2();
        ImGui.getCursorScreenPos(imagePos);

        ImGui.invisibleButton("##ppOrbit", size, size);
        boolean hovered = ImGui.isItemHovered();
        boolean active = ImGui.isItemActive();

        ImGui.getWindowDrawList()
                .addImage(texId, imagePos.x, imagePos.y, imagePos.x + size, imagePos.y + size, 0f, 1f, 1f, 0f);

        if (active && ImGui.isMouseDragging(ImGuiMouseButton.Left, 1f)) {
            float deltax = ImGui.getIO().getMouseDeltaX();
            float deltay = ImGui.getIO().getMouseDeltaY();
            previewAzim += deltax * 0.5f;
            previewElev = Math.max(-89f, Math.min(89f, previewElev + deltay * 0.5f));
            renderer.setCamera(previewAzim, previewElev, previewZoom);
        }

        if (hovered) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0f) {
                previewZoom = Math.max(0.2f, Math.min(5f, previewZoom + wheel * 0.15f));
                renderer.setCamera(previewAzim, previewElev, previewZoom);
            }
        }
    }
}
