package gamax92.ocsymon;

import com.loomcom.symon.Simulator;

public class SymonVM {
	Simulator simulator;

	public SymonVM() {
		super();
		try {
			simulator = new Simulator();
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
