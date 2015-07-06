/*
 * This file is part of HeavySpleef.
 * Copyright (c) 2014-2015 matzefratze123
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.matzefratze123.heavyspleef.flag.defaults;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.matzefratze123.heavyspleef.core.Game;
import de.matzefratze123.heavyspleef.core.MetadatableItemStack;
import de.matzefratze123.heavyspleef.core.config.DefaultConfig;
import de.matzefratze123.heavyspleef.core.event.PlayerEnterQueueEvent;
import de.matzefratze123.heavyspleef.core.event.PlayerLeaveQueueEvent;
import de.matzefratze123.heavyspleef.core.event.Subscribe;
import de.matzefratze123.heavyspleef.core.event.Subscribe.Priority;
import de.matzefratze123.heavyspleef.core.flag.BukkitListener;
import de.matzefratze123.heavyspleef.core.flag.Flag;
import de.matzefratze123.heavyspleef.core.flag.Inject;
import de.matzefratze123.heavyspleef.core.i18n.Messages;
import de.matzefratze123.heavyspleef.core.player.PlayerStateHolder;
import de.matzefratze123.heavyspleef.core.player.SpleefPlayer;
import de.matzefratze123.heavyspleef.flag.presets.LocationFlag;

@Flag(name = "queuelobby")
@BukkitListener
public class FlagQueueLobby extends LocationFlag {

	private static final String LEAVE_ITEM_KEY = "leave_item_queue";
	private static final int RIGHT_HOTBAR_SLOT = 8;
	
	@Inject
	private Game game;
	@Inject
	private DefaultConfig config;
	private Set<SpleefPlayer> died;
	
	public FlagQueueLobby() {
		this.died = Sets.newHashSet();
	}
	
	@Override
	public void onFlagRemove(Game game) {
		for (SpleefPlayer player : game.getQueuedPlayers()) {
			game.unqueue(player);
		}
	}
	
	@Override
	public void getDescription(List<String> description) {
		description.add("Teleports queued players into a lobby where they cannot teleport until they left the queue");
	}
	
	@Subscribe(priority = Priority.MONITOR)
	public void onQueueEnter(PlayerEnterQueueEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		Location teleportPoint = getValue();
		
		SpleefPlayer player = event.getPlayer();
		Player bukkitPlayer = player.getBukkitPlayer();
		
		player.savePlayerState(this);
		PlayerStateHolder.applyDefaultState(bukkitPlayer);
		
		MaterialData data = config.getFlagSection().getLeaveItem();
		MetadatableItemStack stack = new MetadatableItemStack(data.toItemStack(1));
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(getI18N().getString(Messages.Player.LEAVE_QUEUE_DISPLAYNAME));
		meta.setLore(Lists.newArrayList(getI18N().getString(Messages.Player.LEAVE_QUEUE_LORE)));
		stack.setItemMeta(meta);
		
		stack.setMetadata(LEAVE_ITEM_KEY, null);
		
		bukkitPlayer.getInventory().setItem(RIGHT_HOTBAR_SLOT, stack);
		bukkitPlayer.updateInventory();
		
		bukkitPlayer.teleport(teleportPoint);
	}
	
	@Subscribe
	public void onQueueLeave(PlayerLeaveQueueEvent event) {
		SpleefPlayer player = event.getPlayer();
		
		if (died.contains(player)) {
			return;
		}
		
		PlayerStateHolder state = player.removePlayerState(this);
		if (state != null) {
			state.apply(player.getBukkitPlayer(), true);
		} else {
			//Ugh, something went wrong
			player.sendMessage(getI18N().getString(Messages.Player.ERROR_ON_INVENTORY_LOAD));
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SpleefPlayer player = getHeavySpleef().getSpleefPlayer(event.getPlayer());
		if (!game.isQueued(player)) {
			return;
		}
		
		MetadatableItemStack inHand = new MetadatableItemStack(player.getBukkitPlayer().getItemInHand());
		if (!inHand.hasItemMeta() || !inHand.getItemMeta().hasLore() || !inHand.hasMetadata(LEAVE_ITEM_KEY)) {
			return;
		}
		
		//Leave the queue mode
		game.unqueue(player);
		player.sendMessage(getI18N().getVarString(Messages.Command.REMOVED_FROM_QUEUE)
				.setVariable("game", game.getName())
				.toString());
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		SpleefPlayer player = getHeavySpleef().getSpleefPlayer(event.getPlayer());
		
		if (!game.isQueued(player) || player.getPlayerState(this) == null) {
			return;
		}
		
		event.setCancelled(true);
		player.sendMessage(getI18N().getString(Messages.Player.CANNOT_TELEPORT_IN_QUEUE));
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		SpleefPlayer player = getHeavySpleef().getSpleefPlayer(event.getEntity());
		if (!game.isQueued(player)) {
			return;
		}
		
		died.add(player);
		game.unqueue(player);
		player.sendMessage(getI18N().getVarString(Messages.Player.REMOVED_FROM_QUEUE_DEATH)
				.setVariable("game", game.getName())
				.toString());		
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		final SpleefPlayer player = getHeavySpleef().getSpleefPlayer(event.getPlayer());
		
		if (!died.contains(player)) {
			return;
		}
		
		Bukkit.getScheduler().runTaskLater(getHeavySpleef().getPlugin(), new Runnable() {
			
			@Override
			public void run() {
				if (!player.isOnline()) {
					return;
				}
				
				PlayerStateHolder state = player.removePlayerState(this);
				if (state != null) {
					state.apply(player.getBukkitPlayer(), true);
				} else {
					//Ugh, something went wrong
					player.sendMessage(getI18N().getString(Messages.Player.ERROR_ON_INVENTORY_LOAD));
				}
			}
		}, 10L);
	}

}
