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
 * there); anything else waits for the arenas to be ready first.
 */
public final class TransportHarness {

    private static final String WORLD = "ryoiki-transport";
    /** Frames after each teleport that get a screenshot. */
    private static final int[] SHOTS = { 0, 1, 2, 4, 8, 16, 40 };

    private enum Step {
        SETTLE,
        PREPARE,
        CAST,
        IN_DOMAIN,
        AFTER,
        DONE
    }

    private boolean launched;
    private boolean cold;
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

    public static void install() {
        TransportHarness h = new TransportHarness();
        FMLCommonHandler.instance()
            .bus()
            .register(h);
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
                cold = new String(
                    Files.readAllBytes(new File(mc.mcDataDir, "ryoiki-harness").toPath()),
                    StandardCharsets.UTF_8).trim()
                        .startsWith("cold");
            } catch (IOException e) {
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
            mc.launchIntegratedServer(
                WORLD,
                WORLD,
                new WorldSettings(20261112L, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT));
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
        }
        mc.gameSettings.pauseOnLostFocus = false;
        ticks++;
        switch (step) {
            case SETTLE:
                if (ticks == 1) {
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
                if (ticks >= 300 && dirtyNear(mc, 4) == 0) {
                    report("java " + System.getProperty("java.version") + ", " + (cold ? "cold" : "warm") + " run");
                    report(
                        "render distance " + mc.gameSettings.renderDistanceChunks
                            + ", client chunks: "
                            + mc.theWorld.getChunkProvider()
                                .makeString());
                    shot(mc, "ryoiki-0-map");
                    next(cold ? Step.CAST : Step.PREPARE);
                }
                break;
            case PREPARE:
                if (ticks == 1) {
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
                    DomainType type = DomainType.values()[run % DomainType.values().length];
                    report("");
                    report("== cast " + (run + 1) + ": " + type.id + " ==");
                    mc.gameSettings.thirdPersonView = run % 2;
                    onServer(p -> {
                        long t = System.nanoTime();
                        String error = DomainManager.INSTANCE.cast(p, type);
                        report(
                            "cast: " + (error == null ? "ok" : error)
                                + ", "
                                + (System.nanoTime() - t) / 1000
                                + " us on the server");
                    });
                }
                if (ticks == 12) {
                    shot(mc, "ryoiki-" + (run + 1) + "-windup");
                }
                if (ClientDomain.INSTANCE.inside != null) {
                    next(Step.IN_DOMAIN);
                } else if (ticks > 200) {
                    report("FAILED: the domain never opened");
                    finish(mc);
                }
                break;
            case IN_DOMAIN:
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
                    run++;
                    if (run >= 2) {
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
            && (step == Step.CAST || step == Step.IN_DOMAIN || step == Step.AFTER)) {
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
