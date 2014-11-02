package gamax92.ocsymon;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverItem;
import net.minecraft.item.ItemStack;

public class Driver6502Processor extends DriverItem implements Processor {
	protected Driver6502Processor() {
		super(new ItemStack(OCSymon.cpu6502Processor));
	}

	// We want our item to be a cpu component, i.e. it can be placed into
	// computers' cpu slots.

	@Override
	public String slot(ItemStack stack) {
		return Slot.CPU;
	}

	@Override
	public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
		return null;
	}

	@Override
	public int supportedComponents(ItemStack stack) {
		if (stack.getItem() instanceof Item6502Processor)
			return 16;
		return 0;
	}

	@Override
	public Class<? extends Architecture> architecture(ItemStack stack) {
		if (stack.getItem() instanceof Item6502Processor)
			return SymonArchitecture.class;
		return null;
	}
}
