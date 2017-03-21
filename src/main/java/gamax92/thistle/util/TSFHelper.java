package gamax92.thistle.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Charsets;

import gamax92.thistle.Thistle;
import li.cil.oc.api.machine.Value;

public class TSFHelper {

	private static Pattern uuidtest = Pattern.compile("^([0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})$");

	private static int b(Queue<Byte> buffer) {
		return buffer.remove() & 0xFF;
	}

	private static boolean readBoolean(Queue<Byte> buffer) {
		return buffer.remove() != 0;
	}

	private static byte readByte(Queue<Byte> buffer) {
		return buffer.remove();
	}

	private static short readShort(Queue<Byte> buffer) {
		short value = (short) (b(buffer) | (b(buffer) << 8));
		return value;
	}

	private static int readInt(Queue<Byte> buffer) {
		int value = b(buffer) | (b(buffer) << 8) | (b(buffer) << 16) | (b(buffer) << 24);
		return value;
	}

	private static long readLong(Queue<Byte> buffer) {
		long value = b(buffer) | (b(buffer) << 8) | (b(buffer) << 16) | (b(buffer) << 24) | (b(buffer) << 32) | (b(buffer) << 40) | (b(buffer) << 48) | (b(buffer) << 56);
		return value;
	}

	private static double readDouble(Queue<Byte> buffer) {
		return Double.longBitsToDouble(readLong(buffer));
	}

	private static double readFixed(Queue<Byte> buffer) {
		return readInt(buffer) / ((double) 0x10000);
	}

	private static byte[] readByteArray(Queue<Byte> buffer) {
		int length = (buffer.remove() & 0xFF) | ((buffer.remove() & 0xFF) << 8);
		byte[] data = new byte[length];
		for (int i = 0; i < length; i++)
			data[i] = buffer.remove();
		return data;
	}

	private static UUID readUUID(Queue<Byte> buffer) {
		byte[] uuid = new byte[16];
		for (int i = 0; i < uuid.length; i++)
			uuid[i] = buffer.remove();
		char[] hex = Hex.encodeHex(uuid, true);
		StringBuilder address = new StringBuilder();
		for (int i = 0; i < hex.length; i++) {
			address.append(hex[i]);
			if (i == 7 || i == 11 || i == 15 || i == 19)
				address.append('-');
		}
		return UUID.fromString(address.toString());
	}

	private static Map readArrayMap(Queue<Byte> buffer, int conversion) {
		Map arrayMap = new HashMap();
		Object[] pairs = readArray(buffer, conversion);
		for (int i = 0; i < pairs.length; i++) {
			arrayMap.put(i, pairs[i]);
		}
		return arrayMap;
	}

	private static Map readMap(Queue<Byte> buffer, int conversion) {
		Map map = new HashMap();
		Object[] pairs = readArray(buffer, conversion);
		if ((pairs.length % 2) == 1)
			throw new IllegalArgumentException("key with no value");
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i], pairs[i+1]);
		}
		return map;
	}

	private static Object readValue(Queue<Byte> buffer) {
		int id = readInt(buffer);
		Value value = ValueManager.getValue(id);
		if (value == null)
			throw new IllegalArgumentException("no Value available with id: " + id);
		return value;
	}

	public static byte[] bufferToArray(Queue<Byte> buffer) {
		return ArrayUtils.toPrimitive(buffer.toArray(new Byte[0]));
	}

	public static Object[] readArray(Queue<Byte> buffer, int conversion) {
		List<Object> output = new ArrayList<Object>();
		try {
			while (true) {
				int tag = buffer.remove() & 0xFF;
				switch (tag) {
				case 0:
					return output.toArray();
				case 1:
					output.add(null);
					break;
				case 2:
					output.add(readBoolean(buffer));
					break;
				case 3:
					output.add(readByte(buffer));
					break;
				case 4:
					output.add(readShort(buffer));
					break;
				case 5:
					output.add(readInt(buffer));
					break;
				case 6:
					output.add(readLong(buffer));
					break;
				case 7:
					output.add(readDouble(buffer));
					break;
				case 8:
					output.add(readFixed(buffer));
					break;
				case 9:
					output.add(readByteArray(buffer));
					break;
				case 10:
					output.add(new String(readByteArray(buffer), Charsets.UTF_8));
					break;
				case 11:
					UUID uuid = readUUID(buffer);
					output.add(((conversion & 0x20) != 0) ? uuid.toString() : uuid);
					break;
				case 12:
					output.add(((conversion & 0x10) != 0) ? readArrayMap(buffer, conversion) : readArray(buffer, conversion));
					break;
				case 13:
					output.add(readMap(buffer, conversion));
					break;
				case 14:
					output.add(readValue(buffer));
					break;
				default:
					throw new IllegalArgumentException(String.format("Invalid tag %04X", tag));
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static Object[] readArray(Queue<Byte> buffer, boolean convertUUID) {
		return readArray(buffer, convertUUID ? 0x20 : 0);
	}

	public static Object[] readArray(Queue<Byte> buffer) {
		return readArray(buffer, 0);
	}

	private static void writeThing(Queue<Byte> buffer, Object thing, int conversion) {
		if (((conversion & 8) != 0) && thing instanceof String && uuidtest.matcher((String) thing).matches())
			thing = UUID.fromString((String) thing);
		if ((conversion & 4) != 0 && (thing instanceof Byte || thing instanceof Short))
			thing = Integer.valueOf(((Number) thing).intValue());
		if (thing == null)
			writeNull(buffer);
		else if (thing instanceof Boolean)
			writeBoolean(buffer, (Boolean) thing);
		else if (thing instanceof Byte)
			writeByte(buffer, (Byte) thing);
		else if (thing instanceof Short)
			writeShort(buffer, (Short) thing);
		else if (thing instanceof Integer)
			writeInt(buffer, (Integer) thing);
		else if (thing instanceof Long)
			writeLong(buffer, (Long) thing);
		else if (thing instanceof Double) {
			double value = (Double) thing;
			if ((conversion & 2) != 0)
				writeFixed(buffer, value);
			else if ((conversion & 1) != 0)
				writeInt(buffer, (int) Math.floor(value));
			else
				writeDouble(buffer, value);
		} else if (thing instanceof byte[])
			writeByteArray(buffer, (byte[]) thing);
		else if (thing instanceof Byte[])
			writeByteArray(buffer, ArrayUtils.toPrimitive((Byte[]) thing));
		else if (thing instanceof String)
			writeString(buffer, (String) thing);
		else if (thing instanceof UUID)
			writeUUID(buffer, (UUID) thing);
		else if (thing instanceof Object[]) {
			buffer.add((byte) 12);
			writeArray(buffer, (Object[]) thing, conversion);
			writeEnd(buffer);
		} else if (thing instanceof Map)
			writeMap(buffer, (Map) thing, conversion);
		else if (thing instanceof Value)
			writeValue(buffer, (Value) thing);
		else
			Thistle.log.warn("Don't know how to TSF encode a " + thing.toString() + " (" + thing.getClass().getName() + "), please report this to Thistle's author.");
	}

	public static void writeEnd(Queue<Byte> buffer) {
		buffer.add((byte) 0);
	}

	public static void writeNull(Queue<Byte> buffer) {
		buffer.add((byte) 1);
	}

	public static void writeBoolean(Queue<Byte> buffer, boolean value) {
		buffer.add((byte) 2);
		buffer.add((byte) (value ? 1 : 0));
	}

	public static void writeByte(Queue<Byte> buffer, byte value) {
		buffer.add((byte) 3);
		buffer.add(value);
	}

	public static void writeShort(Queue<Byte> buffer, short value) {
		buffer.add((byte) 4);
		buffer.add((byte) (value & 0xFF));
		buffer.add((byte) ((value >>> 8) & 0xFF));
	}

	public static void writeInt(Queue<Byte> buffer, int value) {
		buffer.add((byte) 5);
		buffer.add((byte) (value & 0xFF));
		buffer.add((byte) ((value >>> 8) & 0xFF));
		buffer.add((byte) ((value >>> 16) & 0xFF));
		buffer.add((byte) ((value >>> 24) & 0xFF));
	}

	public static void writeLong(Queue<Byte> buffer, long value) {
		buffer.add((byte) 6);
		buffer.add((byte) (value & 0xFF));
		buffer.add((byte) ((value >>> 8) & 0xFF));
		buffer.add((byte) ((value >>> 16) & 0xFF));
		buffer.add((byte) ((value >>> 24) & 0xFF));
		buffer.add((byte) ((value >>> 32) & 0xFF));
		buffer.add((byte) ((value >>> 40) & 0xFF));
		buffer.add((byte) ((value >>> 48) & 0xFF));
		buffer.add((byte) ((value >>> 56) & 0xFF));
	}

	public static void writeDouble(Queue<Byte> buffer, double num) {
		long value = Double.doubleToRawLongBits(num);
		buffer.add((byte) 7);
		buffer.add((byte) (value & 0xFF));
		buffer.add((byte) ((value >>> 8) & 0xFF));
		buffer.add((byte) ((value >>> 16) & 0xFF));
		buffer.add((byte) ((value >>> 24) & 0xFF));
		buffer.add((byte) ((value >>> 32) & 0xFF));
		buffer.add((byte) ((value >>> 40) & 0xFF));
		buffer.add((byte) ((value >>> 48) & 0xFF));
		buffer.add((byte) ((value >>> 56) & 0xFF));
	}

	public static void writeFixed(Queue<Byte> buffer, double num) {
		int value = (int) (num * 0x10000);
		buffer.add((byte) 8);
		buffer.add((byte) (value & 0xFF));
		buffer.add((byte) ((value >>> 8) & 0xFF));
		buffer.add((byte) ((value >>> 16) & 0xFF));
		buffer.add((byte) ((value >>> 24) & 0xFF));
	}

	public static void writeByteArray(Queue<Byte> buffer, byte[] data) {
		int length = Math.min(data.length, 0xFFFF);
		buffer.add((byte) 9);
		buffer.add((byte) (length & 0xFF));
		buffer.add((byte) (length >>> 8));
		for (int i = 0; i < length; i++) {
			buffer.add(data[i]);
		}
	}

	public static void writeString(Queue<Byte> buffer, String str) {
		byte data[] = str.getBytes(Charsets.UTF_8);
		int length = Math.min(data.length, 0xFFFF);
		buffer.add((byte) 10);
		buffer.add((byte) (length & 0xFF));
		buffer.add((byte) (length >>> 8));
		for (int i = 0; i < length; i++) {
			buffer.add(data[i]);
		}
	}

	public static void writeUUID(Queue<Byte> buffer, UUID uuid) {
		buffer.add((byte) 11);
		byte data[] = UUIDHelper.encodeUUID(uuid.toString());
		for (int i = 0; i < data.length; i++)
			buffer.add(data[i]);
	}

	public static void writeMap(Queue<Byte> buffer, Map<?,?> map, int conversion) {
		buffer.add((byte) 13);
		for (Map.Entry entry : map.entrySet()) {
			writeThing(buffer, entry.getKey(), conversion);
			writeThing(buffer, entry.getValue(), conversion);
		}
		writeEnd(buffer);
	}

	public static void writeMap(Queue<Byte> buffer, Map map) {
		writeMap(buffer, map, 0);
	}

	public static void writeValue(Queue<Byte> buffer, Value value) {
		buffer.add((byte) 14);
		int id = ValueManager.getID(value);
		buffer.add((byte) (id & 0xFF));
		buffer.add((byte) ((id >>> 8) & 0xFF));
		buffer.add((byte) ((id >>> 16) & 0xFF));
		buffer.add((byte) ((id >>> 24) & 0xFF));

	}

	public static void writeArray(Queue<Byte> buffer, Object[] input, int conversion) {
		for (int i = 0; i < input.length; i++) {
			writeThing(buffer, input[i], conversion);
		}
	}

	public static void writeArray(Queue<Byte> buffer, Object[] input) {
		writeArray(buffer, input, 0);
	}
}
