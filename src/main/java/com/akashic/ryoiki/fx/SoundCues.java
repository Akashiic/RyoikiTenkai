package com.akashic.ryoiki.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * The sounds of a domain, by name. The mod ships none: each name is a file {@code <name>.ogg} the owner may put in
 * {@code config/ryoikitenkai/sons/}, and a sound event of the same name exists only when its file does.
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
    /** A blow of a caster in a clash, at the caster; and one that lands, at whom it lands on. */
    public static final String BLOW = "golpe";
    public static final String IMPACT = "impacto";
    /** The track of a clash of domains, over whichever domain track was playing. */
    public static final String CLASH_TRACK = "clash_trilha";

    /**
     * Per domain, as {@code <domain id>_<name>}: the track of the whole expansion, from the hand sign to the end of
     * the shatter, for the caster and those caught.
     */
    public static final String TRACK = "trilha";
    /** Per domain: the caster's voice line, from the first tick of the hand sign, for those who see the cut-in. */
    public static final String VOICE = "voz";
    /** Per domain: the domain closing, for those inside and, at the dome, for those outside. */
    public static final String ENTER = "entrada";
    /** Per domain: the loop heard inside. */
    public static final String AMBIENT = "ambiente";
    /** Per domain: one cut. Only the domains that cut have it. */
    public static final String SLASH = "corte";

    private static final String[] SHARED = { SIGN, BAND_CUT, EXIT, DOME, CLASH_TRACK, BLOW, IMPACT };
    private static final String[] PER_DOMAIN = { TRACK, VOICE, ENTER, AMBIENT };
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
        return name.equals(SIGN) || name.equals(DOME)
            || name.equals(BLOW)
            || name.equals(IMPACT)
            || name.endsWith("_" + ENTER);
    }

    /** Whether a sound is long, and so read from the file as it plays. */
    public static boolean streamed(String name) {
        return name.endsWith("_" + TRACK) || name.endsWith("_" + AMBIENT);
    }

    /** Whether a sound is one of the owner's tracks: the game's own music is silent while one plays. */
    public static boolean track(String name) {
        return name.endsWith("_" + TRACK);
    }

    /** The {@code sounds.json} for the sounds whose files are there. */
    public static String soundsJson(List<String> present) {
        StringBuilder out = new StringBuilder("{");
        for (String name : present) {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append('"')
                .append(name)
                .append("\":{\"category\":\"")
                .append(streamed(name) ? "master" : "player")
                .append("\",\"sounds\":[{\"name\":\"")
                .append(name)
                .append("\",\"stream\":")
                .append(streamed(name))
                .append("}]}");
        }
        return out.append('}')
            .toString();
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
