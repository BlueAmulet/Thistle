package gamax92.thistle;

import com.loomcom.symon.Cpu;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import li.cil.oc.api.machine.Context;

public class ThistleVM {
	// The simulated machine
	public ThistleMachine machine;

	// Allocated cycles per tick
	public int cyclesPerTick;

	public ThistleVM(Context context) {
		super();
		FMLCommonHandler.instance().bus().register(this);
		try {
			machine = new ThistleMachine(context);
			if (context.node().network() == null) {
				// Loading from NBT
				return;
			}
			machine.getCpu().reset();
		} catch (Exception e) {
			Thistle.log.warn("Failed to setup Thistle", e);
		}
	}

	void run() throws Exception {
		machine.getComponentSelector().checkDelay();
		Cpu mCPU = machine.getCpu();
		while (mCPU.getCycles() > 0) {
			mCPU.step();
		}
		machine.getGioDev().flush();
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		Context context = machine.getContext();
		if (!context.isRunning() && !context.isPaused()) {
			FMLCommonHandler.instance().bus().unregister(this);
			return;
		}
		if (event.phase != Phase.START)
			return;
		Cpu mCPU = machine.getCpu();
		if (mCPU.getCycles() < cyclesPerTick)
			mCPU.addCycles(cyclesPerTick);
		machine.getRTC().onServerTick();
	}
}
