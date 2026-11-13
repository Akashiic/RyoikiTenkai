package com.akashic.ryoiki.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * The sounds of a domain, by name. Each name is a sound event in {@code assets/ryoikitenkai/sounds.json} and a file
 * {@code <name>.ogg}: the one in the jar, or the owner's in {@code config/ryoikitenkai/sons/}, which wins.
 *
 * <p>
 * A sound heard from a place in the world must be mono (OpenAL does not attenuate stereo); the others are stereo.
 */
public final class SoundCues {

    /** The hand sign charging, at the caster. */
    public static final String SIGN = "sinal";
    /** The hit on each camera cut of the cut-in. */
    public static final String BAND_CUT = "faixa_corte";
    /** The domain breaking, for those inside. */
    public static final String EXIT = "saida";
    /** The hum of a dome, for those outside. */
    public static final String DOME = "domo";

    /** Per domain, as {@code <domain id>_<name>}: the caster's voice line, at the caster. Silent in the jar. */
    public static final String VOICE = "voz";
    /** Per domain: the domain closing, for those inside and, at the dome, for those outside. */
    public static final String ENTER = "entrada";
    /** Per domain: the loop heard inside. */
    public static final String AMBIENT = "ambiente";
    /** Per domain: one cut. Only the domains that cut have it. */
    public static final String SLASH = "corte";

    private static final String[] SHARED = { SIGN, BAND_CUT, EXIT, DOME };
    private static final String[] PER_DOMAIN = { VOICE, ENTER, AMBIENT };
    private static final String PREFIX = "sounds/";
    private static final String SUFFIX = ".ogg";

    private SoundCues() {}

    public static String of(String domainId, String name) {
        return domainId + "_" + name;
    }

    /** Every sound of the mod, given the domain ids and the ids of those that cut. */
    public static List<String> all(String[] domainIds, String[] slashing) {
        List<String> out = new ArrayList<>();
        for (String s : SHARED) {
            out.add(s);
        }
        for (String id : domainIds) {
            for (String s : PER_DOMAIN) {
                out.add(of(id, s));
            }
        }
        for (String id : slashing) {
            out.add(of(id, SLASH));
        }
        return out;
    }

    /** Whether a sound is heard from a place in the world, and so must be mono. */
    public static boolean mono(String name) {
        return name.equals(SIGN) || name.equals(DOME) || name.endsWith("_" + VOICE) || name.endsWith("_" + ENTER);
    }

    /**
     * The owner's file for a resource path of the mod ({@code sounds/vazio_ambiente.ogg} gives
     * {@code vazio_ambiente.ogg}), or null when the path is not a sound or does not name a plain file.
     */
    public static String externalFile(String resourcePath) {
        if (resourcePath == null || !resourcePath.startsWith(PREFIX) || !resourcePath.endsWith(SUFFIX)) {
            return null;
        }
        String name = resourcePath.substring(PREFIX.length(), resourcePath.length() - SUFFIX.length());
        if (name.isEmpty()) {
            return null;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) {
                return null;
            }
        }
        return name + SUFFIX;
    }
}
