package com.akashic.ryoiki.mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.event.world.BlockEvent;

import com.akashic.ryoiki.Config;
import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.domain.DomainManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * The sorcerer mode, on the server: who is in it, and what that means.
 *
 * <p>
 * A player goes into the mode and out of it when it asks ({@link ModeRules}). In the mode it has no inventory to
 * speak of: its things stay exactly where they are, and it cannot use, move, drop or pick up any of them, nor break
 * or work a block, nor strike the game's own way. Nothing is taken from it and nothing is put away, so there is
 * nothing to give back and nothing that can be lost or doubled on the way.
 *
 * <p>
 * The one moment its things would leave it is when it dies. Then, before the game drops them, they are written into
 * the player's own saved data and taken from its hands; and the player it comes back as gets them again, each in its
 * place, at the moment the game itself would hand over an inventory that is kept. A server that stops in between
 * loses nothing: they are in the player's file.
 */
public final class SorcererMode {

    public static final SorcererMode INSTANCE = new SorcererMode();

    /** In the player's persisted data: its inventory, kept from a death in the mode until it lives again. */
    private static final String KEPT = "ryoikiModoGuardado";
    /**
     * There too: the player's technique (the id of its domain), and its hunger as it was when it went into the mode.
     */
    private static final String TECHNIQUE = "ryoikiTecnica";
    private static final String HUNGER = "ryoikiModoFome";

    private final Set<UUID> on = new HashSet<>();
    private final Map<UUID, Long> changed = new HashMap<>();
    /** What clients asked, from the network threads: who, and the mode wanted. */
    private final Queue<Object[]> asked = new ConcurrentLinkedQueue<>();
    private FMLEventChannel channel;

    private SorcererMode() {}

    /** On both sides: the channel is how a client asks and how it is told. */
    public void init() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(ModeWire.CHANNEL);
        channel.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public FMLEventChannel channel() {
        return channel;
    }

    /** Whether this player (of the server) is in the mode. */
    public boolean isOn(EntityPlayer player) {
        return player instanceof EntityPlayerMP && on.contains(player.getUniqueID());
    }

    /** Nobody is in the mode when the server stops: there is nothing to undo. */
    public void reset() {
        on.clear();
        changed.clear();
        asked.clear();
    }

    @SubscribeEvent
    public void onAsk(FMLNetworkEvent.ServerCustomPacketEvent event) {
        // A network thread: only note it down.
        ByteBuf payload = event.packet.payload();
        byte[] data = new byte[Math.min(payload.readableBytes(), 16)];
        payload.getBytes(payload.readerIndex(), data);
        ModeWire.Message m = ModeWire.decode(data);
        if (m != null && m.op == ModeWire.ASK && event.handler instanceof NetHandlerPlayServer) {
            asked.add(new Object[] { ((NetHandlerPlayServer) event.handler).playerEntity.getUniqueID(), m.on });
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Object[] ask = asked.poll(); ask != null; ask = asked.poll()) {
            EntityPlayerMP player = player((UUID) ask[0]);
            if (player != null) {
                set(player, (Boolean) ask[1]);
            }
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (!on.isEmpty() && server.getTickCounter() % 10 == 0) {
            for (UUID id : on) {
                EntityPlayerMP player = player(id);
                if (player != null && player.isEntityAlive()) {
                    // A death something else undid after its things were put away: it has them back.
                    giveBack(player);
                    fed(player);
                }
            }
        }
    }

    /**
     * Puts a player into the mode or takes it out, if it may. Returns why not ({@link ModeRules}), which the player
     * is told too.
     */
    public int set(EntityPlayerMP player, boolean want) {
        UUID id = player.getUniqueID();
        if (on.contains(id) == want) {
            // Already so: the client that asked may have lost count.
            send(player, ModeWire.state(player.getEntityId(), want));
            return ModeRules.OK;
        }
        long now = MinecraftServer.getServer()
            .getTickCounter();
        Long last = changed.get(id);
        int reason = ModeRules.ask(
            want,
            Config.modeEnabled,
            player.isEntityAlive(),
            DomainManager.INSTANCE.sessionOf(id) != null,
            last == null ? Long.MAX_VALUE : now - last,
            Config.modeCooldown);
        if (reason != ModeRules.OK) {
            send(player, ModeWire.refused(reason));
            return reason;
        }
        changed.put(id, now);
        if (want) {
            on.add(id);
            // What it was doing with its things ends here: a chest it had open, a bow it was drawing.
            if (player.openContainer != player.inventoryContainer) {
                player.closeScreen();
            }
            player.clearItemInUse();
            holdHunger(player);
        } else {
            on.remove(id);
            releaseHunger(player);
        }
        tell(player, want);
        return ModeRules.OK;
    }

    /** The player and everyone who sees it learn its mode. */
    private void tell(EntityPlayerMP player, boolean mode) {
        byte[] data = ModeWire.state(player.getEntityId(), mode);
        send(player, data);
        if (player.worldObj instanceof WorldServer) {
            for (EntityPlayer watcher : ((WorldServer) player.worldObj).getEntityTracker()
                .getTrackingPlayers(player)) {
                if (watcher instanceof EntityPlayerMP && watcher != player) {
                    send((EntityPlayerMP) watcher, data);
                }
            }
        }
    }

    // The technique: which of the domains a player's four techniques belong to. Its own, whatever mode it is in.

    /** A player's technique, or null if it has none yet. */
    public static DomainType technique(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        return data.hasKey(EntityPlayer.PERSISTED_NBT_TAG) ? DomainType.byId(
            data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
                .getString(TECHNIQUE))
            : null;
    }

    /** Gives a player a technique, or takes it away (null). It is kept with the player. */
    public void setTechnique(EntityPlayerMP player, DomainType technique) {
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (technique == null) {
            persisted.removeTag(TECHNIQUE);
        } else {
            persisted.setString(TECHNIQUE, technique.id);
        }
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        tellTechnique(player);
    }

    private void tellTechnique(EntityPlayerMP player) {
        DomainType technique = technique(player);
        send(player, ModeWire.technique(technique == null ? 0 : technique.ordinal() + 1));
    }

    /**
     * Tells a player that one of its four techniques (1 to {@link ModeWire#SLOTS}) recharges: for {@code total}
     * ticks, of which {@code left} are left. Its screen counts down from there on its own.
     */
    public void cooldown(EntityPlayerMP player, int slot, int total, int left) {
        send(player, ModeWire.cooldown(slot, total, left));
    }

    // Hunger. The mode has no hunger bar, so it has no hunger: a player in it counts as fed, and does not get
    // hungrier. What it had when it came in is waiting for it when it goes out.

    private static void holdHunger(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(HUNGER)) {
            NBTTagCompound was = new NBTTagCompound();
            player.getFoodStats()
                .writeNBT(was);
            persisted.setTag(HUNGER, was);
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        }
        fed(player);
    }

    /** Fed, and none the hungrier for what it just did. The count toward the next heal is its own. */
    private static void fed(EntityPlayerMP player) {
        NBTTagCompound now = new NBTTagCompound();
        player.getFoodStats()
            .writeNBT(now);
        if (now.getInteger("foodLevel") == 20 && now.getFloat("foodExhaustionLevel") == 0
            && now.getFloat("foodSaturationLevel") == 5) {
            return;
        }
        now.setInteger("foodLevel", 20);
        now.setFloat("foodSaturationLevel", 5);
        now.setFloat("foodExhaustionLevel", 0);
        player.getFoodStats()
            .readNBT(now);
    }

    private static void releaseHunger(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            return;
        }
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persisted.hasKey(HUNGER)) {
            player.getFoodStats()
                .readNBT(persisted.getCompoundTag(HUNGER));
            persisted.removeTag(HUNGER);
        }
    }

    private void send(EntityPlayerMP player, byte[] data) {
        if (player.playerNetServerHandler != null) {
            channel.sendTo(new FMLProxyPacket(Unpooled.wrappedBuffer(data), ModeWire.CHANNEL), player);
        }
    }

    private static EntityPlayerMP player(UUID id) {
        for (Object o : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            if (((EntityPlayerMP) o).getUniqueID()
                .equals(id)) {
                return (EntityPlayerMP) o;
            }
        }
        return null;
    }

    // Who sees whom.

    @SubscribeEvent
    public void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.target instanceof EntityPlayerMP && isOn((EntityPlayer) event.target)
            && event.entityPlayer instanceof EntityPlayerMP) {
            send((EntityPlayerMP) event.entityPlayer, ModeWire.state(event.target.getEntityId(), true));
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.isEntityAlive()) {
            // It died in the mode and the server stopped, or it left, before it lived again and got its things.
            giveBack(player);
        }
        // Nobody arrives in the mode: a hunger still held is from a server that stopped with the player in it.
        releaseHunger(player);
        tellTechnique(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // The mode is not kept from one visit to the next: there is nothing of it to keep. This is before the
        // player is saved, so it is saved with its own hunger.
        if (event.player instanceof EntityPlayerMP && on.contains(event.player.getUniqueID())) {
            releaseHunger((EntityPlayerMP) event.player);
        }
        on.remove(event.player.getUniqueID());
        changed.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        again(event.player);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        again(event.player);
    }

    /** A player is in the world anew: it has its things if they were kept, and its client hears its mode again. */
    private void again(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        if (player.isEntityAlive()) {
            giveBack((EntityPlayerMP) player);
        }
        tellTechnique((EntityPlayerMP) player);
        if (isOn(player)) {
            send((EntityPlayerMP) player, ModeWire.state(player.getEntityId(), true));
            if (player.isEntityAlive()) {
                holdHunger((EntityPlayerMP) player);
            }
        }
    }

    // What the mode forbids. The client does not even try; this is for the one that does.

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isOn(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (isOn(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEntityEvent event) {
        if (isOn(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (isOn(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseItem(PlayerUseItemEvent.Start event) {
        if (isOn(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickup(EntityItemPickupEvent event) {
        if (isOn(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onToss(ItemTossEvent event) {
        if (!isOn(event.player)) {
            return;
        }
        // The game has already taken the item out of the inventory, and a toss that is stopped destroys it: it goes
        // back where it came from first. If there is nowhere to put it, the toss goes on.
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        InventoryPlayer inventory = player.inventory;
        ItemStack tossed = event.entityItem.getEntityItem();
        ItemStack held = inventory.getCurrentItem();
        if (held == null) {
            inventory.setInventorySlotContents(inventory.currentItem, tossed);
        } else if (held.isItemEqual(tossed) && ItemStack.areItemStackTagsEqual(held, tossed)
            && held.stackSize + tossed.stackSize <= held.getMaxStackSize()) {
                held.stackSize += tossed.stackSize;
            } else if (!inventory.addItemStackToInventory(tossed)) {
                return;
            }
        event.setCanceled(true);
        player.inventoryContainer.detectAndSendChanges();
    }

    // Dying in the mode.

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP && isOn((EntityPlayer) event.entityLiving)) {
            putAway((EntityPlayerMP) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        // The player it comes back as: here the game itself hands over an inventory that is kept, and the saved
        // data of the old one has already been copied.
        if (event.wasDeath && event.entityPlayer instanceof EntityPlayerMP) {
            giveBack((EntityPlayerMP) event.entityPlayer);
        }
    }

    /** Before the game drops them: the player's things go into its saved data, and out of its hands. */
    private static void putAway(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persisted.hasKey(KEPT)) {
            return;
        }
        // Who dies comes back fed, in the mode or out of it: the hunger it had before the mode died with it.
        persisted.removeTag(HUNGER);
        NBTTagList things = player.inventory.writeToNBT(new NBTTagList());
        if (things.tagCount() == 0) {
            return;
        }
        persisted.setTag(KEPT, things);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            player.inventory.mainInventory[i] = null;
        }
        for (int i = 0; i < player.inventory.armorInventory.length; i++) {
            player.inventory.armorInventory[i] = null;
        }
    }

    /** The things put away at a death, back each in its place. Whatever the player got in between is not lost. */
    private static void giveBack(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            return;
        }
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(KEPT)) {
            return;
        }
        NBTTagList things = persisted.getTagList(KEPT, 10);
        persisted.removeTag(KEPT);
        InventoryPlayer inventory = player.inventory;
        List<ItemStack> since = new ArrayList<>();
        for (ItemStack stack : inventory.mainInventory) {
            if (stack != null) {
                since.add(stack);
            }
        }
        for (ItemStack stack : inventory.armorInventory) {
            if (stack != null) {
                since.add(stack);
            }
        }
        inventory.readFromNBT(things);
        for (ItemStack stack : since) {
            if (!inventory.addItemStackToInventory(stack)) {
                player.dropPlayerItemWithRandomChoice(stack, false);
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        RyoikiTenkai.LOG.info(
            "{} died in the sorcerer mode: {} stacks given back",
            player.getCommandSenderName(),
            things.tagCount());
    }

    /** For the command: the words of a refusal, red, or null. */
    public static ChatComponentTranslation words(int reason) {
        String key = ModeRules.key(reason);
        if (key == null) {
            return null;
        }
        ChatComponentTranslation message = new ChatComponentTranslation(key);
        message.getChatStyle()
            .setColor(EnumChatFormatting.RED);
        return message;
    }
}
