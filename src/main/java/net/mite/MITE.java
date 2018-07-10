package net.mite;

import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

/**
 * Created by manageryzy on 7/6/2018.
 */
@Mod(modid = MITE.MODID, name = MITE.NAME, version = MITE.VERSION)
public class MITE {
    public static final String MODID = "mite";
    public static final String NAME = "MITE is too easy";
    public static final String VERSION = "1.0";

    private static Logger logger;

    private static MITE instance = null;

    @Mod.InstanceFactory
    public MITE getInstance(){
        if(instance == null)
            instance = new MITE();

        return instance;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // some example code
        logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event){

    }

    @EventHandler
    public void serverStartingEvent(FMLServerStartingEvent event){

    }


}
