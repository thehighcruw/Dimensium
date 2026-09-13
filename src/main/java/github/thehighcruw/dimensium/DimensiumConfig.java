/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

@Config(modid = Dimensium.MODID)
public class DimensiumConfig {

    @Config.Comment("Maximum side length (per dimension) of a selection that can be copied. Sparse storage — only non-air blocks are held in memory.")
    @Config.RangeInt(min = 1, max = 1000)
    public static int maxCopyVolume = 256;

    @Config.Comment("Maximum brush radius.")
    @Config.RangeInt(min = 1, max = 100)
    public static int maxBrushRadius = 100;

    @Config.Comment("Maximum raycast distance in blocks for all editor tools (block picking, brush preview, selection).")
    @Config.RangeInt(min = 64, max = 4096)
    public static int raycastDistance = 1024;

    @Config.Comment("Near-clip offset in blocks applied to the raycast start to avoid hitting the block the camera is inside.")
    @Config.RangeFloat(min = 0.01f, max = 1.0f)
    public static float raycastNearClip = 0.1f;

    @Config.Comment("Ghost-block count threshold above which the shape preview falls back to bounding-box rendering.")
    @Config.RangeInt(min = 1000, max = 500000)
    public static int maxGhostBlocks = 100000;

    @Config.Comment("Maximum number of blocks placed per smear step before the operation is cut off.")
    @Config.RangeInt(min = 1024, max = 1048576)
    public static int smearBlockCap = 65536;

    @Config.Comment("Per-block selection rendering switches to bounding-box outline above this count.")
    @Config.RangeInt(min = 256, max = 500000)
    public static int maxSelectionRenderBlocks = 100000;

    @Config.Comment("Maximum on-disk size of the per-player editing history, in megabytes.")
    @Config.RangeInt(min = 1, max = 1024)
    public static int editHistoryMaxMb = 10;

    @Config.Comment("ImGui UI scale factor.")
    @Config.RangeFloat(min = 0.5f, max = 3.0f)
    public static float uiScale = 1.0f;

    @Config.Comment("Rotation gizmo snap increment in degrees. Set to 0 to disable snapping.")
    @Config.RangeFloat(min = 0.0f, max = 45.0f)
    public static float rotationSnapDegrees = 1.0f;

    @Config.Comment("Scroll speed multiplier applied to brush-radius scrolling and freecam zoom scrolling.")
    @Config.RangeFloat(min = 0.1f, max = 10.0f)
    public static float worldScrollSpeedModifier = 1.0f;

    @Config.Comment("Scroll speed multiplier applied to UI scrollbars.")
    @Config.RangeFloat(min = 0.1f, max = 10.0f)
    public static float uiScrollSpeedModifier = 1.0f;

    public static void setWorldScrollSpeedModifier(float value) {
        worldScrollSpeedModifier = value;
    }

    public static void setUiScrollSpeedModifier(float value) {
        uiScrollSpeedModifier = value;
    }

    public static void setRotationSnapDegrees(float value) {
        rotationSnapDegrees = value;
        save();
    }

    // ── Editor view keybinds — key codes (LWJGL) and modifier masks (MOD_CTRL=1, MOD_SHIFT=2, MOD_ALT=4) ──
    @Config.Comment("Editor: Select tool — key code")
    public static int keyToolSelect = org.lwjgl.input.Keyboard.KEY_B;
    @Config.Comment("Editor: Select tool — modifier mask")
    public static int modsToolSelect = 0;

    @Config.Comment("Editor: Draw tool — key code")
    public static int keyToolDraw = org.lwjgl.input.Keyboard.KEY_G;
    @Config.Comment("Editor: Draw tool — modifier mask")
    public static int modsToolDraw = 0;

    @Config.Comment("Editor: Noise tool — key code")
    public static int keyToolNoise = org.lwjgl.input.Keyboard.KEY_O;
    @Config.Comment("Editor: Noise tool — modifier mask")
    public static int modsToolNoise = 0;

    @Config.Comment("Editor: Smooth tool — key code")
    public static int keyToolSmooth = org.lwjgl.input.Keyboard.KEY_U;
    @Config.Comment("Editor: Smooth tool — modifier mask")
    public static int modsToolSmooth = 0;

    @Config.Comment("Editor: Extrude tool — key code")
    public static int keyToolExtrude = org.lwjgl.input.Keyboard.KEY_Z;
    @Config.Comment("Editor: Extrude tool — modifier mask")
    public static int modsToolExtrude = 0;

    @Config.Comment("Editor: Pointer tool — key code")
    public static int keyToolPointer = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Pointer tool — modifier mask")
    public static int modsToolPointer = 0;

    @Config.Comment("Editor: Magic Select tool — key code")
    public static int keyToolMagicSelect = org.lwjgl.input.Keyboard.KEY_M;
    @Config.Comment("Editor: Magic Select tool — modifier mask")
    public static int modsToolMagicSelect = 0;

    @Config.Comment("Editor: Freehand Select tool — key code")
    public static int keyToolFreehandSelect = org.lwjgl.input.Keyboard.KEY_N;
    @Config.Comment("Editor: Freehand Select tool — modifier mask")
    public static int modsToolFreehandSelect = 0;

    @Config.Comment("Editor: Lasso Select tool — key code")
    public static int keyToolLassoSelect = org.lwjgl.input.Keyboard.KEY_L;
    @Config.Comment("Editor: Lasso Select tool — modifier mask")
    public static int modsToolLassoSelect = 0;

    @Config.Comment("Editor: Sculpt Draw tool — key code")
    public static int keyToolSculptDraw = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Sculpt Draw tool — modifier mask")
    public static int modsToolSculptDraw = 0;

    @Config.Comment("Editor: Shape tool — key code")
    public static int keyToolShape = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Shape tool — modifier mask")
    public static int modsToolShape = 0;

    @Config.Comment("Editor: Stamp tool — key code")
    public static int keyToolStamp = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Stamp tool — modifier mask")
    public static int modsToolStamp = 0;

    @Config.Comment("Editor: Fill tool — key code")
    public static int keyToolFill = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Fill tool — modifier mask")
    public static int modsToolFill = 0;

    @Config.Comment("Editor: Painter tool — key code")
    public static int keyToolPainter = org.lwjgl.input.Keyboard.KEY_P;
    @Config.Comment("Editor: Painter tool — modifier mask")
    public static int modsToolPainter = 0;

    @Config.Comment("Editor: Rock tool — key code")
    public static int keyToolRock = org.lwjgl.input.Keyboard.KEY_H;
    @Config.Comment("Editor: Rock tool — modifier mask")
    public static int modsToolRock = 0;

    @Config.Comment("Editor: Gradient tool — key code")
    public static int keyToolGradient = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Gradient tool — modifier mask")
    public static int modsToolGradient = 0;

    @Config.Comment("Editor: Move tool — key code")
    public static int keyToolMove = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Move tool — modifier mask")
    public static int modsToolMove = 0;

    @Config.Comment("Editor: Path tool — key code")
    public static int keyToolPath = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Path tool — modifier mask")
    public static int modsToolPath = 0;

    @Config.Comment("Editor: Elevation tool — key code")
    public static int keyToolElevation = org.lwjgl.input.Keyboard.KEY_E;
    @Config.Comment("Editor: Elevation tool — modifier mask")
    public static int modsToolElevation = 0;

    @Config.Comment("Editor: Distort tool — key code")
    public static int keyToolDistort = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Distort tool — modifier mask")
    public static int modsToolDistort = 0;

    @Config.Comment("Editor: Weld tool — key code")
    public static int keyToolWeld = org.lwjgl.input.Keyboard.KEY_J;
    @Config.Comment("Editor: Weld tool — modifier mask")
    public static int modsToolWeld = 0;

    @Config.Comment("Editor: Melt tool — key code")
    public static int keyToolMelt = org.lwjgl.input.Keyboard.KEY_K;
    @Config.Comment("Editor: Melt tool — modifier mask")
    public static int modsToolMelt = 0;

    @Config.Comment("Editor: Roughen tool — key code")
    public static int keyToolRoughen = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Roughen tool — modifier mask")
    public static int modsToolRoughen = 0;

    @Config.Comment("Editor: Shatter tool — key code")
    public static int keyToolShatter = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Shatter tool — modifier mask")
    public static int modsToolShatter = 0;

    @Config.Comment("Editor: Ruler tool — key code")
    public static int keyToolRuler = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Ruler tool — modifier mask")
    public static int modsToolRuler = 0;

    @Config.Comment("Editor: Modelling tool — key code")
    public static int keyToolModelling = org.lwjgl.input.Keyboard.KEY_NONE;
    @Config.Comment("Editor: Modelling tool — modifier mask")
    public static int modsToolModelling = 0;

    @Config.Comment("Editor: Undo — key code")
    public static int keyActionUndo = org.lwjgl.input.Keyboard.KEY_Z;
    @Config.Comment("Editor: Undo — modifier mask")
    public static int modsActionUndo = 1; // MOD_CTRL

    @Config.Comment("Editor: Redo — key code")
    public static int keyActionRedo = org.lwjgl.input.Keyboard.KEY_Y;
    @Config.Comment("Editor: Redo — modifier mask")
    public static int modsActionRedo = 1; // MOD_CTRL

    @Config.Comment("Editor: Copy — key code")
    public static int keyActionCopy = org.lwjgl.input.Keyboard.KEY_C;
    @Config.Comment("Editor: Copy — modifier mask")
    public static int modsActionCopy = 1; // MOD_CTRL

    @Config.Comment("Editor: Cut — key code")
    public static int keyActionCut = org.lwjgl.input.Keyboard.KEY_X;
    @Config.Comment("Editor: Cut — modifier mask")
    public static int modsActionCut = 1; // MOD_CTRL

    @Config.Comment("Editor: Paste — key code")
    public static int keyActionPaste = org.lwjgl.input.Keyboard.KEY_V;
    @Config.Comment("Editor: Paste — modifier mask")
    public static int modsActionPaste = 1; // MOD_CTRL

    @Config.Comment("Editor: Fill — key code")
    public static int keyActionFill = org.lwjgl.input.Keyboard.KEY_F;
    @Config.Comment("Editor: Fill — modifier mask")
    public static int modsActionFill = 1; // MOD_CTRL

    @Config.Comment("Editor: Erase — key code")
    public static int keyActionErase = org.lwjgl.input.Keyboard.KEY_DELETE;
    @Config.Comment("Editor: Erase — modifier mask")
    public static int modsActionErase = 0;

    @Config.Comment("Editor: Confirm placement — key code")
    public static int keyActionConfirm = org.lwjgl.input.Keyboard.KEY_RETURN;
    @Config.Comment("Editor: Confirm placement — modifier mask")
    public static int modsActionConfirm = 0;

    @Config.Comment("Editor: Save blueprint — key code")
    public static int keyActionSaveBlueprint = org.lwjgl.input.Keyboard.KEY_P;
    @Config.Comment("Editor: Save blueprint — modifier mask")
    public static int modsActionSaveBlueprint = 1; // MOD_CTRL

    @Config.Comment("Editor: Blueprint browser — key code")
    public static int keyActionBlueprintBrowser = org.lwjgl.input.Keyboard.KEY_B;
    @Config.Comment("Editor: Blueprint browser — modifier mask")
    public static int modsActionBlueprintBrowser = 1; // MOD_CTRL

    @Config.Comment("Editor: Open settings — key code")
    public static int keyActionSettings = org.lwjgl.input.Keyboard.KEY_PERIOD;
    @Config.Comment("Editor: Open settings — modifier mask")
    public static int modsActionSettings = 1; // MOD_CTRL

    @Config.Comment("Gizmo: Nudge forwards — key code")
    public static int keyGizmoNudgeForward = org.lwjgl.input.Keyboard.KEY_UP;
    @Config.Comment("Gizmo: Nudge forwards — modifier mask")
    public static int modsGizmoNudgeForward = 0;

    @Config.Comment("Gizmo: Nudge backwards — key code")
    public static int keyGizmoNudgeBackward = org.lwjgl.input.Keyboard.KEY_DOWN;
    @Config.Comment("Gizmo: Nudge backwards — modifier mask")
    public static int modsGizmoNudgeBackward = 0;

    @Config.Comment("Gizmo: Nudge right — key code")
    public static int keyGizmoNudgeRight = org.lwjgl.input.Keyboard.KEY_RIGHT;
    @Config.Comment("Gizmo: Nudge right — modifier mask")
    public static int modsGizmoNudgeRight = 0;

    @Config.Comment("Gizmo: Nudge left — key code")
    public static int keyGizmoNudgeLeft = org.lwjgl.input.Keyboard.KEY_LEFT;
    @Config.Comment("Gizmo: Nudge left — modifier mask")
    public static int modsGizmoNudgeLeft = 0;

    @Config.Comment("Gizmo: Nudge up (+Y) — key code")
    public static int keyGizmoNudgeUp = org.lwjgl.input.Keyboard.KEY_PRIOR;
    @Config.Comment("Gizmo: Nudge up (+Y) — modifier mask")
    public static int modsGizmoNudgeUp = 0;

    @Config.Comment("Gizmo: Nudge down (-Y) — key code")
    public static int keyGizmoNudgeDown = org.lwjgl.input.Keyboard.KEY_NEXT;
    @Config.Comment("Gizmo: Nudge down (-Y) — modifier mask")
    public static int modsGizmoNudgeDown = 0;

    @Config.Comment("Camera movement speed multiplier. 1.0 = normal player walk speed.")
    @Config.RangeFloat(min = 0.1f, max = 20.0f)
    public static float movementSpeedMultiplier = 1.0f;

    public static void setMovementSpeedMultiplier(float value) {
        movementSpeedMultiplier = value;
    }

    @Config.Comment("When true, Ctrl/Option+LMB orbits around the block under the cursor. When false, orbits around the block at the crosshair (screen center).")
    public static boolean orbitUseCursor = true;

    public static void setOrbitUseCursor(boolean value) {
        orbitUseCursor = value;
    }

    // ── Window open state — persisted across restarts ──
    @Config.Comment("History window open state.")
    public static boolean windowHistoryOpen = true;

    @Config.Comment("Tool Mask List window open state.")
    public static boolean windowToolMaskListOpen = true;

    @Config.Comment("Tool Mask Editor window open state.")
    public static boolean windowToolMaskEditorOpen = true;

    @Config.Comment("Palette window open state.")
    public static boolean windowPaletteOpen = true;

    @Config.Comment("Palette Editor window open state.")
    public static boolean windowPaletteEditorOpen = true;

    @Config.Comment("Analyze window open state.")
    public static boolean windowAnalyzeOpen = false;

    @Config.Comment("Autoshade window open state.")
    public static boolean windowAutoshadeOpen = false;

    @Config.Comment("Tool panel open state.")
    public static boolean windowToolPanelOpen = true;

    @Config.Comment("Tool Options panel open state.")
    public static boolean windowToolOptionsPanelOpen = true;

    @Config.Comment("Properties panel open state.")
    public static boolean windowRightPanelOpen = true;

    @Config.Comment("Block info window open state.")
    public static boolean windowBlockInfoOpen = true;

    @Config.Comment("Selection window open state.")
    public static boolean windowSelectionOpen = true;

    @Config.Comment("Operations window open state.")
    public static boolean windowOperationsOpen = true;

    @Config.Comment("Clipboard window open state.")
    public static boolean windowClipboardOpen = true;

    // ── Window state setters — call these instead of writing fields directly ──

    public static void setWindowHistoryOpen(boolean v) {
        windowHistoryOpen = v;
        save();
    }

    public static void setWindowToolMaskListOpen(boolean v) {
        windowToolMaskListOpen = v;
        save();
    }

    public static void setWindowToolMaskEditorOpen(boolean v) {
        windowToolMaskEditorOpen = v;
        save();
    }

    public static void setWindowPaletteOpen(boolean v) {
        windowPaletteOpen = v;
        save();
    }

    public static void setWindowPaletteEditorOpen(boolean v) {
        windowPaletteEditorOpen = v;
        save();
    }

    public static void setWindowAnalyzeOpen(boolean v) {
        windowAnalyzeOpen = v;
        save();
    }

    public static void setWindowAutoshadeOpen(boolean v) {
        windowAutoshadeOpen = v;
        save();
    }

    public static void setWindowToolPanelOpen(boolean v) {
        windowToolPanelOpen = v;
        save();
    }

    public static void setWindowToolOptionsPanelOpen(boolean v) {
        windowToolOptionsPanelOpen = v;
        save();
    }

    public static void setWindowBlockInfoOpen(boolean v) {
        windowBlockInfoOpen = v;
        save();
    }

    public static void setWindowSelectionOpen(boolean v) {
        windowSelectionOpen = v;
        save();
    }

    public static void setWindowOperationsOpen(boolean v) {
        windowOperationsOpen = v;
        save();
    }

    public static void setWindowClipboardOpen(boolean v) {
        windowClipboardOpen = v;
        save();
    }

    private static void save() {
        ConfigurationManager.save(DimensiumConfig.class);
    }
}
