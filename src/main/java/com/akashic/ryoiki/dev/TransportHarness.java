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
    private boolean local;
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
                // "local": this client's own server, with no plugin and no other mod, driven by commands like a
                // server is.
                local = words[0].equals("local");
                remote |= local;
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
            if (local) {
                step = Step.JOIN;
                host = "this client";
                mc.launchIntegratedServer(
                    WORLD + "-local",
                    WORLD + "-local",
                    new WorldSettings(20261113L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT)
                        .enableCommands());
            } else if (remote) {
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
    private int[] digAt;
    private int digIn;
    private final java.util.Set<net.minecraft.client.settings.KeyBinding> heldKeys = new java.util.HashSet<>();

    private void held(net.minecraft.client.settings.KeyBinding key, boolean down) {
        if (down) {
            heldKeys.add(key);
        } else {
            heldKeys.remove(key);
            net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }
    }

    private float walkStrafe;
    private float walkForward;
    private int heldBlow;
    private boolean turnaround;
    private float[] turnPose;
    private String[] turnBones = com.akashic.ryoiki.fx.BlowPose.BONES;

    /** The model in four views, over everything, while it is asked for. */
    @SubscribeEvent
    public void onOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        if (turnaround && event.type == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
            Turnaround.draw(
                Minecraft.getMinecraft(),
                event.resolution.getScaledWidth(),
                event.resolution.getScaledHeight(),
                turnPose == null ? null : turnBones,
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
        if (digIn > 0 && --digIn == 0) {
            report(
                "dig: the block under the feet is " + mc.theWorld.getBlock(digAt[0], digAt[1], digAt[2])
                    .getUnlocalizedName());
        }
        for (net.minecraft.client.settings.KeyBinding key : heldKeys) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), true);
        }
        net.minecraft.client.settings.KeyBinding
            .setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), walkForward > 0);
        net.minecraft.client.settings.KeyBinding
            .setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), walkForward < 0);
        net.minecraft.client.settings.KeyBinding
            .setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), walkStrafe > 0);
        net.minecraft.client.settings.KeyBinding
            .setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), walkStrafe < 0);
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
                // In a clash, as the mouse would: one swing. Out of one, the game's own click.
                if (com.akashic.ryoiki.client.ClashInput.fighting()) {
                    com.akashic.ryoiki.client.ClashInput.blow();
                } else {
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, target);
                }
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
                turnBones = com.akashic.ryoiki.fx.BlowPose.BONES;
                if (rest.startsWith("gait ")) {
                    // "turn gait 1 0.25 0": the stride of the mode: how much sprint, where in the stride, which way
                    // (degrees). "turn gait air 0.42": in the air, rising that fast. "turn gait stand".
                    String[] at = rest.split(" ");
                    com.akashic.ryoiki.fx.Gait.State state = new com.akashic.ryoiki.fx.Gait.State();
                    if ("air".equals(at[1])) {
                        state.moving = 1;
                        state.air = 1;
                        state.rising = Float.parseFloat(at[2]);
                    } else if (!"stand".equals(at[1])) {
                        state.moving = 1;
                        state.sprint = Float.parseFloat(at[1]);
                        state.phase = Float.parseFloat(at[2]);
                        state.heading = at.length > 3 ? Float.parseFloat(at[3]) : 0;
                    }
                    turnPose = com.akashic.ryoiki.fx.Gait.sample(state);
                } else if (rest.startsWith("cast ")) {
                    // "turn cast 1 6": the gesture of a technique (1 the pull, 2 the cut), six ticks in.
                    String[] at = rest.split(" ");
                    turnBones = com.akashic.ryoiki.fx.CastPose.BONES;
                    turnPose = com.akashic.ryoiki.fx.CastPose.sample(Integer.parseInt(at[1]), Float.parseFloat(at[2]));
                } else if ("guard".equals(rest)) {
                    turnPose = com.akashic.ryoiki.fx.BlowPose.guard();
                } else if (rest.startsWith("struck ")) {
                    // "turn struck 0.8": a blow taken on the guard, that long ago; "turn struck -0.8": from the right.
                    float at = Float.parseFloat(rest.substring(7));
                    turnPose = com.akashic.ryoiki.fx.BlowPose
                        .struck(com.akashic.ryoiki.fx.BlowPose.guard(), Math.abs(at), at < 0 ? -1 : 1);
                } else if (rest.contains(" ")) {
                    String[] at = rest.split(" ");
                    turnPose = com.akashic.ryoiki.fx.BlowPose.sample(Integer.parseInt(at[0]), Float.parseFloat(at[1]));
                }
                break;
            case "blow":
                heldBlow = Integer.parseInt(rest);
                break;
            case "dig": {
                // The game's own click on the block under the feet, and whether it is still there a moment later.
                int bx = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posX);
                int by = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.boundingBox.minY) - 1;
                int bz = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posZ);
                mc.playerController.clickBlock(bx, by, bz, 1);
                digAt = new int[] { bx, by, bz };
                digIn = 12;
                break;
            }
            case "mode":
                // What Ctrl+R does: asks the server for the other mode.
                com.akashic.ryoiki.client.SorcererClient.INSTANCE.ask();
                break;
            case "slot":
                mc.thePlayer.inventory.currentItem = Integer.parseInt(rest);
                break;
            case "cool": {
                // "cool 4 200 200": a recharge of a technique as the server would announce it (slot, total, left).
                String[] n = rest.split(" ");
                com.akashic.ryoiki.client.SorcererClient.INSTANCE
                    .recharge(Integer.parseInt(n[0]) - 1, Integer.parseInt(n[1]), Integer.parseInt(n[2]));
                break;
            }
            case "press":
                com.akashic.ryoiki.client.SorcererClient.INSTANCE.press(Integer.parseInt(rest) - 1);
                break;
            case "drop":
                // The drop key, as the game sends it: the server is asked to toss the item in the hand.
                mc.thePlayer.dropOneItem(false);
                break;
            case "use":
                // The use button with the item in the hand, as the game sends it.
                if (mc.thePlayer.inventory.getCurrentItem() != null) {
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem());
                }
                break;
            case "attack": {
                // The game's own strike at the nearest other player, as the game sends it.
                net.minecraft.entity.player.EntityPlayer near = null;
                for (Object o : mc.theWorld.playerEntities) {
                    net.minecraft.entity.player.EntityPlayer p = (net.minecraft.entity.player.EntityPlayer) o;
                    if (p != mc.thePlayer && (near == null
                        || p.getDistanceToEntity(mc.thePlayer) < near.getDistanceToEntity(mc.thePlayer))) {
                        near = p;
                    }
                }
                if (near != null) {
                    mc.playerController.attackEntity(mc.thePlayer, near);
                }
                break;
            }
            case "respawn":
                mc.thePlayer.respawnPlayer();
                mc.displayGuiScreen(null);
                break;
            case "inv": {
                // The mode and the player's things as this client has them, and what its first-person hand shows.
                net.minecraft.entity.player.InventoryPlayer inv = mc.thePlayer.inventory;
                StringBuilder things = new StringBuilder();
                for (int i = 0; i < inv.mainInventory.length; i++) {
                    if (inv.mainInventory[i] != null) {
                        things.append(i)
                            .append('=')
                            .append(inv.mainInventory[i].getUnlocalizedName())
                            .append('x')
                            .append(inv.mainInventory[i].stackSize)
                            .append(' ');
                    }
                }
                int armor = 0;
                for (net.minecraft.item.ItemStack piece : inv.armorInventory) {
                    armor += piece == null ? 0 : 1;
                }
                net.minecraft.item.ItemStack hand = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                    net.minecraft.client.renderer.ItemRenderer.class,
                    mc.entityRenderer.itemRenderer,
                    "itemToRender",
                    "field_78453_b");
                StringBuilder modes = new StringBuilder();
                for (Object o : mc.theWorld.playerEntities) {
                    net.minecraft.entity.player.EntityPlayer p = (net.minecraft.entity.player.EntityPlayer) o;
                    modes.append(p.getCommandSenderName())
                        .append(com.akashic.ryoiki.client.SorcererClient.INSTANCE.inMode(p) ? "=sorcerer " : "=normal ")
                        .append("holds ")
                        .append(
                            p.getHeldItem() == null ? "nothing"
                                : p.getHeldItem()
                                    .getUnlocalizedName())
                        .append("; ");
                }
                report(
                    String.format(
                        "inv mode %s%s%s, domain in %d s, slot %d, hand shows %s, health %.0f, food %d, armor %d, things: %s| %s| chat: %s",
                        com.akashic.ryoiki.client.SorcererClient.INSTANCE.on() ? "sorcerer" : "normal",
                        mc.thePlayer.isSwingInProgress ? " swinging" : "",
                        Boolean.TRUE.equals(
                            cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                                net.minecraft.client.multiplayer.PlayerControllerMP.class,
                                mc.playerController,
                                "isHittingBlock",
                                "field_78778_j")) ? " breaking-a-block" : "",
                        com.akashic.ryoiki.client.SorcererClient.INSTANCE.domainSeconds(),
                        inv.currentItem,
                        hand == null ? "nothing" : hand.getUnlocalizedName(),
                        mc.thePlayer.getHealth(),
                        mc.thePlayer.getFoodStats()
                            .getFoodLevel(),
                        armor,
                        things,
                        modes,
                        lastChat(mc)));
                break;
            }
            case "gait":
                // "gait off": the models move as they do without the mode, to compare.
                com.akashic.ryoiki.client.Stride.enabled = !"off".equals(rest);
                break;
            case "sprint":
                held(mc.gameSettings.keyBindSprint, "on".equals(rest));
                break;
            case "jump":
                // "jump on": the jump key held, until "jump off".
                held(mc.gameSettings.keyBindJump, "on".equals(rest));
                break;
            case "walk":
                // "walk 0 -1": hold the keys for strafe and forward (here: backward), until "walk 0 0".
                String[] way = rest.split(" ");
                walkStrafe = Float.parseFloat(way[0]);
                walkForward = Float.parseFloat(way[1]);
                break;
            case "slashes": {
                // Where the next flurries of cuts will be, to go and look at one: for each, the tick of the domain
                // it starts at, where the middle of its first cut is from the arena's centre, and how far that is.
                ClientDomain d = ClientDomain.INSTANCE;
                com.akashic.ryoiki.fx.SlashField.Slash cut = new com.akashic.ryoiki.fx.SlashField.Slash();
                StringBuilder next = new StringBuilder("slashes now " + d.age + ":");
                int flurry = com.akashic.ryoiki.fx.SlashField.flurry(d.age) + 1;
                for (int i = 0; i < 40; i++, flurry++) {
                    int first = flurry * com.akashic.ryoiki.fx.SlashField.FLURRY;
                    int born = com.akashic.ryoiki.fx.SlashField.born(first);
                    com.akashic.ryoiki.fx.SlashField.at(d.session, first, born, d.radius, cut);
                    next.append(String.format(" %d,%.1f,%.1f,%.1f", born, cut.midX, cut.midY, cut.midZ));
                }
                report(next.toString());
                break;
            }
            case "fly":
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
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
                .append(p.isSprinting() ? "(runs)" : "(walks)")
                .append(String.format("v=%.2f", Math.hypot(p.posX - p.prevPosX, p.posZ - p.prevPosZ)))
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
