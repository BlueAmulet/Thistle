package gamax92.ocsymon;

import java.util.logging.Level;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import li.cil.oc.api.machine.Context;

public class SymonVM {
	// The simulated machine
	public SymonMachine machine;

	public SymonVM(Context context) {
		super();
		try {
			machine = new SymonMachine(context);
			machine.getCpu().reset();
		} catch (Exception e) {
			OCSymon.log.warn("Failed to setup Symon", e);
		}
	}

	// The console
	public ConsoleDriver console;

	/*
	 * Perform a reset.
	 */
	private void handleReset(boolean isColdReset) {
		try {
			OCSymon.log.info("Reset requested. Resetting CPU.");
			// Reset CPU
			machine.getCpu().reset();
			// If we're doing a cold reset, clear the memory.
			if (isColdReset) {
				Memory mem = machine.getRam();
				if (mem != null) {
					mem.fill(0);
				}
			}
		} catch (MemoryAccessException ex) {
			OCSymon.log.error("Exception during simulator reset: " + ex.getMessage());
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
		if (mACIA != null && mACIA.hasTxChar()) {
			// This is thread-safe
			console.write(mACIA.txRead());
		}

		// If a key has been pressed, fill the ACIA.
		if (mACIA != null && console.hasInput() && !mACIA.hasRxChar()) {
			mACIA.rxWrite(console.read());
		}
	}

	void run() throws Exception {
		// Run 1k instructions
		for (int i = 0; i < 1000; i++)
			step();
	}
}
