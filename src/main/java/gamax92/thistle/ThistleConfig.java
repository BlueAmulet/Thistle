package gamax92.thistle;

import net.minecraftforge.common.config.Configuration;

public class ThistleConfig {
	public static boolean busError = false;

	public static void loadConfig(Configuration config) {
		config.load();

		busError = config.get(Configuration.CATEGORY_GENERAL, "busError", false).getBoolean();

		if (config.hasChanged())
			config.save();
	}
}
