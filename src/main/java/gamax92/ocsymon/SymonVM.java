package gamax92.ocsymon;

import com.loomcom.symon.Simulator;
import com.loomcom.symon.machines.SymonMachine;

public class SymonVM {
	Simulator simulator;

	public SymonVM() {
		super();
		try {
			simulator = new Simulator(SymonMachine.class);
			simulator.machine.getCpu().reset();
		} catch (Exception e) {
			OCSymon.log.warn("Failed to setup Symon", e);
		}
	}

	void run() throws Exception {
		// Run 1k instructions
		for (int i = 0; i < 1000; i++)
			simulator.step();
	}
}
