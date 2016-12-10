/*
 * Copyright (c) 2014 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
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

package gamax92.ocsymon;

import gamax92.ocsymon.devices.Bank;
import gamax92.ocsymon.devices.BankSwitcher;
import gamax92.ocsymon.devices.Signals;
import li.cil.oc.api.machine.Context;

import java.io.InputStream;
import java.util.logging.Logger;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Acia6551;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.devices.Pia;
import com.loomcom.symon.devices.Via6522;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class SymonMachine {

	private final static Logger logger = Logger.getLogger(SymonMachine.class.getName());

	// Constants used by the simulated system. These define the memory map.
	private static final int BUS_BOTTOM = 0x0000;
	private static final int BUS_TOP = 0xffff;

	// 16K of RAM at $0000-$3FFF
	private static final int MEMORY_BASE = 0x0000;
	private static final int MEMORY_SIZE = 0x4000;

	// 16K of switchable RAM at $4000-$7FFF
	private static final int BANK_BASE = 0x4000;
	private static final int BANK_SIZE = 0x4000;

	// PIA at $8000-$800F
	private static final int PIA_BASE = 0x8000;

	// ACIA at $8800-$8803
	private static final int ACIA_BASE = 0x8800;

	// Bank Switcher at $8804-$8805
	private static final int BNKSWCH_BASE = 0x8804;

	// Signal Device at $8806-$8809
	private static final int SIGDEV_BASE = 0x8806;

	// 16KB ROM at $C000-$FFFF
	private static final int ROM_BASE = 0xC000;
	private static final int ROM_SIZE = 0x4000;

	// The simulated peripherals
	private final Bus bus;
	private final Cpu cpu;
	private final Memory ram;
	private final Bank bank;
	private final Pia pia;
	private final Acia acia;
	private final BankSwitcher bnkswch;
	private final Signals sigdev;
	private Memory rom;
	private Context context;

	public SymonMachine(Context context) throws Exception {
		this.context = context;
		this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
		this.cpu = new Cpu();
		this.ram = new Memory(MEMORY_BASE, MEMORY_SIZE, false);
		this.bank = new Bank(BANK_BASE, BANK_SIZE);
		this.pia = new Via6522(PIA_BASE);
		this.acia = new Acia6551(ACIA_BASE);
		this.bnkswch = new BankSwitcher(BNKSWCH_BASE, this.bank);
		this.sigdev = new Signals(SIGDEV_BASE);

		bus.setMachine(this);
		bus.setCpu(cpu);
		bus.addDevice(ram);
		bus.addDevice(bank);
		bus.addDevice(pia);
		bus.addDevice(acia);
		bus.addDevice(bnkswch);
		bus.addDevice(sigdev);

		// TODO: Make this configurable, of course.
		InputStream romImage = this.getClass().getResourceAsStream("/assets/ocsymon/roms/boot.rom");
		if (romImage != null) {
			logger.info("Loading ROM image from file: boot.rom");
			this.rom = Memory.makeROM(ROM_BASE, ROM_SIZE, romImage);
		} else {
			logger.info("Default ROM file \"boot.rom\" not found.");
			throw new Exception("boot.rom not found");
		}

		bus.addDevice(rom);

	}

	public Bus getBus() {
		return bus;
	}

	public Cpu getCpu() {
		return cpu;
	}

	public Memory getRam() {
		return ram;
	}

	public Acia getAcia() {
		return acia;
	}

	public Bank getBank() {
		return bank;
	}

	public Signals getSigDev() {
		return sigdev;
	}

	public Pia getPia() {
		return pia;
	}

	public Memory getRom() {
		return rom;
	}

	public void setRom(Memory rom) throws MemoryRangeException {
		if (this.rom != null) {
			bus.removeDevice(this.rom);
		}
		this.rom = rom;
		bus.addDevice(this.rom);
	}

	public int getRomBase() {
		return ROM_BASE;
	}

	public int getRomSize() {
		return ROM_SIZE;
	}

	public int getMemorySize() {
		return MEMORY_SIZE;
	}

	public String getName() {
		return "Symon";
	}

	public Context getContext() {
		return context;
	}
}
