package com.akashic.ryoiki.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
 * The owner's sounds: a resource pack over {@code config/ryoikitenkai/sons/}. The mod has no sound of its own, so
 * this pack also writes the {@code sounds.json}, with an event for each file that is there. Read when resources load
 * (at start and on F3+T).
 */
public final class OwnerSounds implements IResourcePack {

    private static final String SOUNDS_JSON = "sounds.json";
    /** The sounds found at the last resource load. */
    private static volatile Set<String> present = Collections.emptySet();

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
            RyoikiTenkai.LOG.info("Sounds of the owner are read from {}", folder);
        } catch (RuntimeException e) {
            RyoikiTenkai.LOG.warn("The owner's sounds will not be read", e);
        }
    }

    /** Whether the owner gave this sound: nothing is played, and nothing logged, for one that is missing. */
    static boolean has(String name) {
        return present.contains(name);
    }

    private List<String> scan() {
        List<String> found = new ArrayList<>();
        String[] files = folder.list();
        if (files != null) {
            Arrays.sort(files);
            for (String f : files) {
                if (f.equals(SoundCues.externalFile("sounds/" + f))) {
                    found.add(f.substring(0, f.length() - 4));
                }
            }
        }
        return found;
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
        if (RyoikiTenkai.MODID.equals(location.getResourceDomain()) && SOUNDS_JSON.equals(location.getResourcePath())) {
            List<String> found = scan();
            present = new HashSet<>(found);
            RyoikiTenkai.LOG.info("Sounds of the owner: {}", found);
            return new ByteArrayInputStream(
                SoundCues.soundsJson(found)
                    .getBytes(StandardCharsets.UTF_8));
        }
        File f = file(location);
        if (f == null) {
            throw new FileNotFoundException(location.toString());
        }
        return new FileInputStream(f);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (RyoikiTenkai.MODID.equals(location.getResourceDomain()) && SOUNDS_JSON.equals(location.getResourcePath())) {
            return true;
        }
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
