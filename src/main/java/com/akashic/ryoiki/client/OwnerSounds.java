package com.akashic.ryoiki.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.fx.SoundCues;

import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * The owner's sounds: a resource pack over {@code config/ryoikitenkai/sons/}, placed after the mod's own so a file
 * there replaces the one in the jar. Read when resources load (at start and on F3+T).
 */
public final class OwnerSounds implements IResourcePack {

    private final File folder;

    private OwnerSounds(File folder) {
        this.folder = folder;
    }

    /** Before the first resource load, so the sound events are built with the owner's files. */
    public static void install() {
        File folder = new File(Minecraft.getMinecraft().mcDataDir, "config/ryoikitenkai/sons");
        try {
            List<IResourcePack> packs = ReflectionHelper
                .getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
            packs.add(new OwnerSounds(folder));
            String[] found = folder.list((dir, name) -> name.endsWith(".ogg"));
            RyoikiTenkai.LOG.info("Sounds of the owner in {}: {}", folder, found == null ? 0 : found.length);
        } catch (RuntimeException e) {
            RyoikiTenkai.LOG.warn("The owner's sounds will not be read", e);
        }
    }

    private File file(ResourceLocation location) {
        if (!RyoikiTenkai.MODID.equals(location.getResourceDomain())) {
            return null;
        }
        String name = SoundCues.externalFile(location.getResourcePath());
        if (name == null) {
            return null;
        }
        File f = new File(folder, name);
        return f.isFile() ? f : null;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        File f = file(location);
        if (f == null) {
            throw new FileNotFoundException(location.toString());
        }
        return new FileInputStream(f);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return file(location) != null;
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton(RyoikiTenkai.MODID);
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "RyoikiTenkai: config/ryoikitenkai/sons";
    }
}
