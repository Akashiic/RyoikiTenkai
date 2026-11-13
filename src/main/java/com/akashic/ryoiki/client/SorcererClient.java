package com.akashic.ryoiki.client;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.akashic.ryoiki.mode.ModeRules;
import com.akashic.ryoiki.mode.ModeWire;
import com.akashic.ryoiki.mode.SorcererMode;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * The sorcerer mode, on the client: the key that asks for it, and what the player sees and can do while in it.
 *
 * <p>
 * The server decides who is in the mode ({@link SorcererMode}); this only shows it. In the mode the hotbar is gone
 * and the hand is empty, the player's own and everyone else's; the keys that would change, open, drop or pick an
 * item do nothing; and a click is a swing of the arm and nothing else, as it is for the casters of a clash
 * ({@link ClashInput}).
 *
 * <p>
 * The player's things are never touched, here or on the server: they are only not shown. The item in a hand is taken
 * out of the player's inventory just for the instant that player is drawn, and put back before anything else can see
 * it gone (a mod that refills an emptied hand, say).
 */
public final class SorcererClient {

    public static final SorcererClient INSTANCE = new SorcererClient();

    /** Ticks a second press of the key is ignored: one press, one change. */
    private static final int ASK_AGAIN = 8;

    private KeyBinding key;
    private boolean on;
    /** The other players in the mode, by entity. */
    private final Set<Integer> others = new HashSet<>();
    /** What the server said, from the network thread. */
    private final Queue<byte[]> heard = new ConcurrentLinkedQueue<>();
    private volatile boolean left;
    private int ticks;
    private int askedAt = -1000;
    /** The tick this player's mode last changed, for the HUD's coming and going. */
    private int changedAt = -1000;
    /** The item taken out of each player's hand while it is drawn, and the slot it goes back to. */
    private final Map<EntityPlayer, Object[]> hidden = new IdentityHashMap<>();

    private SorcererClient() {}

    void install() {
        key = new KeyBinding("ryoikitenkai.tecla.modo", Keyboard.KEY_R, "ryoikitenkai.tecla.categoria");
        ClientRegistry.registerKeyBinding(key);
        SorcererMode.INSTANCE.channel()
            .register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Whether this client's player is in the mode. */
    public boolean on() {
        return on;
    }

    /** Whether a player this client sees is in the mode. */
    public boolean inMode(EntityPlayer player) {
        return player == Minecraft.getMinecraft().thePlayer ? on : others.contains(player.getEntityId());
    }

    /** Ticks since this player's mode changed, for what comes and goes with it. */
    float sinceChange(float partialTicks) {
        return ticks - changedAt + partialTicks;
    }

    /** The key as the player has it, for the screen: "Ctrl+R". */
    String keyName() {
        return "Ctrl+" + GameSettings.getKeyDisplayString(key.getKeyCode());
    }

    /** Asks the server for the other mode. It answers; until then nothing changes here. */
    public void ask() {
        if (ticks - askedAt < ASK_AGAIN || Minecraft.getMinecraft().thePlayer == null) {
            return;
        }
        askedAt = ticks;
        SorcererMode.INSTANCE.channel()
            .sendToServer(new FMLProxyPacket(Unpooled.wrappedBuffer(ModeWire.ask(!on)), ModeWire.CHANNEL));
    }

    @SubscribeEvent
    public void onHeard(FMLNetworkEvent.ClientCustomPacketEvent event) {
        // A network thread: only note it down.
        ByteBuf payload = event.packet.payload();
        byte[] data = new byte[Math.min(payload.readableBytes(), 16)];
        payload.getBytes(payload.readerIndex(), data);
        heard.add(data);
    }

    @SubscribeEvent
    public void onLeft(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        left = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ticks++;
        if (left || mc.thePlayer == null) {
            left = false;
            on = false;
            others.clear();
            hidden.clear();
            heard.clear();
            changedAt = -1000;
            return;
        }
        for (byte[] data = heard.poll(); data != null; data = heard.poll()) {
            ModeWire.Message m = ModeWire.decode(data);
            if (m == null) {
                continue;
            }
            if (m.op == ModeWire.STATE) {
                if (m.entity == mc.thePlayer.getEntityId()) {
                    if (on != m.on) {
                        on = m.on;
                        changedAt = ticks;
                    }
                } else if (m.on) {
                    others.add(m.entity);
                } else {
                    others.remove(m.entity);
                }
            } else if (m.op == ModeWire.REFUSED) {
                String words = ModeRules.key(m.reason);
                if (words != null) {
                    ChatComponentText line = new ChatComponentText(I18n.format(words));
                    line.getChatStyle()
                        .setColor(EnumChatFormatting.RED);
                    mc.thePlayer.addChatMessage(line);
                }
            }
        }
        if (on) {
            release(mc);
        }
    }

    /**
     * In the mode the buttons of the game's own click are never down: not one that was held when the mode began,
     * not one bound to a key. A button the game believes held would go on breaking the block in front.
     */
    private static void release(Minecraft mc) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindPickBlock.getKeyCode(), false);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) {
            return;
        }
        // The key as it was pressed, not as the game counts presses: two mods on one key share that count.
        if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()
            && Keyboard.getEventKey() == key.getKeyCode()
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            ask();
        }
        if (!on) {
            return;
        }
        // The keys of the inventory do nothing: the presses are taken before the game looks for them.
        GameSettings keys = mc.gameSettings;
        for (KeyBinding slot : keys.keyBindsHotbar) {
            drain(slot);
        }
        drain(keys.keyBindInventory);
        drain(keys.keyBindDrop);
        drain(keys.keyBindPickBlock);
        drain(keys.keyBindUseItem);
        // The click, for who has it on a key: one swing.
        boolean struck = false;
        while (keys.keyBindAttack.isPressed()) {
            struck = true;
        }
        if (struck && !ClientDomain.INSTANCE.frozen()) {
            ClashInput.blow();
        }
        release(mc);
    }

    private static void drain(KeyBinding binding) {
        while (binding.isPressed()) {
            // Taken.
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!on || mc.thePlayer == null || ClientDomain.INSTANCE.frozen() || ClashInput.fighting()) {
            // Held by the void, or one of the two of a clash: that has its own rules for the click.
            if (on && event.dwheel != 0) {
                event.setCanceled(true);
            }
            return;
        }
        if (event.dwheel != 0) {
            // The wheel would change the item in the hand.
            event.setCanceled(true);
            return;
        }
        if (event.button < 0) {
            return;
        }
        // Buttons are key codes from -100 up.
        int button = event.button - 100;
        GameSettings keys = mc.gameSettings;
        if (button == keys.keyBindAttack.getKeyCode()) {
            event.setCanceled(true);
            if (event.buttonstate) {
                ClashInput.blow();
            }
        } else if (button == keys.keyBindUseItem.getKeyCode() || button == keys.keyBindPickBlock.getKeyCode()) {
            event.setCanceled(true);
        }
    }

    // What is seen.

    @SubscribeEvent
    public void onOverlayBefore(RenderGameOverlayEvent.Pre event) {
        if (on && event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (on && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ModeHud.draw(
                Minecraft.getMinecraft(),
                this,
                event.resolution.getScaledWidth(),
                event.resolution.getScaledHeight(),
                event.partialTicks);
        }
    }

    /** Before anything draws the player, and before another mod's own swap of what it wears. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforePlayer(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        if (!inMode(player) || hidden.containsKey(player)) {
            return;
        }
        int slot = player.inventory.currentItem;
        if (slot < 0 || slot >= player.inventory.mainInventory.length) {
            return;
        }
        ItemStack held = player.inventory.mainInventory[slot];
        if (held != null) {
            hidden.put(player, new Object[] { slot, held });
            player.inventory.mainInventory[slot] = null;
        }
    }

    /** A draw something stopped never reaches its end: the item goes back now. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void playerNotDrawn(RenderPlayerEvent.Pre event) {
        if (event.isCanceled()) {
            show(event.entityPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void afterPlayer(RenderPlayerEvent.Post event) {
        show(event.entityPlayer);
    }

    /** Whatever happened to a draw, nothing stays hidden past the frame. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFrame(TickEvent.RenderTickEvent event) {
        if (!hidden.isEmpty()) {
            for (EntityPlayer player : hidden.keySet()
                .toArray(new EntityPlayer[0])) {
                show(player);
            }
        }
    }

    private void show(EntityPlayer player) {
        Object[] was = hidden.remove(player);
        if (was != null) {
            int slot = (Integer) was[0];
            // Only into the place it left empty: what the server put there meanwhile stays.
            if (player.inventory.mainInventory[slot] == null) {
                player.inventory.mainInventory[slot] = (ItemStack) was[1];
            }
        }
    }

    /**
     * For the first-person hand, which the game makes ready each tick from the item held: in the mode, from none.
     * The hand lowers what it held and comes back bare, as it does when a slot is emptied.
     */
    public static ItemStack firstPersonItem(ItemStack held) {
        return INSTANCE.on ? null : held;
    }
}
