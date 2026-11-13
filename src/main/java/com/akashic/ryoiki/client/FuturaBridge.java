package com.akashic.ryoiki.client;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;

/**
 * The hand signs on the players' own models. Where 3D Futura Engine draws the players (the network's player-model
 * mod), a caster makes the domain's sign with its model: the arms, the forearms, the hands and, on models that have
 * them, the fingers. The mod is reached by reflection through its public API ({@code FuturaModels.setBonePose}, API
 * 6), so nothing here needs it: without it, or with an older one, {@link #modelled} is false and the cut-in draws the
 * plain skin instead.
 */
final class FuturaBridge {

    static final FuturaBridge INSTANCE = new FuturaBridge();

    /** One sign: the bones it poses and, three for each, their rotations in degrees as in an animation file. */
    private static final class Sign {

        String[] bones;
        float[] degrees;
    }

    private MethodHandle modelKey;
    private MethodHandle setBonePose;
    private MethodHandle setScreenDraw;
    private MethodHandle locatorInLastDraw;
    private final Map<DomainType, Sign> signs = new EnumMap<>(DomainType.class);
    private File override;
    private long overrideStamp;

    private FuturaBridge() {
        try {
            Class<?> api = Class.forName("com.akashic.futura.api.FuturaModels");
            if (api.getField("API_VERSION")
                .getInt(null) >= 6) {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                modelKey = lookup.findStatic(api, "modelKey", MethodType.methodType(String.class, EntityPlayer.class));
                setBonePose = lookup.findStatic(
                    api,
                    "setBonePose",
                    MethodType
                        .methodType(boolean.class, EntityPlayer.class, String[].class, float[].class, float.class));
                setScreenDraw = lookup
                    .findStatic(api, "setScreenDraw", MethodType.methodType(void.class, boolean.class));
                locatorInLastDraw = lookup.findStatic(
                    api,
                    "locatorInLastDraw",
                    MethodType.methodType(float[].class, EntityPlayer.class, String.class));
                RyoikiTenkai.LOG.info("3D Futura Engine found: casters make the hand signs with their models");
            } else {
                RyoikiTenkai.LOG.info("3D Futura Engine is older than API 6: the hand signs use the plain skin");
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            modelKey = null;
            setBonePose = null;
        }
        // The names are those of the models' standard bones; the finger bones (the four fingers as one, in two
        // joints, and the thumb) are the ones the network's models use, and are skipped where a model lacks them.
        signs.put(
            DomainType.MALEVOLENT_SHRINE,
            sign(
                // Both hands raised and joined before the mouth, mirrored, fingers straight; the head bowed. The
                // forearms turn inward by opposite angles: the same sign on both would leave one hand out of place.
                "RightArm -78 0 0",
                "RightForeArm -78 -18 0",
                "LeftArm -78 0 0",
                "LeftForeArm -78 18 0",
                "right_fg 0 0 0",
                "right_ffg 0 0 0",
                "left_fg 0 0 0",
                "left_ffg 0 0 0",
                "Head 8 0 0"));
        signs.put(
            DomainType.INFINITE_VOID,
            sign(
                // One hand raised before the face, fingers up and the thumb folded in; the other arm as it was.
                "RightArm -40 0 0",
                "RightForeArm -125 -30 0",
                "right_fg 0 0 0",
                "right_ffg 0 0 0",
                "left_dmz 0 0 -45",
                "Head -5 0 0"));
        if (Boolean.getBoolean("ryoiki.devHarness")) {
            // Development: the signs can be retuned while the game runs.
            override = new File(Minecraft.getMinecraft().mcDataDir, "ryoiki-pose.txt");
        }
    }

    private static Sign sign(String... lines) {
        Sign s = new Sign();
        s.bones = new String[lines.length];
        s.degrees = new float[lines.length * 3];
        for (int i = 0; i < lines.length; i++) {
            String[] w = lines[i].trim()
                .split("\\s+");
            s.bones[i] = w[0];
            for (int k = 0; k < 3; k++) {
                s.degrees[i * 3 + k] = Float.parseFloat(w[k + 1]);
            }
        }
        return s;
    }

    /** Whether this player is drawn with a model that can make the sign. */
    boolean modelled(EntityPlayer player) {
        if (setBonePose == null || player == null) {
            return false;
        }
        try {
            return (String) modelKey.invoke(player) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Poses the player's model for the next draw.
     *
     * @param sign 0 (as the model's animation has it) to 1 (the sign held)
     */
    void pose(EntityPlayer player, DomainType type, float sign) {
        if (setBonePose == null || player == null || sign <= 0) {
            return;
        }
        reload();
        Sign s = signs.get(type);
        try {
            boolean ignored = (boolean) setBonePose.invoke(player, s.bones, s.degrees, Math.min(1, sign));
        } catch (Throwable e) {
            setBonePose = null;
            RyoikiTenkai.LOG.warn("3D Futura Engine refused the hand sign: using the plain skin from now on", e);
        }
    }

    /** Around a draw of the player's model on the HUD: the model goes where the matrix puts it, not in the world. */
    void screenDraw(boolean drawing) {
        if (setScreenDraw != null) {
            try {
                setScreenDraw.invoke(drawing);
            } catch (Throwable e) {
                setScreenDraw = null;
            }
        }
    }

    /**
     * Where a bone of the model was in the draw that just ended, in blocks from the point it was drawn at; null if
     * unknown. Models differ in height and build: the cut-in aims its camera by the model's own head and hand.
     */
    float[] point(EntityPlayer player, String bone) {
        if (locatorInLastDraw == null) {
            return null;
        }
        try {
            float[] m = (float[]) locatorInLastDraw.invoke(player, bone);
            return m == null ? null : new float[] { m[12], m[13], m[14] };
        } catch (Throwable e) {
            locatorInLastDraw = null;
            return null;
        }
    }

    /** Development only: {@code ryoiki-pose.txt}, lines of {@code <domain id> <bone> <x> <y> <z>}. */
    private void reload() {
        if (override == null || !override.isFile() || override.lastModified() == overrideStamp) {
            return;
        }
        overrideStamp = override.lastModified();
        try {
            Map<DomainType, List<String>> lines = new EnumMap<>(DomainType.class);
            for (String line : Files.readAllLines(override.toPath(), StandardCharsets.UTF_8)) {
                String[] w = line.trim()
                    .split("\\s+", 2);
                DomainType type = w.length < 2 ? null : DomainType.byId(w[0]);
                if (type != null) {
                    lines.computeIfAbsent(type, k -> new ArrayList<>())
                        .add(w[1]);
                }
            }
            for (Map.Entry<DomainType, List<String>> e : lines.entrySet()) {
                signs.put(
                    e.getKey(),
                    sign(
                        e.getValue()
                            .toArray(new String[0])));
            }
        } catch (IOException | RuntimeException e) {
            RyoikiTenkai.LOG.warn("ryoiki-pose.txt could not be read", e);
        }
    }
}
