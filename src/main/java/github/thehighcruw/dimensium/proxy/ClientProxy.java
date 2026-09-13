/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.proxy;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.blueprint.BlueprintRegistry;
import github.thehighcruw.dimensium.editor.handler.KeyHandler;
import github.thehighcruw.dimensium.editor.handler.TickHandler;
import github.thehighcruw.dimensium.editor.overlay.LayoutPresetRegistry;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.tool.state.PaletteRegistry;
import github.thehighcruw.dimensium.editor.window.AnalyzeWindow;
import github.thehighcruw.dimensium.editor.window.AutoshadeWindow;
import github.thehighcruw.dimensium.editor.window.BlockInfoWindow;
import github.thehighcruw.dimensium.editor.window.ClipboardWindow;
import github.thehighcruw.dimensium.editor.window.ColourFieldWindow;
import github.thehighcruw.dimensium.editor.window.DistortSelectionWindow;
import github.thehighcruw.dimensium.editor.window.FillSelectionWindow;
import github.thehighcruw.dimensium.editor.window.FilterSelectionWindow;
import github.thehighcruw.dimensium.editor.window.HistoryWindow;
import github.thehighcruw.dimensium.editor.window.LayoutPresetManageWindow;
import github.thehighcruw.dimensium.editor.window.OperationsWindow;
import github.thehighcruw.dimensium.editor.window.PaletteEditorWindow;
import github.thehighcruw.dimensium.editor.window.PaletteWindow;
import github.thehighcruw.dimensium.editor.window.ReplaceSelectionWindow;
import github.thehighcruw.dimensium.editor.window.SelectionWindow;
import github.thehighcruw.dimensium.editor.window.SmoothSelectionWindow;
import github.thehighcruw.dimensium.editor.window.ToolMaskEditorWindow;
import github.thehighcruw.dimensium.editor.window.ToolMaskListWindow;
import github.thehighcruw.dimensium.editor.window.TypeReplaceSelectionWindow;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.InputHandler;
import github.thehighcruw.dimensium.world.inventory.CreativeGuiHandler;

@SideOnly(Side.CLIENT)
public class ClientProxy implements IProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // toggleDimensium is a global keybind — lives in the MC controls menu.
        ClientRegistry.registerKeyBinding(Dimensium.toggleDimensium);
        // Editor-view keybinds are configured in Settings > Keybinds, not registered with MC.
        Dimensium.applyKeybinds();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(SelectionRenderer.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new OverlayRenderer());
        MinecraftForge.EVENT_BUS.register(new InputHandler());
        MinecraftForge.EVENT_BUS.register(BlockColorCache.INSTANCE);
        CreativeGuiHandler.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new KeyHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new TickHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        BlueprintRegistry.INSTANCE.init();
        ToolMaskRegistry.INSTANCE.load();
        PaletteRegistry.INSTANCE.load();
        // Touch all floating window singletons so their constructors self-register
        // with ImGuiWindowRegistry before any mouse events can fire.
        // ToolPanel is instantiated in OverlayRenderer (which is registered
        // in init() above) — accessing it here forces its ImGuiWindow registration.
        @SuppressWarnings("unused")
        Object[] windows = { AnalyzeWindow.INSTANCE, BlockInfoWindow.INSTANCE, AutoshadeWindow.INSTANCE,
            LayoutPresetManageWindow.INSTANCE, ClipboardWindow.INSTANCE, ColourFieldWindow.INSTANCE,
            DistortSelectionWindow.INSTANCE, FillSelectionWindow.INSTANCE, FilterSelectionWindow.INSTANCE,
            HistoryWindow.INSTANCE, OperationsWindow.INSTANCE, ReplaceSelectionWindow.INSTANCE,
            SelectionWindow.INSTANCE, SmoothSelectionWindow.INSTANCE, TypeReplaceSelectionWindow.INSTANCE,
            ToolMaskListWindow.INSTANCE, ToolMaskEditorWindow.INSTANCE, PaletteWindow.INSTANCE,
            PaletteEditorWindow.INSTANCE, OverlayRenderer.TOOL_WINDOW, OverlayRenderer.TOOL_OPTIONS_WINDOW };

        if (DimensiumConfig.windowHistoryOpen) HistoryWindow.INSTANCE.setOpen(true);
        if (DimensiumConfig.windowToolMaskListOpen) ToolMaskListWindow.INSTANCE.open();
        if (DimensiumConfig.windowToolMaskEditorOpen) ToolMaskEditorWindow.INSTANCE.open();
        if (DimensiumConfig.windowPaletteOpen) PaletteWindow.INSTANCE.setOpen(true);
        if (DimensiumConfig.windowPaletteEditorOpen) PaletteEditorWindow.INSTANCE.open();
        if (DimensiumConfig.windowAnalyzeOpen) AnalyzeWindow.INSTANCE.open();
        if (DimensiumConfig.windowAutoshadeOpen) AutoshadeWindow.INSTANCE.open();
        if (DimensiumConfig.windowBlockInfoOpen) BlockInfoWindow.INSTANCE.setOpen(true);
        if (DimensiumConfig.windowSelectionOpen) SelectionWindow.INSTANCE.open();
        if (DimensiumConfig.windowOperationsOpen) OperationsWindow.INSTANCE.open();
        if (DimensiumConfig.windowClipboardOpen) ClipboardWindow.INSTANCE.open();
        if (!DimensiumConfig.windowToolPanelOpen) OverlayRenderer.TOOL_WINDOW.setOpen(false);
        if (!DimensiumConfig.windowToolOptionsPanelOpen) OverlayRenderer.TOOL_OPTIONS_WINDOW.setOpen(false);

        LayoutPresetRegistry reg = LayoutPresetRegistry.INSTANCE;
        reg.registerWindow("tools", OverlayRenderer.TOOL_WINDOW::isOpen, OverlayRenderer.TOOL_WINDOW::setOpen, true);
        reg.registerWindow(
            "toolOptions",
            OverlayRenderer.TOOL_OPTIONS_WINDOW::isOpen,
            OverlayRenderer.TOOL_OPTIONS_WINDOW::setOpen,
            true);
        reg.registerWindow(
            "toolMaskList",
            ToolMaskListWindow.INSTANCE::isOpen,
            ToolMaskListWindow.INSTANCE::setOpen,
            true);
        reg.registerWindow(
            "toolMaskEditor",
            ToolMaskEditorWindow.INSTANCE::isOpen,
            ToolMaskEditorWindow.INSTANCE::setOpen,
            true);
        reg.registerWindow("palette", PaletteWindow.INSTANCE::isOpen, PaletteWindow.INSTANCE::setOpen, true);
        reg.registerWindow(
            "paletteEditor",
            PaletteEditorWindow.INSTANCE::isOpen,
            PaletteEditorWindow.INSTANCE::setOpen,
            true);
        reg.registerWindow("selection", SelectionWindow.INSTANCE::isOpen, SelectionWindow.INSTANCE::setOpen, true);
        reg.registerWindow("operations", OperationsWindow.INSTANCE::isOpen, OperationsWindow.INSTANCE::setOpen, true);
        reg.registerWindow("clipboard", ClipboardWindow.INSTANCE::isOpen, ClipboardWindow.INSTANCE::setOpen, true);
        reg.registerWindow("blockInfo", BlockInfoWindow.INSTANCE::isOpen, BlockInfoWindow.INSTANCE::setOpen, true);
        reg.registerWindow("history", HistoryWindow.INSTANCE::isOpen, HistoryWindow.INSTANCE::setOpen, true);
        reg.registerWindow("analyze", AnalyzeWindow.INSTANCE::isOpen, AnalyzeWindow.INSTANCE::setOpen, false);
        reg.registerWindow("autoshade", AutoshadeWindow.INSTANCE::isOpen, AutoshadeWindow.INSTANCE::setOpen, false);
    }

    @Override
    public void loadComplete(cpw.mods.fml.common.event.FMLLoadCompleteEvent event) {
        // Nothing — BlockColorCache initializes lazily on first render frame via TextureStitchEvent.
    }
}
