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
package de.matzefratze123.heavyspleef.core.extension;

import org.bukkit.Location;

import de.matzefratze123.heavyspleef.core.Game;
import de.matzefratze123.heavyspleef.core.GameManager;
import de.matzefratze123.heavyspleef.core.QuitCause;
import de.matzefratze123.heavyspleef.core.config.ConfigType;
import de.matzefratze123.heavyspleef.core.config.SignLayoutConfiguration;
import de.matzefratze123.heavyspleef.core.i18n.I18N;
import de.matzefratze123.heavyspleef.core.i18n.I18NManager;
import de.matzefratze123.heavyspleef.core.i18n.Messages;
import de.matzefratze123.heavyspleef.core.layout.SignLayout;
import de.matzefratze123.heavyspleef.core.player.SpleefPlayer;

@Extension(name = "leave-sign")
public class LeaveSignExtension extends SignExtension {

	public static final String IDENTIFIER = "leave";
	private final I18N i18n = I18NManager.getGlobal();
	
	@SuppressWarnings("unused")
	private LeaveSignExtension() {}
	
	public LeaveSignExtension(Location location) {
		super(location);
	}

	@Override
	public void onSignClick(SpleefPlayer player) {
		GameManager manager = getHeavySpleef().getGameManager();
		Game game = manager.getGame(player);
		
		if (game == null) {
			player.sendMessage(i18n.getString(Messages.Command.NOT_INGAME));
			return;
		}
		
		game.requestLose(player, QuitCause.SELF);
	}
	
	@Override
	public SignLayout retrieveSignLayout() {
		SignLayoutConfiguration config = heavySpleef.getConfiguration(ConfigType.LEAVE_SIGN_LAYOUT_CONFIG);
		return config.getLayout();
	}

}
