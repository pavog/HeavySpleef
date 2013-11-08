/**
 *   HeavySpleef - Advanced spleef plugin for bukkit
 *   
 *   Copyright (C) 2013 matzefratze123
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.matzefratze123.heavyspleef.command;

import org.bukkit.command.CommandSender;

import de.matzefratze123.heavyspleef.command.UserType.Type;
import de.matzefratze123.heavyspleef.core.GameManager;
import de.matzefratze123.heavyspleef.core.Game;
import de.matzefratze123.heavyspleef.core.GameState;
import de.matzefratze123.heavyspleef.core.StopCause;
import de.matzefratze123.heavyspleef.util.Permissions;

@UserType(Type.ADMIN)
public class CommandDelete extends HSCommand {

	public CommandDelete() {
		setMaxArgs(1);
		setMinArgs(1);
		setPermission(Permissions.DELETE_GAME);
		setUsage("/spleef delete <name>");
		setHelp("Deletes a game");
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!GameManager.hasGame(args[0])) {
			sender.sendMessage(_("arenaDoesntExists"));
			return;
		}
		
		Game game = GameManager.getGame(args[0]);
		if (game.getGameState() == GameState.INGAME || game.getGameState() == GameState.COUNTING || game.getGameState() == GameState.LOBBY) {
			game.stop(StopCause.STOP);
		}
		
		game.getQueue().clear();
		GameManager.deleteGame(args[0]);
		sender.sendMessage(_("gameDeleted"));
	}

}