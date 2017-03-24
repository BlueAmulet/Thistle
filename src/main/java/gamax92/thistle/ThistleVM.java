package gamax92.thistle;

import com.loomcom.symon.Cpu;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import li.cil.oc.api.machine.Context;

public class ThistleVM {
	// The simulated machine
	public ThistleMachine machine;

	// Allocated cycles per tick
	public int cyclesPerTick;

	public ThistleVM(Context context) {
		super();
		try {
			machine = new ThistleMachine(context);
			if (context.node().network() == null) {
				// Loading from NBT
				return;
			}
			machine.getCpu().reset();
			FMLCommonHandler.instance().bus().register(this);
		} catch (Exception e) {
			Thistle.log.warn("Failed to setup Thistle", e);
		}
	}

	void run() throws Exception {
		machine.getComponentSelector().checkDelay();
		Cpu mCPU = machine.getCpu();
		while (mCPU.getCycles() > 0) {
			mCPU.step();
			if (ThistleConfig.debugCpuTraceLog)
				Thistle.log.info("[Cpu] " + mCPU.getCpuState().toTraceEvent());
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
