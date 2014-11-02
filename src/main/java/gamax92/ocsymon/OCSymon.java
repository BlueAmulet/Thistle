package gamax92.ocsymon;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * This mod demonstrates how to add item components, i.e. items that can be
 * placed in a computer and provide methods to it.
 */
@Mod(modid = OCSymon.MODID, name = OCSymon.NAME, version = OCSymon.VERSION, dependencies = "required-after:OpenComputers@[1.4.0,)")
public class OCSymon {
	public static final String MODID = "ocsymon";
	public static final String NAME = "OC 6502 Symon";
	public static final String VERSION = "1.0";

	@Mod.Instance
	public static OCSymon instance;

	public static Logger log;
	public static Item6502Processor cpu6502Processor;
	private int masssoundCardID;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		log = event.getModLog();
		
		cpu6502Processor = new Item6502Processor();
		GameRegistry.registerItem(cpu6502Processor, "cpu6502");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e) {
		li.cil.oc.api.Driver.add(new Driver6502Processor());
	}
}
