package com.akashic.ryoiki;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.akashic.ryoiki.arena.ArenaShape;

/** config/ryoikitenkai.cfg. */
public final class Config {

    /** The interior radius of a domain, in blocks: who is this close to the caster is caught. */
    public static int radius = 30;
    /** Domains at the same time, per dimension. */
    public static int maxDomains = 2;
    /** Ticks after a domain opens in which someone caught may still answer it with a domain, and clash. */
    public static int clashWindow = 60;
    /** Whether players may go into the sorcerer mode at all. */
    public static boolean modeEnabled = true;
    /** Ticks a player must wait between two changes of mode. */
    public static int modeCooldown = 20;
    public static int arenaX = 200000;
    public static int arenaZ = 200000;
    public static int arenaY = 120;

    private Config() {}

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        radius = cfg.getInt(
            "raio",
            "dominio",
            radius,
            ArenaShape.MIN_RADIUS,
            ArenaShape.MAX_RADIUS,
            "Raio do dominio em blocos: quem esta a essa distancia do conjurador e pego.");
        maxDomains = cfg.getInt("maximo", "dominio", maxDomains, 1, 8, "Dominios ao mesmo tempo, por dimensao.");
        clashWindow = cfg.getInt(
            "janela",
            "choque",
            clashWindow,
            0,
            400,
            "Ticks depois de um dominio abrir em que quem foi pego ainda pode responder com um dominio e chocar. "
                + "Durante o sinal de mao do primeiro sempre pode.");
        modeEnabled = cfg.getBoolean(
            "ativo",
            "modo",
            modeEnabled,
            "Se os jogadores podem entrar no modo Feiticeiro (Ctrl+R). Desligado, quem esta nele ainda pode sair.");
        modeCooldown = cfg
            .getInt("recarga", "modo", modeCooldown, 0, 1200, "Ticks entre duas trocas de modo do mesmo jogador.");
        arenaX = cfg.getInt(
            "x",
            "arena",
            arenaX,
            -20000000,
            20000000,
            "Onde ficam as arenas (X da primeira): uma regiao longe do mapa, na mesma dimensao do conjurador. "
                + "O terreno em volta e apagado; o mod se recusa a apagar chunks onde ja houve jogadores.");
        arenaZ = cfg.getInt("z", "arena", arenaZ, -20000000, 20000000, "Z das arenas.");
        arenaY = cfg.getInt("y", "arena", arenaY, 8, 220, "Altura do chao das arenas.");
        if (cfg.hasChanged()) {
            cfg.save();
        }
    }
}
