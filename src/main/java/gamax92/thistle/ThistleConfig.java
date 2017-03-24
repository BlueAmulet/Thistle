package gamax92.thistle;

import net.minecraftforge.common.config.Configuration;

public class ThistleConfig {

	public static int clocksPerTick = 15000;

	// Debugging
	public static boolean debugCpuSlowDown = false;
	public static boolean debugCpuTraceLog = false;
	public static boolean debugMemoryReads = false;
	public static boolean debugMemoryWrites = false;
	public static boolean debugComponentReads = false;
	public static boolean debugComponentWrites = false;
	public static boolean debugDeviceReads = false;
	public static boolean debugDeviceWrites = false;
	public static boolean debugEEPROMReads = false;
	public static boolean debugEEPROMWrites = false;
	public static boolean debugComponentCalls = false;

	private static final String CATEGORY_GENERAL = "general";
	private static final String CATEGORY_DEBUG = "debug";

	public static void loadConfig(Configuration config) {
		config.load();

		clocksPerTick = config.get(CATEGORY_GENERAL, "clocksPerTick", clocksPerTick).getInt();

		// 6502 Debugging
		debugCpuSlowDown = config.get(CATEGORY_DEBUG, "cpuSlowDown", false, "Slows down the cpu for easier debugging.").getBoolean();
		debugCpuTraceLog = config.get(CATEGORY_DEBUG, "cpuTraceLog", false, "Log all opcodes the cpu runs. Use with cpuSlowDown.").getBoolean();
		debugMemoryReads = config.get(CATEGORY_DEBUG, "memoryReads", false, "Log all reads to memory.").getBoolean();
		debugMemoryWrites = config.get(CATEGORY_DEBUG, "memoryWrites", false, "Log all writes to memory.").getBoolean();
		debugComponentReads = config.get(CATEGORY_DEBUG, "componentReads", false, "Log all reads to components.").getBoolean();
		debugComponentWrites = config.get(CATEGORY_DEBUG, "componentWrites", false, "Log all writes to components.").getBoolean();
		debugDeviceReads = config.get(CATEGORY_DEBUG, "deviceReads", false, "Log all reads to devices. ($E000-$EFFF)").getBoolean();
		debugDeviceWrites = config.get(CATEGORY_DEBUG, "deviceWrites", false, "Log all writes to devices. ($E000-$EFFF)").getBoolean();
		debugEEPROMReads = config.get(CATEGORY_DEBUG, "eepromReads", false, "Log all reads to the eeprom. Use with cpuSlowDown.").getBoolean();
		debugEEPROMWrites = config.get(CATEGORY_DEBUG, "eepromWrites", false, "Log all writes to the eeprom.").getBoolean();
		debugComponentCalls = config.get(CATEGORY_DEBUG, "componentCalls", false, "Log all component method invokes.").getBoolean();

		if (config.hasChanged())
			config.save();
	}
}
