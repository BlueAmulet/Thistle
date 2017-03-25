package gamax92.thistle.util;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import gamax92.thistle.Thistle;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import net.minecraft.nbt.NBTTagCompound;

public class ValueManager {

	private static Map<Context, BiMap<Integer, Value>> valuestore = new HashMap();

	private static BiMap<Integer, Value> getValueMap(Context context) {
		BiMap<Integer, Value> valuemap = valuestore.get(context);
		if (valuemap == null) {
			valuemap = HashBiMap.create();
			valuestore.put(context, valuemap);
		}
		return valuemap;
	}

	private static void addValue(BiMap<Integer, Value> valuemap, Value value) {
		Integer id = value.hashCode();
		while (valuemap.containsKey(id))
			id = (int) ((Math.random()*2-1) * -((double) Integer.MIN_VALUE));
		valuemap.put(id, value);
	}

	public static Value getValue(int id, Context context) {
		return getValueMap(context).get(id);
	}

	public static int getID(Value value, Context context) {
		BiMap<Integer, Value> valuemap = getValueMap(context);
		if (!valuemap.containsValue(value))
			addValue(valuemap, value);
		return valuemap.inverse().get(value);
	}

	public static void removeValue(Value value, Context context) {
		value.dispose(context);
		getValueMap(context).inverse().remove(value);
	}

	public static void removeAll(Context context) {
		BiMap<Integer, Value> valuemap = valuestore.get(context);
		if (valuemap != null) {
			for (Value value : valuemap.values()) {
				value.dispose(context);
			}
			valuemap.clear();
			valuestore.remove(context);
		}
	}

	public static void load(NBTTagCompound nbt, Context context) {
		BiMap<Integer, Value> valuemap = getValueMap(context);
		int length = nbt.getInteger("length");
		for (int i = 0; i < length; i++) {
			try {
				Class clazz = Class.forName(nbt.getString("class" + i));
				Value value = (Value) clazz.newInstance();
				value.load(nbt.getCompoundTag("nbt" + i));
				valuemap.put(nbt.getInteger("id" + i), value);
			} catch (Exception e) {
				Thistle.log.error("Failed to restore Value from NBT", e);
			}
		}
	}

	public static void save(NBTTagCompound nbt, Context context) {
		int i = 0;
		for (Map.Entry<Integer, Value> entry : getValueMap(context).entrySet()) {
			Value value = entry.getValue();
			NBTTagCompound valueTag = new NBTTagCompound();
			value.save(valueTag);
			nbt.setString("class" + i, value.getClass().getName());
			nbt.setInteger("id", entry.getKey());
			nbt.setTag("nbt" + i, valueTag);
			i++;
		}
		nbt.setInteger("length", i);
	}
}