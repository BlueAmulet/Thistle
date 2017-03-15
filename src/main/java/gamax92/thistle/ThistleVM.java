package gamax92.thistle;

import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import li.cil.oc.api.machine.Context;

public class ThistleVM {
	// The simulated machine
	public ThistleMachine machine;

	// The console
	public ConsoleDriver console;

	// Allocated cycles per tick
	public int cyclesPerTick;

	public ThistleVM(Context context) {
		super();
		try {
			machine = new ThistleMachine(context);
			machine.getCpu().reset();
		} catch (Exception e) {
			Thistle.log.warn("Failed to setup Symon", e);
		}
	}

	/*
	 * Perform a reset.
	 */
	private void handleReset(boolean isColdReset) {
		try {
			Thistle.log.info("Reset requested. Resetting CPU.");
			// Reset CPU
			machine.getCpu().reset();
			// If we're doing a cold reset, clear the memory.
			if (isColdReset) {
				Memory mem = machine.getRam();
				if (mem != null)
					mem.fill(0);
			}
		} catch (MemoryAccessException ex) {
			Thistle.log.error("Exception during simulator reset: " + ex.getMessage());
		}
	}

	/**
	 * Perform a single step of the simulated system.
	 */
	public void step() throws MemoryAccessException {
		machine.getCpu().step();

		Acia mACIA = machine.getAcia();
		// Read from the ACIA and immediately update the console if there's
		// output ready.
		if (mACIA != null && mACIA.hasTxChar())
			console.write(mACIA.txRead()); // This is thread-safe

		// If a key has been pressed, fill the ACIA.
		if (mACIA != null && console.hasInput() && !mACIA.hasRxChar())
			mACIA.rxWrite(console.read());
	}

	void run() throws Exception {
		Cpu mCPU = machine.getCpu();
		mCPU.addCycles(cyclesPerTick);
		while (mCPU.getCycles() > 0)
			step();
	}
}
