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

package gamax92.thistle;

import li.cil.oc.api.machine.Context;

import java.util.ArrayList;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;

import gamax92.thistle.devices.BootROM;
import gamax92.thistle.devices.ComponentSelector;
import gamax92.thistle.devices.ComputerInfo;
import gamax92.thistle.devices.CopyEngine;
import gamax92.thistle.devices.RTC;
import gamax92.thistle.devices.BankSelector;
import gamax92.thistle.devices.ThistleIO;

public class ThistleMachine {

	// Constants used by the simulated system. These define the memory map.

	// Generic IO at $E000-$E00F
	private static final int GIODEV_BASE = 0xE000;

	// Component Mapper at $E010-$E01F
	private static final int COMPMAP_BASE = 0xE010;

	// Bank Selector at $E020-$E03F
	private static final int BANKSEL_BASE = 0xE020;

	// Copy Engine at $E040-$E04F
	private static final int COPYENG_BASE = 0xE040;

	// RTC and Timers at $E050-$E05F
	private static final int RTCTIMER_BASE = 0xE050;

	// Computer Information at $E100-$E11F
	private static final int COMPINFO_BASE = 0xE100;

	// EEPROM data+code at $EF00-$FFFF
	private static final int EEPROM_DATA_BASE = 0xEF00;
	private static final int EEPROM_CODE_BASE = 0xF000;

	// The simulated peripherals
	private final Bus bus;
	private final Cpu cpu;
	private final ThistleIO genericio;
	private final ComponentSelector compsel;
	private final BankSelector banksel;
	private final CopyEngine copyeng;
	private final ComputerInfo compinfo;
	private final RTC rtc;
	private final BootROM eeprom;
	private Context context;

	// The machine memory
	private ArrayList<Byte> mem = new ArrayList<Byte>();

	public ThistleMachine(Context context) throws Exception {
		this.context = context;
		this.bus = new Bus();
		this.cpu = new Cpu();
		this.genericio = new ThistleIO(GIODEV_BASE);
		this.compsel = new ComponentSelector(COMPMAP_BASE);
		this.banksel = new BankSelector(BANKSEL_BASE);
		this.copyeng = new CopyEngine(COPYENG_BASE);
		this.rtc = new RTC(RTCTIMER_BASE);
		this.compinfo = new ComputerInfo(COMPINFO_BASE);
		this.eeprom = new BootROM(EEPROM_DATA_BASE);

		bus.setMachine(this);
		bus.setCpu(this.cpu);
		bus.addDevice(this.genericio);
		bus.addDevice(this.compsel);
		bus.addDevice(this.banksel);
		bus.addDevice(this.copyeng);
		bus.addDevice(this.rtc);
		bus.addDevice(this.compinfo);
		bus.addDevice(this.eeprom);
	}

	public Bus getBus() {
		return bus;
	}

	public Cpu getCpu() {
		return cpu;
	}

	public ThistleIO getGioDev() {
		return genericio;
	}

	public ComponentSelector getComponentSelector() {
		return compsel;
	}

	public BankSelector getBankSelector() {
		return banksel;
	}

	public CopyEngine getCopyEngine() {
		return copyeng;
	}

	public RTC getRTC() {
		return rtc;
	}

	public ComputerInfo getCompInfo() {
		return compinfo;
	}

	public BootROM getEEPROM() {
		return eeprom;
	}

	public String getName() {
		return "Thistle";
	}

	public Context getContext() {
		return context;
	}

	public void resize(int newsize) {
		if (newsize > this.mem.size())
			for (int i = this.mem.size(); i < newsize; i++)
				this.mem.add((byte) 0xFF);
		else if (newsize < this.mem.size())
			for (int i = this.mem.size(); i > newsize; i--)
				this.mem.remove(i - 1);
	}

	public void reset() {
		// TODO: Clear mappings and restore default mappings.
	}

	public int getMemsize() {
		return this.mem.size();
	}

	public byte readMem(int address) {
		return this.mem.get(address);
	}

	public void writeMem(int address, byte data) {
		this.mem.set(address, data);
	}
}
