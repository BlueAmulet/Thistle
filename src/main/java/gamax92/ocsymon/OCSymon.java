package gamax92.ocsymon;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * This mod demonstrates how to add item components, i.e. items that can be
 * placed in a computer and provide methods to it.
 */
@Mod(modid = "ocsymon", name = "OC 6502 Symon", version = "1.0.0", dependencies = "required-after:OpenComputers@[1.4.0,)")
public class OCSymon {
	@Mod.Instance
	public static OCSymon instance;

	public static Logger log;
	public static Item6502Processor cpuPseudoProcessor;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		log = e.getModLog();

		cpuPseudoProcessor = new Item6502Processor();
		GameRegistry.registerItem(cpuPseudoProcessor, "oc:cpu_pseudo_processor");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e) {
		li.cil.oc.api.Driver.add(new Driver6502Processor());
	}
}
