package gamax92.thistle;

import net.minecraftforge.common.config.Configuration;

public class ThistleConfig {

	public static int clocksPerTick = 15000;

	public static void loadConfig(Configuration config) {
		config.load();

		clocksPerTick = config.get(Configuration.CATEGORY_GENERAL, "clocksPerTick", clocksPerTick).getInt();

		if (config.hasChanged())
			config.save();
	}
}
