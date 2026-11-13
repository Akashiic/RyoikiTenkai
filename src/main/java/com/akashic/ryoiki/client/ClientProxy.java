package com.akashic.ryoiki.client;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import com.akashic.ryoiki.CommonProxy;
import com.akashic.ryoiki.RyoikiTenkai;

import cpw.mods.fml.common.FMLCommonHandler;

public final class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        OwnerSounds.install();
    }

    @Override
    public void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(ClientDomain.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(CameraShake.INSTANCE);
        DomainRenderer renderer = new DomainRenderer();
        MinecraftForge.EVENT_BUS.register(renderer);
        MinecraftForge.EVENT_BUS.register(MusicHush.INSTANCE);
        // Development only, never in the release jar: -Dryoiki.devHarness=true and a "ryoiki-harness" file.
        if (Boolean.getBoolean("ryoiki.devHarness")
            && new File(Minecraft.getMinecraft().mcDataDir, "ryoiki-harness").isFile()) {
            try {
                MethodHandles.publicLookup()
                    .findStatic(
                        Class.forName("com.akashic.ryoiki.dev.TransportHarness"),
                        "install",
                        MethodType.methodType(void.class))
                    .invoke();
            } catch (Throwable e) {
                RyoikiTenkai.LOG.error("Could not start the development harness", e);
            }
        }
    }
}
