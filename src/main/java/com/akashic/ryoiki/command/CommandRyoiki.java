package com.akashic.ryoiki.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.arena.ArenaManager;
import com.akashic.ryoiki.domain.DomainManager;
import com.akashic.ryoiki.domain.DomainSession;

/**
 * /ryoiki expandir &lt;vazio|santuario&gt; [jogador] : casts a domain; /ryoiki encerrar : ends the caller's;
 * /ryoiki preparar : builds this world's arenas ahead of the first cast. Cast in the hand sign of someone else's
 * domain, or just after it caught you, and the two domains clash. For operators, while the domains have no
 * key and no item of their own.
 */
public final class CommandRyoiki extends CommandBase {

    @Override
    public String getCommandName() {
        return "ryoiki";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "ryoikitenkai.comando.uso";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if ("expandir".equalsIgnoreCase(args[0]) && args.length >= 2) {
            DomainType type = DomainType.byId(args[1]);
            if (type == null) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            EntityPlayerMP caster = args.length >= 3 ? getPlayer(sender, args[2]) : getCommandSenderAsPlayer(sender);
            String error = DomainManager.INSTANCE.cast(caster, type);
            if (error != null) {
                ChatComponentTranslation message = new ChatComponentTranslation(error);
                message.getChatStyle()
                    .setColor(EnumChatFormatting.RED);
                sender.addChatMessage(message);
            }
        } else if ("encerrar".equalsIgnoreCase(args[0])) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            DomainSession s = DomainManager.INSTANCE.sessionOf(player.getUniqueID());
            if (s != null) {
                DomainManager.INSTANCE.end(s, player.getServerForPlayer());
            }
        } else if ("preparar".equalsIgnoreCase(args[0])) {
            WorldServer world = getCommandSenderAsPlayer(sender).getServerForPlayer();
            for (int slot = 0; slot < ArenaManager.INSTANCE.slots(world)
                .size(); slot++) {
                String error = ArenaManager.INSTANCE.prepare(world, slot);
                if (error != null) {
                    sender.addChatMessage(new ChatComponentTranslation(error));
                    return;
                }
            }
            sender.addChatMessage(
                new ChatComponentTranslation("ryoikitenkai.comando.preparando", ArenaManager.INSTANCE.pending(world)));
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "expandir", "encerrar", "preparar");
        }
        if (args.length == 2 && "expandir".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "vazio", "santuario");
        }
        return null;
    }
}
