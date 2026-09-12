/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import java.util.EnumMap;

import net.minecraft.client.settings.KeyBinding;

import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import github.thehighcruw.dimensium.handler.GradientGuiHandler;
import github.thehighcruw.dimensium.history.ServerCaptureQueue;
import github.thehighcruw.dimensium.history.ServerEditQueue;
import github.thehighcruw.dimensium.network.PacketHandler;
import github.thehighcruw.dimensium.proxy.IProxy;
import github.thehighcruw.dimensium.tool.Tool;

@Mod(
    modid = Dimensium.MODID,
    name = Dimensium.NAME,
    version = Dimensium.VERSION,
    dependencies = "required-after:gtnhlib")
public class Dimensium {

    public static final String MODID = "dimensium";
    public static final String NAME = "Dimensium";
    public static final String VERSION = "1.0.0";

    @Instance(MODID)
    public static Dimensium instance;

    public static Logger logger;

    @SidedProxy(
        clientSide = "github.thehighcruw.dimensium.proxy.ClientProxy",
        serverSide = "github.thehighcruw.dimensium.proxy.ServerProxy")
    public static IProxy proxy;

    // Modifier flag constants (bitmask).
    public static final int MOD_CTRL = 1;
    public static final int MOD_SHIFT = 2;
    public static final int MOD_ALT = 4;

    // Global keybind — registered with MC so it appears in the controls menu.
    public static KeyBinding toggleDimensium = new KeyBinding("key.dimensium.toggle", Keyboard.KEY_RSHIFT, "Dimensium");

    // Tool-switch keybinds — one per Tool enum value, populated by applyKeybinds().
    public static final EnumMap<Tool, KeyBinding> toolKeybinds = buildToolKeybinds();
    public static final EnumMap<Tool, Integer> toolKeybindMods = new EnumMap<>(Tool.class);

    // Editor-view action keybinds — NOT registered with MC; configurable via Settings > Keybinds.
    public static KeyBinding actionUndo = new KeyBinding("key.dimensium.undo", Keyboard.KEY_Z, "Dimensium");
    public static KeyBinding actionRedo = new KeyBinding("key.dimensium.redo", Keyboard.KEY_Y, "Dimensium");
    public static KeyBinding actionCopy = new KeyBinding("key.dimensium.copy", Keyboard.KEY_C, "Dimensium");
    public static KeyBinding actionCut = new KeyBinding("key.dimensium.cut", Keyboard.KEY_X, "Dimensium");
    public static KeyBinding actionPaste = new KeyBinding("key.dimensium.paste", Keyboard.KEY_V, "Dimensium");
    public static KeyBinding actionFill = new KeyBinding("key.dimensium.fill", Keyboard.KEY_F, "Dimensium");
    public static KeyBinding actionErase = new KeyBinding("key.dimensium.erase", Keyboard.KEY_DELETE, "Dimensium");
    public static KeyBinding actionConfirm = new KeyBinding("key.dimensium.confirm", Keyboard.KEY_RETURN, "Dimensium");
    public static KeyBinding actionSaveBlueprint = new KeyBinding(
        "key.dimensium.save_blueprint",
        Keyboard.KEY_P,
        "Dimensium");
    public static KeyBinding actionBlueprintBrowser = new KeyBinding(
        "key.dimensium.blueprint_browser",
        Keyboard.KEY_B,
        "Dimensium");
    public static KeyBinding actionSettings = new KeyBinding(
        "key.dimensium.settings",
        Keyboard.KEY_PERIOD,
        "Dimensium");

    // Modifier masks for action keybinds.
    public static int actionUndoMods = MOD_CTRL;
    public static int actionRedoMods = MOD_CTRL;
    public static int actionCopyMods = MOD_CTRL;
    public static int actionCutMods = MOD_CTRL;
    public static int actionPasteMods = MOD_CTRL;
    public static int actionFillMods = MOD_CTRL;
    public static int actionEraseMods = 0;
    public static int actionConfirmMods = 0;
    public static int actionSaveBlueprintMods = MOD_CTRL;
    public static int actionBlueprintBrowserMods = MOD_CTRL;
    public static int actionSettingsMods = MOD_CTRL;

    private static EnumMap<Tool, KeyBinding> buildToolKeybinds() {
        EnumMap<Tool, KeyBinding> map = new EnumMap<>(Tool.class);
        for (Tool t : Tool.values()) {
            map.put(
                t,
                new KeyBinding(
                    "key.dimensium.tool." + t.name()
                        .toLowerCase(),
                    Keyboard.KEY_NONE,
                    "Dimensium"));
        }
        return map;
    }

    public static void applyKeybinds() {
        setToolKey(Tool.POINTER, DimensiumConfig.keyToolPointer, DimensiumConfig.modsToolPointer);
        setToolKey(Tool.SELECT, DimensiumConfig.keyToolSelect, DimensiumConfig.modsToolSelect);
        setToolKey(Tool.MAGIC_SELECT, DimensiumConfig.keyToolMagicSelect, DimensiumConfig.modsToolMagicSelect);
        setToolKey(Tool.FREEHAND_SELECT, DimensiumConfig.keyToolFreehandSelect, DimensiumConfig.modsToolFreehandSelect);
        setToolKey(Tool.LASSO_SELECT, DimensiumConfig.keyToolLassoSelect, DimensiumConfig.modsToolLassoSelect);
        setToolKey(Tool.FREEHAND_DRAW, DimensiumConfig.keyToolDraw, DimensiumConfig.modsToolDraw);
        setToolKey(Tool.SCULPT_DRAW, DimensiumConfig.keyToolSculptDraw, DimensiumConfig.modsToolSculptDraw);
        setToolKey(Tool.SHAPE, DimensiumConfig.keyToolShape, DimensiumConfig.modsToolShape);
        setToolKey(Tool.STAMP, DimensiumConfig.keyToolStamp, DimensiumConfig.modsToolStamp);
        setToolKey(Tool.FILL, DimensiumConfig.keyToolFill, DimensiumConfig.modsToolFill);
        setToolKey(Tool.PAINTER, DimensiumConfig.keyToolPainter, DimensiumConfig.modsToolPainter);
        setToolKey(Tool.NOISE, DimensiumConfig.keyToolNoise, DimensiumConfig.modsToolNoise);
        setToolKey(Tool.ROCK, DimensiumConfig.keyToolRock, DimensiumConfig.modsToolRock);
        setToolKey(Tool.GRADIENT, DimensiumConfig.keyToolGradient, DimensiumConfig.modsToolGradient);
        setToolKey(Tool.SMOOTH, DimensiumConfig.keyToolSmooth, DimensiumConfig.modsToolSmooth);
        setToolKey(Tool.EXTRUDE, DimensiumConfig.keyToolExtrude, DimensiumConfig.modsToolExtrude);
        setToolKey(Tool.MOVE, DimensiumConfig.keyToolMove, DimensiumConfig.modsToolMove);
        setToolKey(Tool.PATH, DimensiumConfig.keyToolPath, DimensiumConfig.modsToolPath);
        setToolKey(Tool.ELEVATION, DimensiumConfig.keyToolElevation, DimensiumConfig.modsToolElevation);
        setToolKey(Tool.DISTORT, DimensiumConfig.keyToolDistort, DimensiumConfig.modsToolDistort);
        setToolKey(Tool.WELD, DimensiumConfig.keyToolWeld, DimensiumConfig.modsToolWeld);
        setToolKey(Tool.MELT, DimensiumConfig.keyToolMelt, DimensiumConfig.modsToolMelt);
        setToolKey(Tool.ROUGHEN, DimensiumConfig.keyToolRoughen, DimensiumConfig.modsToolRoughen);
        setToolKey(Tool.SHATTER, DimensiumConfig.keyToolShatter, DimensiumConfig.modsToolShatter);
        setToolKey(Tool.RULER, DimensiumConfig.keyToolRuler, DimensiumConfig.modsToolRuler);
        setToolKey(Tool.MODELLING, DimensiumConfig.keyToolModelling, DimensiumConfig.modsToolModelling);
        actionUndo.setKeyCode(DimensiumConfig.keyActionUndo);
        actionUndoMods = DimensiumConfig.modsActionUndo;
        actionRedo.setKeyCode(DimensiumConfig.keyActionRedo);
        actionRedoMods = DimensiumConfig.modsActionRedo;
        actionCopy.setKeyCode(DimensiumConfig.keyActionCopy);
        actionCopyMods = DimensiumConfig.modsActionCopy;
        actionCut.setKeyCode(DimensiumConfig.keyActionCut);
        actionCutMods = DimensiumConfig.modsActionCut;
        actionPaste.setKeyCode(DimensiumConfig.keyActionPaste);
        actionPasteMods = DimensiumConfig.modsActionPaste;
        actionFill.setKeyCode(DimensiumConfig.keyActionFill);
        actionFillMods = DimensiumConfig.modsActionFill;
        actionErase.setKeyCode(DimensiumConfig.keyActionErase);
        actionEraseMods = DimensiumConfig.modsActionErase;
        actionConfirm.setKeyCode(DimensiumConfig.keyActionConfirm);
        actionConfirmMods = DimensiumConfig.modsActionConfirm;
        actionSaveBlueprint.setKeyCode(DimensiumConfig.keyActionSaveBlueprint);
        actionSaveBlueprintMods = DimensiumConfig.modsActionSaveBlueprint;
        actionBlueprintBrowser.setKeyCode(DimensiumConfig.keyActionBlueprintBrowser);
        actionBlueprintBrowserMods = DimensiumConfig.modsActionBlueprintBrowser;
        actionSettings.setKeyCode(DimensiumConfig.keyActionSettings);
        actionSettingsMods = DimensiumConfig.modsActionSettings;
    }

    private static void setToolKey(Tool t, int key, int mods) {
        toolKeybinds.get(t)
            .setKeyCode(key);
        toolKeybindMods.put(t, mods);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        try {
            ConfigurationManager.registerConfig(DimensiumConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register Dimensium config", e);
        }

        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GradientGuiHandler());
        PacketHandler.init();
        // Register on the common bus so draining fires on both integrated and dedicated server.
        FMLCommonHandler.instance()
            .bus()
            .register(ServerEditQueue.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ServerCaptureQueue.INSTANCE);
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

}
