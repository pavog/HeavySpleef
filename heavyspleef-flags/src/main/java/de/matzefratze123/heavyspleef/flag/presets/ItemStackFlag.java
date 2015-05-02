package de.matzefratze123.heavyspleef.flag.presets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.dom4j.Attribute;
import org.dom4j.Element;

import com.google.common.collect.Maps;
import com.sk89q.worldedit.blocks.ItemType;

import de.matzefratze123.heavyspleef.core.flag.AbstractFlag;
import de.matzefratze123.heavyspleef.core.flag.InputParseException;

public abstract class ItemStackFlag extends AbstractFlag<ItemStack> {

	@Override
	public void marshal(Element element) {
		ItemStack stack = getValue();
		marshalSerializeable(element, stack);
	}
	
	private void marshalSerializeable(Element baseElement, ConfigurationSerializable serializeable) {
		Map<String, Object> serialized = serializeable.serialize();
		if (serializeable instanceof ItemMeta) {
			baseElement.addAttribute("itemmeta", String.valueOf(true));
		}
		
		for (Entry<String, Object> entry : serialized.entrySet()) {
			Element entryElement = baseElement.addElement(entry.getKey());
			Object value = entry.getValue();
			
			if (value instanceof ItemMeta) {
				marshalSerializeable(entryElement, (ItemMeta) value);
			} else {
				serializeObject(value, entryElement);
			}
		}
	}
	
	private void serializeObject(Object value, Element element) {
		element.addAttribute("type", value.getClass().getName());
		
		if (value instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) value;
			Iterator<?> iterator = collection.iterator();
			
			while (iterator.hasNext()) {
				Object val = iterator.next();
				
				Element valElement = element.addElement("entry");
				serializeObject(val, valElement);
			}
		} else {
			element.addText(value.toString());
		}
	}

	@Override
	public void unmarshal(Element element) {
		Map<String, Object> serializedMap = Maps.newHashMap();
		unmarshalElement(element, serializedMap);
		
		ItemStack stack = ItemStack.deserialize(serializedMap);
		setValue(stack);
	}
	
	@SuppressWarnings("unchecked")
	private void unmarshalElement(Element baseElement, Map<String, Object> map) {
		List<Element> childElements = baseElement.elements();
		
		for (Element childElement : childElements) {
			String name = childElement.getName();
			Attribute itemMetaAttribute = childElement.attribute("itemmeta");
			
			Object value;
			
			if (itemMetaAttribute != null && Boolean.valueOf(itemMetaAttribute.getValue()).booleanValue()) {
				Map<String, Object> metaMap = Maps.newHashMap();
				unmarshalElement(childElement, metaMap);
				
				Material material = Material.valueOf((String) map.get("type"));
				ItemMeta metaDummy = Bukkit.getItemFactory().getItemMeta(material);
				
				Class<?> deserializationClass = metaDummy.getClass();
				
				do {
					if (!deserializationClass.isAnnotationPresent(DelegateDeserialization.class)) {
						break;
					}
					
					DelegateDeserialization annotation = deserializationClass.getAnnotation(DelegateDeserialization.class);
					deserializationClass = annotation.value();
				} while (true);
				
				ItemMeta meta;
				
				try {
					Method method = deserializationClass.getMethod("deserialize", Map.class);
					meta = (ItemMeta) method.invoke(null, metaMap);
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new IllegalStateException("Cannot deserialize item meta", e);
				}
				
				value = meta;
			} else {
				value = deserializeObject(childElement);
			}
			
			map.put(name, value);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Object deserializeObject(Element element) {
		String typeName = element.attributeValue("type");
		Class<?> type;
		
		try {
			type = Class.forName(typeName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot deserialize element " + element.getName() + " (content: '" + element.getText() + "')", e);
		}
		
		String text = element.getText();
		
		if (Integer.class.isAssignableFrom(type)) {
			return Integer.parseInt(text);
		} else if (Short.class.isAssignableFrom(type)) {
			return Short.parseShort(text);
		} else if (Byte.class.isAssignableFrom(type)) {
			return Byte.parseByte(text);
		} else if (Long.class.isAssignableFrom(type)) {
			return Long.parseLong(text);
		} else if (Float.class.isAssignableFrom(type)) {
			return Float.parseFloat(text);
		} else if (Double.class.isAssignableFrom(type)) {
			return Double.parseDouble(text);
		} else if (Character.class.isAssignableFrom(type)) {
			return text.charAt(0);
		} else if (String.class.isAssignableFrom(type)) {
			return text;
		} else if (Boolean.class.isAssignableFrom(type)) {
			return Boolean.parseBoolean(text);
		} else if (Collection.class.isAssignableFrom(type)) {
			List<Element> entries = element.elements("entry");
			Collection<Object> collection;
			
			try {
				collection = type.asSubclass(Collection.class).newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException("Cannot instantiate collection class '" + type.getName() + "'", e);
			}
			
			for (Element entryElement : entries) {
				Object deserialized = deserializeObject(entryElement);
				collection.add(deserialized);
			}
			
			return collection;
		}
		
		throw new IllegalArgumentException("Cannot deserialize type '" + type.getName() + "'");
	}

	@Override
	public ItemStack parseInput(Player player, String input) throws InputParseException {
		String components[] = input.split(" ");

		if (components.length == 0) {
			throw new InputParseException("No value was given for this itemstack flag");
		}

		int amount = 1;
		MaterialData data = parseMaterial(components[0]);

		if (components.length > 1) {
			try {
				amount = Integer.parseInt(components[1]);
			} catch (NumberFormatException e) {
				throw new InputParseException("Invalid amount '" + components[1] + "' given");
			}
		}

		ItemStack stack = data.toItemStack(amount);
		return stack;
	}
	
	@SuppressWarnings("deprecation")
	protected MaterialData parseMaterial(String str) throws InputParseException {
		String[] parts = str.split(":");
		if (parts.length == 0) {
			throw new InputParseException("No item material and data given");
		}

		Material material = getMaterialByName(parts[0]);
		byte data = 0;

		if (parts.length > 1) {
			try {
				data = Byte.parseByte(parts[1]);
			} catch (NumberFormatException nfe) {
				throw new InputParseException("Invalid data for item: '" + parts[1] + "'");
			}
		}

		return new MaterialData(material, data);
	}

	@SuppressWarnings("deprecation")
	protected Material getMaterialByName(String str) throws InputParseException {
		int id;

		try {
			// Try to parse the item id
			id = Integer.parseInt(str);
		} catch (Exception e) {
			// Hmm, failed now we try to get the material by name
			try {
				str = str.toUpperCase();
				id = ItemType.lookup(str).getID();
			} catch (Exception e1) {
				// Failed again, no suitable material found
				throw new InputParseException("Invalid item material '" + str + "'");
			}
		}

		return Material.getMaterial(id);
	}

	@Override
	public String getValueAsString() {
		return getValue().toString();
	}

}
