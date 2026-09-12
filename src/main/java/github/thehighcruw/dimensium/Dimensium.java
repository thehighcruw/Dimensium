/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

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

    // Editor-view keybinds — NOT registered with MC; configurable via Settings > Keybinds.
    public static KeyBinding toolSelect = new KeyBinding("key.dimensium.tool.select", Keyboard.KEY_B, "Dimensium");
    public static KeyBinding toolDraw = new KeyBinding("key.dimensium.tool.draw", Keyboard.KEY_P, "Dimensium");
    public static KeyBinding toolNoise = new KeyBinding("key.dimensium.tool.noise", Keyboard.KEY_O, "Dimensium");
    public static KeyBinding toolSmooth = new KeyBinding("key.dimensium.tool.smooth", Keyboard.KEY_U, "Dimensium");
    public static KeyBinding toolExtrude = new KeyBinding("key.dimensium.tool.extrude", Keyboard.KEY_Z, "Dimensium");
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

    // Modifier masks for each editor-view keybind (parallel to the KeyBinding objects).
    public static int toolSelectMods = 0;
    public static int toolDrawMods = 0;
    public static int toolNoiseMods = 0;
    public static int toolSmoothMods = 0;
    public static int toolExtrudeMods = 0;
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

    public static void applyKeybinds() {
        toolSelect.setKeyCode(DimensiumConfig.keyToolSelect);
        toolSelectMods = DimensiumConfig.modsToolSelect;
        toolDraw.setKeyCode(DimensiumConfig.keyToolDraw);
        toolDrawMods = DimensiumConfig.modsToolDraw;
        toolNoise.setKeyCode(DimensiumConfig.keyToolNoise);
        toolNoiseMods = DimensiumConfig.modsToolNoise;
        toolSmooth.setKeyCode(DimensiumConfig.keyToolSmooth);
        toolSmoothMods = DimensiumConfig.modsToolSmooth;
        toolExtrude.setKeyCode(DimensiumConfig.keyToolExtrude);
        toolExtrudeMods = DimensiumConfig.modsToolExtrude;
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
