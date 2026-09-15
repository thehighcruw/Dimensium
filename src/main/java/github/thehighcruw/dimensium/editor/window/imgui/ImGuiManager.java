/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.assertion.ImAssertCallback;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiPopupFlags;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import javax.annotation.Nonnull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public final class ImGuiManager {

    public static final ImGuiManager INSTANCE = new ImGuiManager();

    private ImGuiManager() {}

    private DimensiumImGuiGlRenderer glRenderer;
    private boolean initialized = false;
    private float uiScale = 1.0f;
    private float lastAppliedScale = 0f;

    public float getUIScale() {
        return uiScale;
    }

    public void setUIScale(float scale) {
        uiScale = Math.max(0.5f, Math.min(3.0f, scale));
        DimensiumConfig.uiScale = uiScale;
        ConfigurationManager.save(DimensiumConfig.class);
    }

    private final Queue<Character> pendingChars = new ArrayDeque<>();
    private final Queue<int[]> pendingKeyEvents = new ArrayDeque<>();
    private float pendingWheel = 0f;

    // Track modifier key state from key events — Keyboard.isKeyDown() is unreliable on macOS
    // (LWJGL 2 does not correctly report Ctrl held state on some macOS configurations).
    private boolean trackedLCtrl, trackedRCtrl;
    private boolean trackedLShift, trackedRShift;
    private boolean trackedLAlt, trackedRAlt;

    public boolean isInitialized() {
        return initialized;
    }

    private static final float FONT_SIZE_BASE_PX = 24f;
    private File fontTempFile = null;

    private void ensureFontFile() {
        if (fontTempFile != null && fontTempFile.exists()) return;
        try {
            fontTempFile = File.createTempFile("dimensium-font-", ".ttf");
            fontTempFile.deleteOnExit();
            try (InputStream in = ImGuiManager.class.getResourceAsStream("/assets/dimensium/fonts/Nunito.ttf");
                    FileOutputStream out = new FileOutputStream(fontTempFile)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = Objects.requireNonNull(in).read(buf)) != -1) out.write(buf, 0, n);
            }
        } catch (Exception e) {
            Dimensium.logger.error("Failed to extract font to temp file", e);
            fontTempFile = null;
        }
    }

    public void ensureInit() {
        if (initialized) return;
        ImGui.setAssertCallback(new ImAssertCallback() {

            @Override
            public void imAssertCallback(String expr, int line, String file) {
                Dimensium.logger.error(
                        "ImGui assertion failed: {} ({}:{})", expr, file, line, new RuntimeException("stack trace"));
                FMLCommonHandler.instance().exitJava(1, false);
            }
        });
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename("dimensium_layout.ini");
        io.addConfigFlags(ImGuiConfigFlags.NoMouseCursorChange);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        uiScale = DimensiumConfig.uiScale;
        lastAppliedScale = uiScale;
        ensureFontFile();
        buildFontAtlas(io);
        applyStyle();
        glRenderer = new DimensiumImGuiGlRenderer();
        glRenderer.init();
        initialized = true;
        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        () -> {
                            if (initialized) {
                                ImGui.saveIniSettingsToDisk("dimensium_layout.ini");
                                Dimensium.logger.error(
                                        "JVM exiting while ImGui is still active — likely a native crash. Check run/jvm-crash-*.log");
                            }
                        },
                        "imgui-crash-sentinel"));
    }

    private void buildFontAtlas(ImGuiIO io) {
        float sizePx = FONT_SIZE_BASE_PX * uiScale;
        ImFontAtlas atlas = io.getFonts();
        atlas.setFreeTypeRenderer(true);
        if (fontTempFile != null) {
            atlas.addFontFromFileTTF(fontTempFile.getAbsolutePath(), sizePx);
        } else {
            atlas.addFontDefault();
        }
    }

    private void rebuildFont() {
        // Rebuild font atlas in place — do NOT destroy/recreate the context.
        // Destroying the context while an ImGui popup is open (e.g. the View menu) triggers
        // an assertion in Dear ImGui's popup stack cleanup, crashing via the assert callback.
        // In-place rebuild also preserves docking layout without losing the .ini state.
        ImGuiIO io = ImGui.getIO();
        io.getFonts().clear();
        buildFontAtlas(io);
        glRenderer.rebuildFontTexture();
    }

    /**
     * ImGui display space = physical pixels (fbW×fbH), fbScale=1.
     * This keeps panel widths in physical pixels, independent of MC GUI scale.
     * cursorX/Y are in MC scaled pixels; multiplied by guiScale to get physical.
     */
    public void newFrame(int fbW, int fbH, int guiScale, float cursorX, float cursorY) {
        ensureInit();
        if (uiScale != lastAppliedScale) {
            rebuildFont();
            applyStyle();
            lastAppliedScale = uiScale;
        }
        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(fbW, fbH);
        io.setDisplayFramebufferScale(1f, 1f);
        io.setMousePos(cursorX * guiScale, cursorY * guiScale);
        // On macOS, Ctrl+LMB is remapped to RMB by the OS before LWJGL sees it.
        // Detect this case (tracked Ctrl held + RMB down but not LMB) and remap back to LMB+Ctrl.
        boolean trackedCtrl = trackedLCtrl || trackedRCtrl;
        boolean rmb = Mouse.isButtonDown(1);
        boolean macosCtrlClick = trackedCtrl && rmb && !Mouse.isButtonDown(0);
        io.setMouseDown(0, Mouse.isButtonDown(0) || macosCtrlClick);
        io.setMouseDown(1, rmb && !macosCtrlClick);
        io.setMouseDown(2, Mouse.isButtonDown(2));
        io.setMouseWheel(pendingWheel);
        pendingWheel = 0f;

        while (!pendingKeyEvents.isEmpty()) {
            int[] ev = pendingKeyEvents.poll();
            io.addKeyEvent(ev[0], ev[1] == 1);
        }

        while (!pendingChars.isEmpty()) {
            io.addInputCharacter(pendingChars.poll());
        }

        // Re-emit tracked modifier state every frame so Dear ImGui never sees a stale release.
        // Keyboard.isKeyDown() is unreliable on macOS (LWJGL 2), so we use state tracked from
        // key events in addKeyEvent() instead of polling.
        boolean ctrlHeld = trackedLCtrl || trackedRCtrl;
        io.addKeyEvent(ImGuiKey.LeftCtrl, trackedLCtrl);
        io.addKeyEvent(ImGuiKey.RightCtrl, trackedRCtrl);
        io.addKeyEvent(ImGuiKey.LeftShift, trackedLShift);
        io.addKeyEvent(ImGuiKey.RightShift, trackedRShift);
        io.addKeyEvent(ImGuiKey.LeftAlt, trackedLAlt);
        io.addKeyEvent(ImGuiKey.RightAlt, trackedRAlt);

        ImGui.newFrame();

        // NewFrame() recomputes io.Key* flags from its internal queue. On macOS, the physical
        // Ctrl key fires as LeftSuper in LWJGL 2, so NewFrame sets KeySuper=true, KeyCtrl=false.
        // Fix: read what NewFrame computed, remap Super→Ctrl when we tracked Ctrl via key events,
        // then write all four flags and KeyMods from one consistent value so Render()'s
        // assertion (KeyMods == derived(KeyCtrl|KeyShift|KeyAlt|KeySuper)) always passes.
        int mods = io.getKeyMods();
        if (ctrlHeld) {
            mods = (mods & ~ImGuiKey.ImGuiMod_Super) | ImGuiKey.ImGuiMod_Ctrl;
        }
        io.setKeyMods(mods);
        io.setKeyCtrl((mods & ImGuiKey.ImGuiMod_Ctrl) != 0);
        io.setKeyShift((mods & ImGuiKey.ImGuiMod_Shift) != 0);
        io.setKeyAlt((mods & ImGuiKey.ImGuiMod_Alt) != 0);
        io.setKeySuper((mods & ImGuiKey.ImGuiMod_Super) != 0);
    }

    public void endFrame() {
        ImGui.render();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        glRenderer.renderDrawData(ImGui.getDrawData());

        GL11.glPopAttrib();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    public void addChar(char c) {
        pendingChars.offer(c);
    }

    public void addKeyEvent(int lwjglKey, boolean down) {
        int imguiKey = lwjglToImGui(lwjglKey);
        if (imguiKey == -1) return;
        pendingKeyEvents.add(new int[] {imguiKey, down ? 1 : 0});
        switch (lwjglKey) {
            case Keyboard.KEY_LCONTROL:
                trackedLCtrl = down;
                break;
            case Keyboard.KEY_RCONTROL:
                trackedRCtrl = down;
                break;
            case Keyboard.KEY_LSHIFT:
                trackedLShift = down;
                break;
            case Keyboard.KEY_RSHIFT:
                trackedRShift = down;
                break;
            case Keyboard.KEY_LMENU:
                trackedLAlt = down;
                break;
            case Keyboard.KEY_RMENU:
                trackedRAlt = down;
                break;
            default:
                break;
        }
    }

    private int lwjglToImGui(int key) {
        return LwjglKeyMap.toImGui(key);
    }

    public void addMouseWheel(float delta) {
        pendingWheel += delta;
    }

    public boolean wantCaptureMouse() {
        return initialized && ImGui.getIO().getWantCaptureMouse();
    }

    public boolean wantCaptureKeyboard() {
        return initialized && ImGui.getIO().getWantCaptureKeyboard();
    }

    public boolean anyModalOpen() {
        return initialized && ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup);
    }

    private void applyStyle() {
        ImGui.styleColorsDark();
        ImGuiStyle style = getStyle();

        style.setColor(ImGuiCol.Text, 1.00f, 1.00f, 1.00f, 1.00f);
        style.setColor(ImGuiCol.WindowBg, 0.055f, 0.055f, 0.078f, 1.00f);
        style.setColor(ImGuiCol.PopupBg, 0.055f, 0.055f, 0.078f, 0.97f);
        style.setColor(ImGuiCol.ChildBg, 0.000f, 0.000f, 0.000f, 0.00f);
        style.setColor(ImGuiCol.FrameBg, 0.10f, 0.10f, 0.16f, 0.95f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.20f, 0.20f, 0.27f, 0.95f);
        style.setColor(ImGuiCol.FrameBgActive, 0.24f, 0.50f, 1.00f, 0.24f);
        style.setColor(ImGuiCol.TitleBg, 0.055f, 0.055f, 0.078f, 1.00f);
        style.setColor(ImGuiCol.TitleBgActive, 0.055f, 0.055f, 0.078f, 1.00f);
        style.setColor(ImGuiCol.Button, 0.13f, 0.13f, 0.18f, 0.95f);
        style.setColor(ImGuiCol.ButtonHovered, 0.20f, 0.20f, 0.27f, 0.95f);
        style.setColor(ImGuiCol.ButtonActive, 0.24f, 0.50f, 1.00f, 0.40f);
        style.setColor(ImGuiCol.Header, 0.13f, 0.13f, 0.18f, 0.95f);
        style.setColor(ImGuiCol.HeaderHovered, 0.20f, 0.20f, 0.27f, 0.95f);
        style.setColor(ImGuiCol.HeaderActive, 0.24f, 0.50f, 1.00f, 0.40f);
        style.setColor(ImGuiCol.SliderGrab, 0.24f, 0.50f, 1.00f, 0.70f);
        style.setColor(ImGuiCol.SliderGrabActive, 0.24f, 0.50f, 1.00f, 1.00f);
        style.setColor(ImGuiCol.CheckMark, 0.24f, 0.50f, 1.00f, 1.00f);
        style.setColor(ImGuiCol.Separator, 1.00f, 1.00f, 1.00f, 0.07f);
        style.setColor(ImGuiCol.MenuBarBg, 0.055f, 0.055f, 0.078f, 0.97f);
        style.setColor(ImGuiCol.ScrollbarBg, 0.055f, 0.055f, 0.078f, 0.60f);
        style.setColor(ImGuiCol.ScrollbarGrab, 0.20f, 0.20f, 0.27f, 1.00f);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.30f, 0.30f, 0.40f, 1.00f);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.24f, 0.50f, 1.00f, 1.00f);
    }

    @Nonnull
    private ImGuiStyle getStyle() {
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(0f);
        style.setChildRounding(0f);
        style.setFrameRounding(0f);
        style.setScrollbarRounding(0f);
        style.setGrabRounding(0f);
        style.setPopupRounding(0f);
        style.setTabRounding(0f);
        style.setWindowBorderSize(0f);
        style.setDisplaySafeAreaPadding(0f, 0f);
        style.setFramePadding(6f * uiScale, 3f * uiScale);
        style.setItemSpacing(6f * uiScale, 4f * uiScale);
        style.setWindowPadding(8f * uiScale, 8f * uiScale);
        style.setScrollbarSize(14f * uiScale);
        style.setGrabMinSize(10f * uiScale);
        return style;
    }
}
