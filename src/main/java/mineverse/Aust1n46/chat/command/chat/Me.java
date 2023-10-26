package mineverse.Aust1n46.chat.command.chat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import com.comphenix.protocol.events.PacketContainer;

import me.clip.placeholderapi.PlaceholderAPI;
import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.api.events.VentureChatEvent;
import mineverse.Aust1n46.chat.channel.ChatChannel;
import mineverse.Aust1n46.chat.localization.LocalizedMessage;
import mineverse.Aust1n46.chat.utilities.Format;

public class Me extends Command {

	private MineverseChat plugin = MineverseChat.getInstance();
	public Me() {
		super("me");
	}

	@Override
	public boolean execute(CommandSender sender, String command, String[] args) {
		if (sender.hasPermission("venturechat.me")) {
			if (args.length > 0) {
				String msg = "";
				for (int x = 0; x < args.length; x++)
					if (args[x].length() > 0)
						msg += " " + args[x];
				if (sender instanceof Player && MineverseChatAPI.getOnlineMineverseChatPlayer((Player) sender).hasFilter()) {
					msg = Format.FilterChat(msg);
				}
				if (sender.hasPermission("venturechat.color.legacy")) {
					msg = Format.FormatStringLegacyColor(msg);
				}
				if (sender.hasPermission("venturechat.color"))
					msg = Format.FormatStringColor(msg);
				if (sender.hasPermission("venturechat.format"))
					msg = Format.FormatString(msg);
				if (sender instanceof Player) {
					Player p = (Player) sender;
					//Format.broadcastToServer("* " + p.getDisplayName() + msg);
					SendEmote(sender, args);
					return true;
				}
				//Format.broadcastToServer("* " + sender.getName() + msg);
				SendEmote(sender, args);
				return true;
			}
			sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/me").replace("{args}", "[message]"));
			return true;
		}
		sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
		return true;
	}

	public void SendEmote(CommandSender sender, String[] args) {
		boolean bungee = false;
		String chat = "";
		Boolean filterthis = true;
		for (int x = 0; x < args.length; x++)
					if (args[x].length() > 0)
						chat += " " + args[x];
		String format;
		Set<Player> recipients = new HashSet<Player>(plugin.getServer().getOnlinePlayers());
		MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer((Player) sender);
		ChatChannel eventChannel = mcp.getCurrentChannel();

		Location locreceip;
		Location locsender = mcp.getPlayer().getLocation();
		Location diff;
		mcp.addListening(eventChannel.getName());
		Double chDistance = (double) 0;
		String curColor = "";

		if(eventChannel.hasSpeakPermission() && !mcp.getPlayer().hasPermission(eventChannel.getSpeakPermission())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_SPEAK_PERMISSIONS.toString());
			mcp.setQuickChat(false);
			return;
		}

		curColor = eventChannel.getChatColor();
		bungee = eventChannel.getBungee();

		long dateTimeSeconds = System.currentTimeMillis() / Format.MILLISECONDS_PER_SECOND;
		int chCooldown = 0;
		if(eventChannel.hasCooldown()) {
			chCooldown = eventChannel.getCooldown();
		}
		try {
			if (mcp.hasCooldown(eventChannel)) {
				long cooldownTime = mcp.getCooldowns().get(eventChannel).longValue();
				if (dateTimeSeconds < cooldownTime) {
					long remainingCooldownTime = cooldownTime - dateTimeSeconds;
					String cooldownString = Format.parseTimeStringFromMillis(remainingCooldownTime * Format.MILLISECONDS_PER_SECOND);
					mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_COOLDOWN.toString()
							.replace("{cooldown}", cooldownString));
					mcp.setQuickChat(false);
					bungee = false;
					return;
				}
			}
			if (eventChannel.hasCooldown()) {
				if (!mcp.getPlayer().hasPermission("venturechat.cooldown.bypass")) {
					mcp.addCooldown(eventChannel, dateTimeSeconds + chCooldown);
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		if(eventChannel.hasDistance()) {
			chDistance = eventChannel.getDistance();
		}

		//format = Format.FormatStringAll(eventChannel.getFormat());
		format = Format.FormatStringAll((String) "{venturechat_channel_color}* {vault_prefix}{player_displayname}");
		filterthis = eventChannel.isFiltered();
		if(filterthis) {
			if(mcp.hasFilter()) {
				chat = Format.FilterChat(chat);
			}
		}

		PluginManager pluginManager = plugin.getServer().getPluginManager();
		for(MineverseChatPlayer p : MineverseChatAPI.getOnlineMineverseChatPlayers()) {
			if(p.getPlayer() != mcp.getPlayer()) {
				if(!p.isListening(eventChannel.getName())) {
					recipients.remove(p.getPlayer());
					//recipientCount--;
					continue;
				}

				if(chDistance > (double) 0 && !bungee && !p.getRangedSpy()) {
					locreceip = p.getPlayer().getLocation();
					if(locreceip.getWorld() == mcp.getPlayer().getWorld()) {
						diff = locreceip.subtract(locsender);
						if(Math.abs(diff.getX()) > chDistance || Math.abs(diff.getZ()) > chDistance || Math.abs(diff.getY()) > chDistance) {
							recipients.remove(p.getPlayer());
							//recipientCount--;
							continue;
						}
						if(!mcp.getPlayer().canSee(p.getPlayer())) {
							//recipientCount--;
							continue;
						}
					}
					else {
						recipients.remove(p.getPlayer());
						//recipientCount--;
						continue;
					}
				}
				if(!mcp.getPlayer().canSee(p.getPlayer())) {
					//recipientCount--;
					continue;
				}
			}
		}

		if(mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
			chat = Format.FormatStringLegacyColor(chat);
		}
		if(mcp.getPlayer().hasPermission("venturechat.color")) {
			chat = Format.FormatStringColor(chat);
		}
		if(mcp.getPlayer().hasPermission("venturechat.format")) {
			chat = Format.FormatString(chat);
		}
		if(curColor.equalsIgnoreCase("None")) {
			// Format the placeholders and their color codes to determine the last color code to use for the chat message color
			chat = Format.getLastCode(Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), format))) + chat;
		}
		else {
			chat = curColor + chat;
		}

		String globalJSON = Format.convertToJson(mcp, format, chat); 
		format = Format.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), Format.FormatStringAll(format)));
		String message = Format.stripColor(format + chat); // UTF-8 encoding issues.
		int hash = message.hashCode();

		VentureChatEvent ventureChatEvent = new VentureChatEvent(mcp, mcp.getName(), mcp.getNickname(), MineverseChat.getVaultPermission().getPrimaryGroup(mcp.getPlayer()), eventChannel, recipients, recipients.size(), format, chat, globalJSON, hash, bungee);
		//Bukkit.getServer().getPluginManager().callEvent(ventureChatEvent);
		//handleVentureChatEvent(ventureChatEvent);
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Bukkit.getServer().getPluginManager().callEvent(ventureChatEvent);
				handleVentureChatEvent(ventureChatEvent);
			}
		});
		mcp.setQuickChat(false);
	}

	public void handleVentureChatEvent(VentureChatEvent event) {
		MineverseChatPlayer mcp = event.getMineverseChatPlayer();
		ChatChannel channel = event.getChannel();
		Set<Player> recipients = event.getRecipients();
		int recipientCount = event.getRecipientCount();
		String format = event.getFormat();
		String chat = event.getChat();
		String consoleChat = event.getConsoleChat();
		String globalJSON = event.getGlobalJSON();
		int hash = event.getHash();
		boolean bungee = event.isBungee();
		
		if(!bungee) {
			if(recipientCount == 1) {
				if(!plugin.getConfig().getString("emptychannelalert", "&6No one is listening to you.").equals("")) {
					mcp.getPlayer().sendMessage(Format.FormatStringAll(plugin.getConfig().getString("emptychannelalert", "&6No one is listening to you.")));	
				}
			}
			for(Player p : recipients) {
				String json = Format.formatModerationGUI(globalJSON, p, mcp.getName(), channel.getName(), hash);
				PacketContainer packet = Format.createPacketPlayOutChat(json);
				Format.sendPacketPlayOutChat(p, packet);
			}
			//Bukkit.getConsoleSender().sendMessage(consoleChat);
			Bukkit.getConsoleSender().sendMessage(format);
			return;
		}
		else {
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(byteOutStream);
			try {
				out.writeUTF("Chat");
				out.writeUTF(channel.getName());
				out.writeUTF(mcp.getName());
				out.writeUTF(mcp.getUUID().toString());
				out.writeBoolean(mcp.getBungeeToggle());
				out.writeInt(hash);
				out.writeUTF(format);
				out.writeUTF(chat);
				if(plugin.getConfig().getString("loglevel", "info").equals("debug")) {
					System.out.println(out.size() + " size bytes without json");
				}
				out.writeUTF(globalJSON);
				if(plugin.getConfig().getString("loglevel", "info").equals("debug")) {
					System.out.println(out.size() + " bytes size with json");
				}
				out.writeUTF(MineverseChat.getVaultPermission().getPrimaryGroup(mcp.getPlayer()));
				out.writeUTF(mcp.getNickname());
				mcp.getPlayer().sendPluginMessage(plugin, MineverseChat.PLUGIN_MESSAGING_CHANNEL, byteOutStream.toByteArray());
				out.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return;
		}
	}
}
