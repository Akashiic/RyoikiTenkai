package com.akashic.ryoiki;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.akashic.ryoiki.arena.ArenaManager;
import com.akashic.ryoiki.block.BlockVeil;
import com.akashic.ryoiki.command.CommandRyoiki;
import com.akashic.ryoiki.domain.DomainManager;
import com.akashic.ryoiki.mode.SorcererMode;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * RyoikiTenkai: Domain Expansion and Domain Clash for Minecraft 1.7.10 (inspired by Jujutsu Kaisen and by Jujutsu
 * Shenanigans). A mod for both sides: the server runs the domains and their arenas, the client draws them.
 */
@Mod(modid = RyoikiTenkai.MODID, name = RyoikiTenkai.NAME, version = Tags.VERSION)
public final class RyoikiTenkai {

    public static final String MODID = "ryoikitenkai";
    public static final String NAME = "RyoikiTenkai";
    public static final Logger LOG = LogManager.getLogger(NAME);

    @Mod.Instance(MODID)
    public static RyoikiTenkai instance;

    @SidedProxy(clientSide = "com.akashic.ryoiki.client.ClientProxy", serverSide = "com.akashic.ryoiki.CommonProxy")
    public static CommonProxy proxy;

    /** The invisible block the arenas are made of. */
    public static Block veil;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
        veil = new BlockVeil();
        GameRegistry.registerBlock(veil, "veu");
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(DomainManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(DomainManager.INSTANCE);
        // Tickets of a previous run are dropped: the arenas ask for theirs again when first used.
        ForgeChunkManager.setForcedChunkLoadingCallback(this, new ForgeChunkManager.LoadingCallback() {

            @Override
            public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
                for (ForgeChunkManager.Ticket ticket : tickets) {
                    ForgeChunkManager.releaseTicket(ticket);
                }
            }
        });
        SorcererMode.INSTANCE.init();
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandRyoiki());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        DomainManager.INSTANCE.endAll();
        SorcererMode.INSTANCE.reset();
        ArenaManager.INSTANCE.reset();
    }
}
