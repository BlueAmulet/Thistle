/*
 * Copyright (c) 2014 Seth J. Morabito <web@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon;

import gamax92.ocsymon.ConsoleDriver;
import gamax92.ocsymon.SymonMachine;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;

/**
 * Symon Simulator Interface and Control.
 * <p/>
 * This class provides a control and I/O system for the simulated 6502 system.
 * It includes the simulated CPU itself, as well as 32KB of RAM, 16KB of ROM,
 * and a simulated ACIA for serial I/O. The ACIA is attached to a dumb terminal
 * with a basic 80x25 character display.
 */
public class Simulator {

	private final static Logger logger = Logger.getLogger(Simulator.class.getName());

	// The simulated machine
	public SymonMachine machine;

	// The console
	public ConsoleDriver console;

	public Simulator() throws Exception {
		this.machine = new SymonMachine();
	}

	/*
	 * Perform a reset.
	 */
	private void handleReset(boolean isColdReset) {
		try {
			logger.log(Level.INFO, "Reset requested. Resetting CPU.");
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
			logger.log(Level.SEVERE, "Exception during simulator reset: " + ex.getMessage());
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
}
