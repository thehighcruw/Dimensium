package github.thehighcruw.dimensium.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import github.thehighcruw.dimensium.handler.PlayerHistoryEventHandler;

public class ServerProxy implements IProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {}

    @Override
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerHistoryEventHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {}

    @Override
    public void loadComplete(FMLLoadCompleteEvent event) {}
}
