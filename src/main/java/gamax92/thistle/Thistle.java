package gamax92.thistle;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gamax92.thistle.util.LogMessage;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Machine;
import net.minecraftforge.common.config.Configuration;

@Mod(modid = Thistle.MODID, name = Thistle.NAME, version = Thistle.VERSION, dependencies = "required-after:OpenComputers@[1.6.0,)")
public class Thistle {
	public static final String MODID = "thistle";
	public static final String NAME = "Thistle Computer";
	public static final String VERSION = "1.0.4";

	@Mod.Instance
	public static Thistle instance;

	public static Logger log;
	private Configuration config;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		log = LogManager.getLogger(this.MODID, new LogMessage("Thistle"));
		config = new Configuration(event.getSuggestedConfigurationFile());

		ThistleConfig.loadConfig(config);
		Machine.add(ThistleArchitecture.class);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		boolean configurationIssue = false;
		if (Settings.get().eepromSize() != 4096) {
			configurationIssue = true;
			log.error("EEPROM size is no longer 4096 bytes, Thistle will not work properly.");
		}
		if (Settings.get().eepromDataSize() != 256) {
			configurationIssue = true;
			log.error("EEPROM data size is no longer 256 bytes, Thistle will not work properly.");
		}
		if (configurationIssue)
			log.error("Please reconfigure OpenComputers or you will run into issues.");

		// Register EEPROM
		InputStream romImage = this.getClass().getResourceAsStream("/assets/" + MODID + "/roms/boot.rom");
		if (romImage != null) {
			try {
				byte[] code = IOUtils.toByteArray(romImage);
				Items.registerEEPROM("Thistle", code, null, false);
			} catch (IOException e) {
				log.warn("Failed reading boot.rom, no custom EEPROMs available");
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(romImage);
			}
		} else {
			log.warn("boot.rom could not be located, no custom EEPROMs available");
		}
	}
}
