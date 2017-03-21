package gamax92.thistle.util;

import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class UUIDHelper {
	public static byte[] encodeUUID(UUID uuid) {
		return encodeUUID(uuid.toString());
	}

	public static byte[] encodeUUID(String uuid) {
		try {
			byte[] data = Hex.decodeHex(uuid.replaceAll(Pattern.quote("-"), "").toCharArray());
			if (data.length != 16)
				throw new RuntimeException("UUID '" + uuid + "' did not decode to 16 bytes.");
			return data;
		} catch (DecoderException e) {
			// We throw something else since this exception only occurs if the length of characters is odd.
			throw new RuntimeException("Failed to decode uuid '" + uuid + "'", e);
		}
	}
}
