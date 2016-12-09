package gamax92.ocsymon;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import li.cil.oc.api.Machine;

@Mod(modid = OCSymon.MODID, name = OCSymon.NAME, version = OCSymon.VERSION, dependencies = "required-after:OpenComputers@[1.5.0,)")
public class OCSymon {
	public static final String MODID = "ocsymon";
	public static final String NAME = "OCSymon6502";
	public static final String VERSION = "1.0.2";

	@Mod.Instance
	public static OCSymon instance;

	public static Logger log;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		log = event.getModLog();

		Machine.add(SymonArchitecture.class);
	}
}
