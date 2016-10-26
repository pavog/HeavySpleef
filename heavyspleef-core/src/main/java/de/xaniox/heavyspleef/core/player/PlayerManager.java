/*
 * This file is part of HeavySpleef.
 * Copyright (c) 2014-2016 Matthias Werning
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
package de.xaniox.heavyspleef.core.player;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import de.xaniox.heavyspleef.core.HeavySpleef;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class PlayerManager implements Listener {
	
	private final HeavySpleef heavySpleef;
	private final Set<SpleefPlayer> onlineSpleefPlayers;
	
	public PlayerManager(HeavySpleef heavySpleef) {
		this.onlineSpleefPlayers = Sets.newLinkedHashSet();
		this.heavySpleef = heavySpleef;
		
		Bukkit.getPluginManager().registerEvents(this, heavySpleef.getPlugin());
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			SpleefPlayer spleefPlayer = new SpleefPlayer(player, heavySpleef);
			onlineSpleefPlayers.add(spleefPlayer);
		}
	}
	
	public SpleefPlayer getSpleefPlayer(final Player bukkitPlayer) {
		return getUniquePlayer(new Predicate<SpleefPlayer>() {

			@Override
			public boolean apply(SpleefPlayer input) {
				return input.getName().equalsIgnoreCase(bukkitPlayer.getName());
			}
		});
	}
	
	public SpleefPlayer getSpleefPlayer(final String name) {
		return getUniquePlayer(new Predicate<SpleefPlayer>() {

			@Override
			public boolean apply(SpleefPlayer input) {
				return input.getName().equalsIgnoreCase(name);
			}
		});
	}
	
	public SpleefPlayer getSpleefPlayer(final UUID uuid) {
		return getUniquePlayer(new Predicate<SpleefPlayer>() {

			@Override
			public boolean apply(SpleefPlayer input) {
				return input.getUniqueId().equals(uuid);
			}
		});
	}
	
	private SpleefPlayer getUniquePlayer(Predicate<SpleefPlayer> predicate) {
		for (SpleefPlayer player : onlineSpleefPlayers) {
			if (!predicate.apply(player)) {
				continue;
			}
			
			return player;
		}
		
		return null;
	}
	
	public Set<SpleefPlayer> getSpleefPlayers() {
		return Collections.unmodifiableSet(onlineSpleefPlayers);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Player player = e.getPlayer();
		
		SpleefPlayer spleefPlayer = new SpleefPlayer(player, heavySpleef);
		onlineSpleefPlayers.add(spleefPlayer);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        handlePlayerLeave(e.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent e) {
		handlePlayerLeave(e.getPlayer());
	}
	
	private void handlePlayerLeave(Player player) {
		SpleefPlayer spleefPlayer = getSpleefPlayer(player);
		
		if (spleefPlayer != null) {
			spleefPlayer.setOnline(false);
			onlineSpleefPlayers.remove(spleefPlayer);
		}
	}
	
}