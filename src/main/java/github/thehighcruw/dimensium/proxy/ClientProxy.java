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
import github.thehighcruw.dimensium.blueprint.BlueprintRegistry;
import github.thehighcruw.dimensium.handler.CreativeGuiHandler;
import github.thehighcruw.dimensium.handler.InputHandler;
import github.thehighcruw.dimensium.handler.KeyHandler;
import github.thehighcruw.dimensium.handler.TickHandler;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.render.popup.AnalyzeWindow;
import github.thehighcruw.dimensium.render.popup.AutoshadeWindow;
import github.thehighcruw.dimensium.render.popup.BlockInfoWindow;
import github.thehighcruw.dimensium.render.popup.ClipboardWindow;
import github.thehighcruw.dimensium.render.popup.ColourFieldWindow;
import github.thehighcruw.dimensium.render.popup.DistortSelectionWindow;
import github.thehighcruw.dimensium.render.popup.FillSelectionWindow;
import github.thehighcruw.dimensium.render.popup.FilterSelectionWindow;
import github.thehighcruw.dimensium.render.popup.OperationsWindow;
import github.thehighcruw.dimensium.render.popup.PaletteEditorWindow;
import github.thehighcruw.dimensium.render.popup.PaletteWindow;
import github.thehighcruw.dimensium.render.popup.ReplaceSelectionWindow;
import github.thehighcruw.dimensium.render.popup.SelectionWindow;
import github.thehighcruw.dimensium.render.popup.SmoothSelectionWindow;
import github.thehighcruw.dimensium.render.popup.ToolMaskEditorWindow;
import github.thehighcruw.dimensium.render.popup.ToolMaskListWindow;
import github.thehighcruw.dimensium.render.popup.TypeReplaceSelectionWindow;
import github.thehighcruw.dimensium.render.sidebar.HistoryWindow;
import github.thehighcruw.dimensium.render.world.SelectionRenderer;
import github.thehighcruw.dimensium.tool.BlockColorCache;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.state.PaletteRegistry;

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
            ClipboardWindow.INSTANCE, ColourFieldWindow.INSTANCE, DistortSelectionWindow.INSTANCE,
            FillSelectionWindow.INSTANCE, FilterSelectionWindow.INSTANCE, HistoryWindow.INSTANCE,
            OperationsWindow.INSTANCE, ReplaceSelectionWindow.INSTANCE, SelectionWindow.INSTANCE,
            SmoothSelectionWindow.INSTANCE, TypeReplaceSelectionWindow.INSTANCE, ToolMaskListWindow.INSTANCE,
            ToolMaskEditorWindow.INSTANCE, PaletteWindow.INSTANCE, PaletteEditorWindow.INSTANCE,
            OverlayRenderer.toolPanel, OverlayRenderer.toolOptionsPanel };

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
        if (!DimensiumConfig.windowToolPanelOpen) OverlayRenderer.toolPanel.setOpen(false);
        if (!DimensiumConfig.windowToolOptionsPanelOpen) OverlayRenderer.toolOptionsPanel.setOpen(false);
    }

    @Override
    public void loadComplete(cpw.mods.fml.common.event.FMLLoadCompleteEvent event) {
        // Nothing — BlockColorCache initializes lazily on first render frame via TextureStitchEvent.
    }
}
