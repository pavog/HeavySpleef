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
package de.xaniox.heavyspleef.flag.presets;

import de.xaniox.heavyspleef.core.flag.InputParseException;
import de.xaniox.heavyspleef.core.player.SpleefPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.dom4j.Element;

import java.util.List;

public abstract class LocationListFlag extends ListFlag<Location> {
	
	@Override
	public List<Location> parseInput(SpleefPlayer player, String input) throws InputParseException {
		throw new InputParseException("Multiple locations cannot be parsed");
	}
	
	@Override
	public void marshalListItem(Element element, Location item) {
		Element worldElement = element.addElement("world");
		Element xElement = element.addElement("x");
		Element yElement = element.addElement("y");
		Element zElement = element.addElement("z");
		
		worldElement.addText(item.getWorld().getName());
		xElement.addText(String.valueOf(item.getX()));
		yElement.addText(String.valueOf(item.getY()));
		zElement.addText(String.valueOf(item.getZ()));
		
		if (item.getYaw() != 0f) {
			element.addElement("yaw").addText(String.valueOf(item.getYaw()));
		}
		if (item.getPitch() != 0f) {
			element.addElement("pitch").addText(String.valueOf(item.getPitch()));
		}
	}

	@Override
	public Location unmarshalListItem(Element element) {
		Element worldElement = element.element("world");
		Element xElement = element.element("x");
		Element yElement = element.element("y");
		Element zElement = element.element("z");
		Element yawElement = element.element("yaw");
		Element pitchElement = element.element("pitch");
		
		World world = Bukkit.getWorld(worldElement.getText());
		double x = Double.parseDouble(xElement.getText());
		double y = Double.parseDouble(yElement.getText());
		double z = Double.parseDouble(zElement.getText());
		float yaw = 0f;
		float pitch = 0f;
		
		if (yawElement != null) {
			yaw = Float.parseFloat(yawElement.getText());
		}
		if (pitchElement != null) {
			pitch = Float.parseFloat(pitchElement.getText());
		}
		
		Location location = new Location(world, x, y, z, yaw, pitch);
		return location;
	}
	
	@Override
	public String getListItemAsString(Location location) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		builder.append(location.getBlockX());
		builder.append(',');
		builder.append(location.getBlockY());
		builder.append(',');
		builder.append(location.getBlockZ());
		builder.append(')');
		
		return builder.toString();
	}

	@Override
	public ListInputParser<Location> createParser() {
		//Location lists cannot be parsed by a string
		return null;
	}

}