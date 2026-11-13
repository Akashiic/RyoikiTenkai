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
    /** The permission node a player needs to go into the mode; empty for none. */
    public static String modePermission = "ryoikitenkai.modo";
    /** Ticks a player must wait between two changes of mode. */
    public static int modeCooldown = 20;
    /** Seconds from a player expanding its domain with the mode until it may again. */
    public static int domainCooldown = 60;
    /** The damage of the techniques, in half hearts, whatever the armour. 0: none (and nothing to veto). */
    public static float pullDamage = 1;
    public static float cutDamage = 5;
    public static float redDamage = 6;
    public static float blackDamage = 8;
    public static float redSelfDamage = 4;
    /** How far from the purple its burst kills, in blocks. */
    public static int purpleRadius = 80;
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
        modePermission = cfg.getString(
            "permissao",
            "modo",
            modePermission,
            "Permissao (node) que o jogador precisa ter para entrar no modo Feiticeiro com Ctrl+R. Operadores tem "
                + "por padrao. Vazio: qualquer um entra. No modo, as habilidades ignoram WorldGuard e qualquer outra "
                + "protecao, entre os jogadores que estao nele.")
            .trim();
        modeCooldown = cfg
            .getInt("recarga", "modo", modeCooldown, 0, 1200, "Ticks entre duas trocas de modo do mesmo jogador.");
        domainCooldown = cfg.getInt(
            "recarga",
            "dominio",
            domainCooldown,
            0,
            3600,
            "Segundos entre duas Expansoes de Dominio do mesmo jogador pelo modo Feiticeiro, contados a partir do "
                + "sinal de mao. O comando /ryoiki expandir nao tem recarga.");
        pullDamage = cfg.getFloat(
            "atracao",
            "dano",
            pullDamage,
            0,
            40,
            "Dano da Atracao (Vazio, habilidade 1) em meios coracoes, a cada meio segundo em quem o orbe segura, "
                + "igual com qualquer armadura. Com 0 ela so puxa.");
        redDamage = cfg.getFloat(
            "repulsao",
            "dano",
            redDamage,
            0,
            40,
            "Dano da Repulsao (Vazio, habilidade 2) de perto, em meios coracoes; cai com a distancia ate menos de "
                + "um quarto no fim do alcance.");
        blackDamage = cfg.getFloat(
            "golpe_negro",
            "dano",
            blackDamage,
            0,
            40,
            "Dano do golpe negro: o soco em quem a Repulsao traz de volta (R segurado), em meios coracoes.");
        redSelfDamage = cfg.getFloat(
            "repulsao_retorno",
            "dano",
            redSelfDamage,
            0,
            40,
            "Dano que o proprio conjurador leva quando a Repulsao volta (R segurado) sem trazer ninguem.");
        cutDamage = cfg.getFloat(
            "talho",
            "dano",
            cutDamage,
            0,
            40,
            "Dano do Talho (Santuario, habilidade 1) em meios coracoes, igual com qualquer armadura.");
        purpleRadius = cfg.getInt(
            "raio",
            "roxo",
            purpleRadius,
            1,
            160,
            "Raio do estouro do roxo (azul parado + vermelho), em blocos: mata os jogadores no modo dentro dele.");
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
