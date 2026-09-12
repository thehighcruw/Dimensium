package github.thehighcruw.dimensium.render.panel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.render.panel.sections.DistortSection;
import github.thehighcruw.dimensium.render.panel.sections.ElevationSection;
import github.thehighcruw.dimensium.render.panel.sections.ExtrudeSection;
import github.thehighcruw.dimensium.render.panel.sections.FillSection;
import github.thehighcruw.dimensium.render.panel.sections.FreehandSection;
import github.thehighcruw.dimensium.render.panel.sections.FreehandSelectSection;
import github.thehighcruw.dimensium.render.panel.sections.GradientSection;
import github.thehighcruw.dimensium.render.panel.sections.LassoSelectSection;
import github.thehighcruw.dimensium.render.panel.sections.MagicSelectSection;
import github.thehighcruw.dimensium.render.panel.sections.MeltSection;
import github.thehighcruw.dimensium.render.panel.sections.ModellingSection;
import github.thehighcruw.dimensium.render.panel.sections.MoveSection;
import github.thehighcruw.dimensium.render.panel.sections.NoiseSection;
import github.thehighcruw.dimensium.render.panel.sections.PainterSection;
import github.thehighcruw.dimensium.render.panel.sections.PathSection;
import github.thehighcruw.dimensium.render.panel.sections.RockSection;
import github.thehighcruw.dimensium.render.panel.sections.RoughenSection;
import github.thehighcruw.dimensium.render.panel.sections.RulerSection;
import github.thehighcruw.dimensium.render.panel.sections.SculptSection;
import github.thehighcruw.dimensium.render.panel.sections.SelectSection;
import github.thehighcruw.dimensium.render.panel.sections.ShapeSection;
import github.thehighcruw.dimensium.render.panel.sections.ShatterSection;
import github.thehighcruw.dimensium.render.panel.sections.SmoothSection;
import github.thehighcruw.dimensium.render.panel.sections.StampSection;
import github.thehighcruw.dimensium.render.panel.sections.WeldSection;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.Tool;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class ToolPanel extends ImGuiWindow {

    public int currentW = 400;
    public static final int MIN_W = 200;
    public static final int MAX_W = 700;

    private boolean open = true;

    @Override
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowToolPanelOpen(value);
    }

    // ── Tool categories ───────────────────────────────────────────────────────

    private static String[] categoryNames() {
        return new String[] { I18n.format("dimensium.ui.category.selecting"),
            I18n.format("dimensium.ui.category.creating"), I18n.format("dimensium.ui.category.painting"),
            I18n.format("dimensium.ui.category.manipulating"), I18n.format("dimensium.ui.category.utility") };
    }

    static final Tool[][] CATEGORY_TOOLS = {
        { Tool.POINTER, Tool.SELECT, Tool.MAGIC_SELECT, Tool.FREEHAND_SELECT, Tool.LASSO_SELECT },
        { Tool.FREEHAND_DRAW, Tool.SCULPT_DRAW, Tool.SHAPE, Tool.FILL, Tool.STAMP, Tool.PATH, Tool.MODELLING,
            Tool.ROCK },
        { Tool.PAINTER, Tool.NOISE, Tool.GRADIENT }, { Tool.SMOOTH, Tool.WELD, Tool.MELT, Tool.ROUGHEN, Tool.EXTRUDE,
            Tool.MOVE, Tool.ELEVATION, Tool.DISTORT, Tool.SHATTER },
        { Tool.RULER } };

    // ── Per-tool registry ─────────────────────────────────────────────────────

    static final class ToolEntry {

        final ToolSection section;
        final float r, g, b;

        ToolEntry(ToolSection section, float r, float g, float b) {
            this.section = section;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static Map<Tool, ToolEntry> buildToolMap() {
        ToolStates s = ToolStates.INSTANCE;
        Map<Tool, ToolEntry> map = new LinkedHashMap<>();
        map.put(Tool.SELECT, new ToolEntry(new SelectSection(s.select), 0.20f, 0.85f, 0.75f));
        map.put(Tool.MAGIC_SELECT, new ToolEntry(new MagicSelectSection(s.magicSelect, s.select), 0.75f, 0.35f, 1.00f));
        map.put(Tool.FREEHAND_SELECT, new ToolEntry(new FreehandSelectSection(s.brush), 0.20f, 0.85f, 0.75f));
        map.put(Tool.LASSO_SELECT, new ToolEntry(new LassoSelectSection(s.lassoSelect), 0.20f, 0.85f, 0.75f));
        map.put(Tool.STAMP, new ToolEntry(new StampSection(s.stamp), 0.90f, 0.65f, 0.20f));
        map.put(Tool.FREEHAND_DRAW, new ToolEntry(new FreehandSection(s.freehand, s.brush), 0.24f, 0.50f, 1.00f));
        map.put(Tool.SCULPT_DRAW, new ToolEntry(new SculptSection(s.sculpt, s.brush), 0.08f, 0.65f, 0.80f));
        map.put(Tool.PAINTER, new ToolEntry(new PainterSection(s.painter, s.brush), 0.24f, 0.50f, 1.00f));
        map.put(Tool.NOISE, new ToolEntry(new NoiseSection(s.noise, s.brush, s.palette), 1.00f, 0.58f, 0.20f));
        map.put(Tool.GRADIENT, new ToolEntry(new GradientSection(s.gradient, s.brush, s.palette), 0.62f, 0.28f, 1.00f));
        map.put(Tool.ROCK, new ToolEntry(new RockSection(s.rock, s.brush), 0.55f, 0.42f, 0.28f));
        map.put(Tool.SMOOTH, new ToolEntry(new SmoothSection(s.smooth, s.brush), 0.20f, 0.78f, 0.72f));
        map.put(Tool.SHAPE, new ToolEntry(new ShapeSection(s.shape), 0.28f, 0.78f, 0.30f));
        map.put(Tool.FILL, new ToolEntry(new FillSection(s.floodfill), 1.00f, 0.78f, 0.10f));
        map.put(Tool.EXTRUDE, new ToolEntry(new ExtrudeSection(s.extrude), 1.00f, 0.28f, 0.68f));
        map.put(Tool.MOVE, new ToolEntry(new MoveSection(), 0.95f, 0.70f, 0.15f));
        map.put(Tool.PATH, new ToolEntry(new PathSection(s.path), 0.55f, 0.85f, 1.00f));
        map.put(Tool.MODELLING, new ToolEntry(new ModellingSection(s.modelling), 0.80f, 0.55f, 0.90f));
        map.put(Tool.ELEVATION, new ToolEntry(new ElevationSection(s.elevation), 0.40f, 0.72f, 0.30f));
        map.put(Tool.DISTORT, new ToolEntry(new DistortSection(s.distort, s.brush), 0.85f, 0.45f, 0.90f));
        map.put(Tool.WELD, new ToolEntry(new WeldSection(s.weld, s.brush), 0.60f, 0.80f, 0.70f));
        map.put(Tool.MELT, new ToolEntry(new MeltSection(s.melt, s.brush), 0.05f, 0.90f, 0.65f));
        map.put(Tool.ROUGHEN, new ToolEntry(new RoughenSection(s.roughen, s.brush), 0.75f, 0.55f, 0.30f));
        map.put(Tool.SHATTER, new ToolEntry(new ShatterSection(s.shatter, s.brush), 0.60f, 0.65f, 0.75f));
        map.put(Tool.RULER, new ToolEntry(new RulerSection(s.ruler), 0.40f, 0.85f, 0.55f));
        return Collections.unmodifiableMap(map);
    }

    final Map<Tool, ToolEntry> toolMap = buildToolMap();
    private final ImInt catIdx = new ImInt(0);
    private final ImInt toolIdx = new ImInt(0);

    /** No-op. Mouse is now managed by ImGui. */
    public void updateMouse(int mx, int my) {}

    public void render(int sw, int sh) {
        if (!open) return;
        float menuH = github.thehighcruw.dimensium.render.MenuBar.INSTANCE.height();
        float scale = github.thehighcruw.dimensium.render.imgui.ImGuiManager.INSTANCE.getUIScale();
        float physW = this.currentW * scale;
        ImGui.setNextWindowPos(0, menuH, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(physW, sh - menuH, ImGuiCond.FirstUseEver);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f * scale, 8f * scale);
        ImBoolean pOpen = new ImBoolean(true);
        ImGui.begin(I18n.format("dimensium.ui.panel.tools"), pOpen);
        captureBounds();
        if (!pOpen.get()) {
            setOpen(false);
            ImGui.end();
            ImGui.popStyleVar();
            return;
        }

        String[] cats = categoryNames();
        int currentCat = categoryFor(DimensiumMode.INSTANCE.selectedTool);
        catIdx.set(currentCat);
        ImGui.setNextItemWidth(-1);
        if (ImGui.combo("##cat", catIdx, cats) && catIdx.get() != currentCat) {
            DimensiumMode.INSTANCE.selectedTool = CATEGORY_TOOLS[catIdx.get()][0];
        }

        int cat = categoryFor(DimensiumMode.INSTANCE.selectedTool);
        Tool[] tools = CATEGORY_TOOLS[cat];
        String[] toolLabels = new String[tools.length];
        int selTool = 0;
        for (int i = 0; i < tools.length; i++) {
            toolLabels[i] = I18n.format(tools[i].label);
            if (tools[i] == DimensiumMode.INSTANCE.selectedTool) selTool = i;
        }
        toolIdx.set(selTool);
        ImGui.setNextItemWidth(-1);
        if (ImGui.combo("##tool", toolIdx, toolLabels)) {
            DimensiumMode.INSTANCE.selectedTool = tools[toolIdx.get()];
        }

        ImGui.end();
        ImGui.popStyleVar();
    }

    private static int categoryFor(Tool tool) {
        for (int c = 0; c < CATEGORY_TOOLS.length; c++) {
            for (Tool ct : CATEGORY_TOOLS[c]) {
                if (ct == tool) return c;
            }
        }
        return 0;
    }
}
