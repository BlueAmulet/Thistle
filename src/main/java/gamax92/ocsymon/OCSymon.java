package gamax92.ocsymon;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import li.cil.oc.Settings;
import li.cil.oc.api.Machine;
import net.minecraftforge.common.config.Configuration;

@Mod(modid = OCSymon.MODID, name = OCSymon.NAME, version = OCSymon.VERSION, dependencies = "required-after:OpenComputers@[1.6.0,)")
public class OCSymon {
	public static final String MODID = "ocsymon";
	public static final String NAME = "OCSymon6502";
	public static final String VERSION = "1.0.2";

	@Mod.Instance
	public static OCSymon instance;

	public static Logger log;
	private Configuration config;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		log = event.getModLog();
		config = new Configuration(event.getSuggestedConfigurationFile());

		SymonConfig.loadConfig(config);
		Machine.add(SymonArchitecture.class);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		boolean configurationIssue = false;
		if (Settings.get().eepromSize() != 4096) {
			configurationIssue = true;
			log.error("EEPROM size is no longer 4096 bytes, OCSymon will not work properly.");
		}
		if (Settings.get().eepromDataSize() != 256) {
			configurationIssue = true;
			log.error("EEPROM data size is no longer 256 bytes, OCSymon will not work properly.");
		}
		if (configurationIssue)
			log.error("Please reconfigure OpenComputers or you will run into issues.");
	}
}
