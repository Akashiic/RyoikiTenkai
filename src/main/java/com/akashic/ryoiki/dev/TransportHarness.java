package com.akashic.ryoiki.dev;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.arena.ArenaManager;
import com.akashic.ryoiki.client.ClientDomain;
import com.akashic.ryoiki.domain.DomainManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Development-only test of the trip into a domain and back (never in the release jar): with
 * {@code -Dryoiki.devHarness=true} and a {@code ryoiki-harness} file in the run directory, the client opens a world
 * with real terrain, casts each domain and records, frame by frame, whether the player has ground under its feet and
 * how many
 * meshes around it wait to be rebuilt. Screenshots go to {@code screenshots/ryoiki-*.png}, the report to
 * {@code ryoiki-harness.txt}; then the client quits.
 * <p>
 * The file's content picks the run: {@code cold} casts at once (the arena is built and the terrain around it is still
 * there); {@code warm} waits for the arenas to be ready first. On a real server (the whole modpack, Crucible):
 * {@code server <host> <port>} joins it, waits for a {@code ryoiki-harness-go} file and then takes its orders from a
 * {@code ryoiki-harness-cmd} file (see {@link #command}); the player must be an operator to cast with
 * {@code /ryoiki}.
 */
public final class TransportHarness {

    private static final String WORLD = "ryoiki-transport";
    /** Frames after each teleport that get a screenshot. */
    private static final int[] SHOTS = { 0, 1, 2, 4, 8, 16, 40 };

    private enum Step {
        JOIN,
        SETTLE,
        IDLE,
        PREPARE,
        CAST,
        IN_DOMAIN,
        AFTER,
        DONE
    }

    private boolean launched;
    private boolean cold;
    /** On a real server: no access to the server's objects, everything goes through chat commands. */
    private boolean remote;
    private String host;
    private int port;
    private int casts = 2;
    private int joinAttempts;
    private DomainType castType = DomainType.INFINITE_VOID;
    private int view;
    private int watchLeft;
    private int watchShot;
    private int commandWait;
    private final java.util.ArrayDeque<String> commands = new java.util.ArrayDeque<>();
    private int deadTicks;
    private int menuTicks;
    private Step step = Step.SETTLE;
    private int ticks;
    private int run;
    private PrintWriter out;
    private volatile Runnable serverJob;
    private volatile int pendingVoid = -1;

    // frame recording around a teleport
    private double lastX = Double.NaN;
    private double lastZ;
    private String phase;
    private int frame = -1;
    private long teleportNanos;
    private long lastFrameNanos;
    private int firstGround = -1;
    private final List<String> lines = new ArrayList<>();

    /** Every sound the game plays, in the log: what is heard in a domain besides the owner's files. */
    @SubscribeEvent
    public void onPlaySound(net.minecraftforge.client.event.sound.PlaySoundEvent17 event) {
        RyoikiTenkai.LOG.info(
            "harness sound: {} (inside: {})",
            event.sound.getPositionedSoundLocation(),
            ClientDomain.INSTANCE.inside);
    }

    public static void install() {
        TransportHarness h = new TransportHarness();
        FMLCommonHandler.instance()
            .bus()
            .register(h);
        MinecraftForge.EVENT_BUS.register(h);
        RyoikiTenkai.LOG.warn("RyoikiTenkai DEV HARNESS enabled: this client will run the transport test and quit");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        Runnable job = serverJob;
        if (event.phase == TickEvent.Phase.START && job != null) {
            serverJob = null;
            job.run();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || step == Step.DONE) {
            return;
        }
        if (!launched && mc.theWorld == null
            && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen != null && ++menuTicks > 60)) {
            launched = true;
            try {
                String[] words = new String(
                    Files.readAllBytes(new File(mc.mcDataDir, "ryoiki-harness").toPath()),
                    StandardCharsets.UTF_8).trim()
                        .split("\\s+");
                cold = words[0].equals("cold");
                if (words[0].equals("server")) {
                    remote = true;
                    host = words[1];
                    port = Integer.parseInt(words[2]);
                }
            } catch (IOException | RuntimeException e) {
                cold = false;
            }
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.renderDistanceChunks = 8;
            mc.gameSettings.limitFramerate = 260;
            mc.gameSettings.enableVsync = false;
            mc.mouseHelper = new net.minecraft.util.MouseHelper() {

                @Override
                public void mouseXYChange() {
                    deltaX = 0;
                    deltaY = 0;
                }
            };
            if (remote) {
                step = Step.JOIN;
                join(mc);
            } else {
                mc.launchIntegratedServer(
                    WORLD,
                    WORLD,
                    new WorldSettings(20261112L, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT));
            }
            return;
        }
        if (step == Step.JOIN) {
            if (mc.thePlayer != null && mc.theWorld != null) {
                next(Step.SETTLE);
            } else if (mc.currentScreen instanceof net.minecraft.client.gui.GuiDisconnected && ++menuTicks > 100) {
                // Refused or timed out: say so and try again a few times (a loaded test machine is slow to answer).
                menuTicks = 0;
                if (++joinAttempts > 6) {
                    report("FAILED: could not join " + host + ":" + port);
                    finish(mc);
                } else {
                    RyoikiTenkai.LOG.warn("Harness: join attempt {} failed, trying again", joinAttempts);
                    join(mc);
                }
            }
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiDownloadTerrain)) {
            mc.displayGuiScreen(null);
        }
        mc.gameSettings.pauseOnLostFocus = false;
        if (remote && mc.thePlayer.getHealth() <= 0) {
            // A fresh test world can kill the player where it spawns: get up and carry on.
            if (++deadTicks % 40 == 1) {
                report("the player died (tick " + ticks + " of " + step + "): respawning");
                mc.thePlayer.respawnPlayer();
            }
            return;
        }
        ticks++;
        if (watchLeft > 0 && --watchLeft % 30 == 0) {
            watchShot++;
            shot(mc, String.format("ryoiki-watch-%03d", watchShot));
            report(
                String.format(
                    "watch %03d: at %.1f %.1f %.1f, inside %s, domes %d, client chunks %s",
                    watchShot,
                    mc.thePlayer.posX,
                    mc.thePlayer.boundingBox.minY,
                    mc.thePlayer.posZ,
                    ClientDomain.INSTANCE.inside,
                    ClientDomain.INSTANCE.domes.size(),
                    mc.theWorld.getChunkProvider()
                        .makeString()));
        }
        switch (step) {
            case SETTLE:
                if (ticks == 1 && !remote) {
                    onServer(p -> {
                        WorldServer w = p.getServerForPlayer();
                        w.setWorldTime(6000);
                        w.getWorldInfo()
                            .setRaining(false);
                        int x = w.getSpawnPoint().posX;
                        int z = w.getSpawnPoint().posZ;
                        p.capabilities.isFlying = false;
                        p.playerNetServerHandler
                            .setPlayerLocation(x + 0.5, w.getTopSolidOrLiquidBlock(x, z), z + 0.5, 30, 15);
                    });
                }
                // On a server the test waits for a "ryoiki-harness-go" file: whoever runs it places the players first.
                if (remote ? ticks >= 100 && new File(mc.mcDataDir, "ryoiki-harness-go").isFile()
                    : ticks >= 300 && dirtyNear(mc, 4) == 0) {
                    report(
                        "java " + System.getProperty("java.version")
                            + ", "
                            + (remote ? "on " + host + ":" + port : cold ? "cold run" : "warm run")
                            + ", player "
                            + mc.thePlayer.getCommandSenderName());
                    report(
                        "render distance " + mc.gameSettings.renderDistanceChunks
                            + ", client chunks: "
                            + mc.theWorld.getChunkProvider()
                                .makeString());
                    shot(mc, "ryoiki-0-map");
                    next(remote ? Step.IDLE : cold ? Step.CAST : Step.PREPARE);
                }
                break;
            case IDLE:
                command(mc);
                break;
            case PREPARE:
                if (remote) {
                    if (ticks == 1) {
                        mc.thePlayer.sendChatMessage("/ryoiki preparar");
                    }
                    if (ticks == 400) {
                        next(Step.CAST);
                    }
                } else if (ticks == 1) {
                    onServer(p -> {
                        WorldServer w = p.getServerForPlayer();
                        long t = System.nanoTime();
                        for (int slot = 0; slot < ArenaManager.INSTANCE.slots(w)
                            .size(); slot++) {
                            String error = ArenaManager.INSTANCE.prepare(w, slot);
                            if (error != null) {
                                report("prepare failed: " + error);
                            }
                        }
                        report("arenas built in " + (System.nanoTime() - t) / 1000000 + " ms");
                        pendingVoid = ArenaManager.INSTANCE.pending(w);
                    });
                } else if (ticks % 20 == 0) {
                    onServer(p -> pendingVoid = ArenaManager.INSTANCE.pending(p.getServerForPlayer()));
                    if (pendingVoid == 0) {
                        report("terrain around the arenas emptied after " + ticks + " ticks");
                        next(Step.CAST);
                    }
                }
                break;
            case CAST:
                if (ticks == 1) {
                    DomainType type = remote ? castType : DomainType.values()[run % DomainType.values().length];
                    report("");
                    report("== cast " + (run + 1) + ": " + type.id + " ==");
                    mc.gameSettings.thirdPersonView = remote ? view : run % 2;
                    if (remote) {
                        report(
                            String.format(
                                "casting at %.1f %.1f %.1f",
                                mc.thePlayer.posX,
                                mc.thePlayer.boundingBox.minY,
                                mc.thePlayer.posZ));
                        mc.thePlayer.sendChatMessage("/ryoiki expandir " + type.id);
                    } else onServer(p -> {
                        long t = System.nanoTime();
                        String error = DomainManager.INSTANCE.cast(p, type);
                        report(
                            "cast: " + (error == null ? "ok" : error)
                                + ", "
                                + (System.nanoTime() - t) / 1000
                                + " us on the server");
                    });
                }
                // Every tick of the hand sign: the cut-in, frame by frame.
                // (-Dryoiki.shotEvery=2 for every other tick, where saving a capture takes longer than a tick.)
                if (ticks >= 2 && ticks <= 26 && ticks % Integer.getInteger("ryoiki.shotEvery", 1) == 0) {
                    shot(mc, "ryoiki-" + (run + 1) + (ticks == 12 ? "-windup" : String.format("-windup-t%02d", ticks)));
                }
                if (ClientDomain.INSTANCE.inside != null) {
                    next(Step.IN_DOMAIN);
                } else if (ticks > 200) {
                    report("the domain never opened. Chat: " + lastChat(mc));
                    shot(mc, "ryoiki-" + (run + 1) + "-refused");
                    if (remote) {
                        run++;
                        next(Step.IDLE);
                    } else {
                        finish(mc);
                    }
                }
                break;
            case IN_DOMAIN:
                if (ticks == 30 || ticks == 60) {
                    // Toward what stands behind the caster, then off to one side.
                    mc.thePlayer.rotationYaw = ClientDomain.INSTANCE.yaw + (ticks == 30 ? 180 : 135);
                    mc.thePlayer.rotationPitch = ticks == 30 ? -16 : 2;
                }
                if (ticks == 34 || ticks == 64 || ticks == 68) {
                    shot(mc, "ryoiki-" + (run + 1) + "-look-t" + ticks);
                }
                if (ticks == 100) {
                    report(
                        "in the domain, client chunks: " + mc.theWorld.getChunkProvider()
                            .makeString() + ", kept: " + ClientDomain.INSTANCE.keep.keptCount());
                    shot(mc, "ryoiki-" + (run + 1) + "-inside");
                    // Walk into the wall: the shell must hold.
                    mc.thePlayer.rotationYaw = 90;
                }
                if (ticks > 100 && ticks < 200) {
                    net.minecraft.client.settings.KeyBinding
                        .setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), ticks < 190);
                }
                if (ticks == 200) {
                    double dx = mc.thePlayer.posX - ClientDomain.INSTANCE.arenaX;
                    double dz = mc.thePlayer.posZ - ClientDomain.INSTANCE.arenaZ;
                    report(
                        String.format(
                            "walked into the wall: %.2f blocks from the centre (radius %d), y %.2f (floor %.0f)",
                            Math.sqrt(dx * dx + dz * dz),
                            ClientDomain.INSTANCE.radius,
                            mc.thePlayer.boundingBox.minY,
                            ClientDomain.INSTANCE.arenaY));
                    shot(mc, "ryoiki-" + (run + 1) + "-wall");
                }
                if (ClientDomain.INSTANCE.inside == null) {
                    next(Step.AFTER);
                }
                break;
            case AFTER:
                if (ticks == 160) {
                    report(
                        "back, client chunks: " + mc.theWorld.getChunkProvider()
                            .makeString() + ", kept: " + ClientDomain.INSTANCE.keep.keptCount());
                    report(
                        String.format(
                            "standing at %.1f %.1f %.1f",
                            mc.thePlayer.posX,
                            mc.thePlayer.boundingBox.minY,
                            mc.thePlayer.posZ));
                    run++;
                    if (remote) {
                        next(Step.IDLE);
                    } else if (run >= casts) {
                        finish(mc);
                    } else {
                        next(Step.CAST);
                    }
                }
                break;
            default:
                break;
        }
    }

    /** Every frame: follows the player across a teleport and notes when the world around it is whole again. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || step == Step.DONE) {
            return;
        }
        long now = System.nanoTime();
        double x = mc.thePlayer.posX;
        double z = mc.thePlayer.posZ;
        if (!Double.isNaN(lastX) && Math.abs(x - lastX) + Math.abs(z - lastZ) > 500
            && (step == Step.CAST || step == Step.IN_DOMAIN || step == Step.AFTER || step == Step.IDLE)) {
            flushFrames();
            phase = ClientDomain.INSTANCE.inside != null ? "in" : "out";
            frame = 0;
            teleportNanos = lastFrameNanos;
            firstGround = -1;
        }
        lastX = x;
        lastZ = z;
        if (frame >= 0) {
            boolean ground = mc.theWorld.blockExists(MathHelper.floor_double(x), 0, MathHelper.floor_double(z));
            int dirty = dirtyNear(mc, 3);
            if (ground && firstGround < 0) {
                firstGround = frame;
            }
            if (frame <= 40) {
                lines.add(
                    String.format(
                        "  frame %2d  +%4d ms  ground %s  meshes in view queued to rebuild: %d  flash %d",
                        frame,
                        (now - teleportNanos) / 1000000,
                        ground ? "yes" : "NO",
                        dirty,
                        ClientDomain.INSTANCE.flash));
            }
            for (int s : SHOTS) {
                if (s == frame) {
                    shot(mc, "ryoiki-" + (run + 1) + "-" + phase + "-f" + (frame < 10 ? "0" : "") + frame);
                    now = System.nanoTime();
                }
            }
            frame++;
            if (frame > 400) {
                flushFrames();
            }
        }
        lastFrameNanos = now;
    }

    private void flushFrames() {
        if (frame < 0) {
            return;
        }
        report("teleport " + phase + ": ground under the player from frame " + firstGround);
        for (String l : lines) {
            report(l);
        }
        lines.clear();
        frame = -1;
    }

    /**
     * Sections with blocks, in view and within {@code chunks} of the player, whose mesh is queued to be rebuilt.
     * Not a count of holes: a chunk the server sends again is queued too, and keeps its old mesh until then. And it
     * is vanilla's renderer only (Angelica has its own). What the player sees is in the screenshots.
     */
    private static int dirtyNear(Minecraft mc, int chunks) {
        try {
            Field f = RenderGlobal.class.getDeclaredField("worldRenderers");
            f.setAccessible(true);
            WorldRenderer[] all = (WorldRenderer[]) f.get(mc.renderGlobal);
            if (all == null) {
                return -1;
            }
            int n = 0;
            double reach = chunks * 16;
            for (WorldRenderer r : all) {
                if (r != null && r.needsUpdate
                    && r.isInFrustum
                    && !mc.theWorld.getChunkFromBlockCoords(r.posX, r.posZ)
                        .getAreLevelsEmpty(r.posY, r.posY + 15)
                    && Math.abs(r.posX + 8 - mc.thePlayer.posX) <= reach
                    && Math.abs(r.posZ + 8 - mc.thePlayer.posZ) <= reach
                    && Math.abs(r.posY + 8 - mc.thePlayer.posY) <= reach) {
                    n++;
                }
            }
            return n;
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    /**
     * Joins the server. FML's handshake waits on a latch that only its own entry points create; this one creates it
     * and nothing else (the others need the server list or ping the server first).
     */
    private void join(Minecraft mc) {
        cpw.mods.fml.client.FMLClientHandler.instance()
            .connectToRealmsServer(host, port);
        mc.displayGuiScreen(new net.minecraft.client.multiplayer.GuiConnecting(new GuiMainMenu(), mc, host, port));
    }

    /**
     * On a server the test is driven from outside: lines appended to a {@code ryoiki-harness-cmd} file in the run
     * directory, one command at a time. {@code chat <text>} sends a chat line or a command; {@code cast <domain>}
     * casts and records the trip; {@code view <0|1|2>} sets the camera; {@code look <yaw> <pitch>};
     * {@code shot <name>}; {@code turn on|off} shows the model in four views, in the pose of {@code ryoiki-bones.txt};
     * {@code blow <n>} holds a blow's pose on this player ({@code blow 0} lets go);
     * {@code film <ticks>} takes a screenshot every other tick; {@code hit <n>} gives n blows to
     * the nearest other player; {@code watch <seconds>} takes a
     * screenshot every second and a half; {@code where} writes
     * the position; {@code quit}.
     */
    private int hitsLeft;
    private int filmLeft;
    private int heldBlow;
    private boolean turnaround;
    private float[] turnPose;

    /** The model in four views, over everything, while it is asked for. */
    @SubscribeEvent
    public void onOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        if (turnaround && event.type == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
            Turnaround.draw(
                Minecraft.getMinecraft(),
                event.resolution.getScaledWidth(),
                event.resolution.getScaledHeight(),
                turnPose == null ? null : com.akashic.ryoiki.fx.BlowPose.BONES,
                turnPose);
        }
    }

    private int filmed;

    private void command(Minecraft mc) {
        File file = new File(mc.mcDataDir, "ryoiki-harness-cmd");
        if (ticks % 10 == 0 && file.isFile()) {
            try {
                for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                    if (!line.trim()
                        .isEmpty()) {
                        commands.add(line.trim());
                    }
                }
                Files.delete(file.toPath());
            } catch (IOException e) {
                return;
            }
        }
        if (heldBlow > 0) {
            // A blow held at its full pose on this player's model, to look at it.
            ClientDomain.Blow held = new ClientDomain.Blow();
            held.blow = heldBlow;
            held.age = 2;
            ClientDomain.INSTANCE.blows.put(mc.thePlayer.getEntityId(), held);
        }
        if (filmLeft > 0) {
            // A screenshot every other tick: what a blow looks like from one tick to the next.
            if (filmLeft % 2 == 0) {
                shot(mc, String.format("ryoiki-film-%03d", filmed++));
            }
            filmLeft--;
        }
        if (hitsLeft > 0 && ticks % 7 == 0) {
            // One blow at the nearest other player, as a click would give.
            net.minecraft.entity.player.EntityPlayer target = null;
            for (Object o : mc.theWorld.playerEntities) {
                net.minecraft.entity.player.EntityPlayer p = (net.minecraft.entity.player.EntityPlayer) o;
                if (p != mc.thePlayer && (target == null
                    || p.getDistanceSqToEntity(mc.thePlayer) < target.getDistanceSqToEntity(mc.thePlayer))) {
                    target = p;
                }
            }
            if (target != null) {
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);
                hitsLeft--;
            }
        }
        if (commandWait > 0) {
            commandWait--;
            return;
        }
        String line = commands.poll();
        if (line == null) {
            return;
        }
        String[] words = line.split(" ", 2);
        String rest = words.length > 1 ? words[1] : "";
        report("> " + line);
        switch (words[0]) {
            case "chat":
                mc.thePlayer.sendChatMessage(rest);
                commandWait = 20;
                break;
            case "cast":
                castType = DomainType.byId(rest);
                next(Step.CAST);
                break;
            case "turn":
                turnaround = !"off".equals(rest);
                // "turn 1 2.5": the first blow, two and a half ticks in. "turn guard". "turn on": the bones file.
                turnPose = null;
                if ("guard".equals(rest)) {
                    turnPose = com.akashic.ryoiki.fx.BlowPose.guard();
                } else if (rest.contains(" ")) {
                    String[] at = rest.split(" ");
                    turnPose = com.akashic.ryoiki.fx.BlowPose.sample(Integer.parseInt(at[0]), Float.parseFloat(at[1]));
                }
                break;
            case "blow":
                heldBlow = Integer.parseInt(rest);
                break;
            case "film":
                filmLeft = Integer.parseInt(rest);
                break;
            case "hit":
                hitsLeft = Integer.parseInt(rest);
                break;
            case "view":
                view = Integer.parseInt(rest);
                mc.gameSettings.thirdPersonView = view;
                break;
            case "look": {
                String[] angles = rest.split(" ");
                mc.thePlayer.rotationYaw = Float.parseFloat(angles[0]);
                mc.thePlayer.rotationPitch = Float.parseFloat(angles[1]);
                commandWait = 5;
                break;
            }
            case "shot":
                shot(mc, "ryoiki-" + rest);
                break;
            case "watch":
                watchLeft = Integer.parseInt(rest) * 20;
                break;
            case "wait":
                commandWait = Integer.parseInt(rest) * 20;
                break;
            case "where":
                report(
                    String.format(
                        "at %.1f %.1f %.1f in dimension %d, inside %s, domes %d, players seen %s, chat: %s",
                        mc.thePlayer.posX,
                        mc.thePlayer.boundingBox.minY,
                        mc.thePlayer.posZ,
                        mc.thePlayer.dimension,
                        ClientDomain.INSTANCE.inside,
                        ClientDomain.INSTANCE.domes.size(),
                        seen(mc),
                        lastChat(mc)));
                break;
            case "quit":
                finish(mc);
                break;
            default:
                report("unknown command");
                break;
        }
    }

    /** The players this client has in its world, with how far each is. */
    private static String seen(Minecraft mc) {
        StringBuilder out = new StringBuilder();
        for (Object o : mc.theWorld.playerEntities) {
            net.minecraft.entity.player.EntityPlayer p = (net.minecraft.entity.player.EntityPlayer) o;
            out.append(p.getCommandSenderName())
                .append('#')
                .append(p.getEntityId())
                .append('@')
                .append((int) p.getDistanceToEntity(mc.thePlayer))
                .append(' ');
        }
        ClientDomain d = ClientDomain.INSTANCE;
        out.append("clash ")
            .append(d.clash)
            .append(" of #")
            .append(d.clashEntities[0])
            .append(" and #")
            .append(d.clashEntities[1]);
        return out.toString();
    }

    /** The last lines of chat, where the server's refusal is. */
    private static String lastChat(Minecraft mc) {
        StringBuilder out = new StringBuilder();
        for (Object line : mc.ingameGUI.getChatGUI()
            .getSentMessages()) {
            out.append(" | ")
                .append(line);
        }
        try {
            for (java.lang.reflect.Field f : net.minecraft.client.gui.GuiNewChat.class.getDeclaredFields()) {
                if (!java.util.List.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                f.setAccessible(true);
                java.util.List<?> lines = (java.util.List<?>) f.get(mc.ingameGUI.getChatGUI());
                if (lines.isEmpty() || !(lines.get(0) instanceof net.minecraft.client.gui.ChatLine)) {
                    continue;
                }
                for (int i = 0; i < Math.min(4, lines.size()); i++) {
                    out.append(" || ")
                        .append(
                            ((net.minecraft.client.gui.ChatLine) lines.get(i)).func_151461_a()
                                .getUnformattedText());
                }
                break;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            out.append(" (chat unreadable: ")
                .append(e)
                .append(')');
        }
        return out.toString();
    }

    private interface ServerAction {

        void run(EntityPlayerMP player);
    }

    private void onServer(ServerAction action) {
        String name = Minecraft.getMinecraft().thePlayer.getCommandSenderName();
        serverJob = () -> {
            EntityPlayerMP p = MinecraftServer.getServer()
                .getConfigurationManager()
                .func_152612_a(name);
            if (p != null) {
                action.run(p);
            }
        };
    }

    private void next(Step s) {
        step = s;
        ticks = 0;
    }

    private void finish(Minecraft mc) {
        flushFrames();
        report("done");
        step = Step.DONE;
        out.close();
        mc.shutdown();
    }

    private static void shot(Minecraft mc, String name) {
        ScreenShotHelper
            .saveScreenshot(mc.mcDataDir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private synchronized void report(String line) {
        try {
            if (out == null) {
                out = new PrintWriter(
                    Files.newBufferedWriter(
                        new File(Minecraft.getMinecraft().mcDataDir, "ryoiki-harness.txt").toPath(),
                        StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            return;
        }
        out.println(line);
        out.flush();
    }
}
