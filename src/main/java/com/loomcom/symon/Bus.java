package com.loomcom.symon;

import java.util.ArrayList;

import com.loomcom.symon.devices.Device;

import gamax92.thistle.ThistleMachine;
import gamax92.thistle.api.IThistleDevice;
import gamax92.thistle.devices.BankSelector;
import li.cil.oc.api.machine.Signal;
import net.minecraft.nbt.NBTTagCompound;

/*
 * This is not an original/modified Symon file, this class exists in Symon's
 * package because it replaced an implementation of Symon's old Bus.
 */
public class Bus {

	// EEPROM data+code at $EF00-$FFFF
	private static final int EEPROM_DATA_BASE = 0xEF00;
	private static final int EEPROM_CODE_BASE = 0xF000;

	private ThistleMachine machine;
	private ArrayList<Device> deviceList = new ArrayList<Device>();

	public int read(int address) {
		address &= 0xFFFF;
		if (address < 0xE000 || address >= EEPROM_CODE_BASE) {
			int select = address >>> 12;
			int bankMask = machine.getComponentSelector().getMask();
			if (select == 15 && (bankMask & (1 << 4)) == 0) {
				return machine.getEEPROM().read(address - EEPROM_DATA_BASE) & 0xFF; // This reads EEPROM code but offset must be data
			} else if (select >= 10 && (bankMask & (1 << (select - 10))) == 0) {
				int index = address >>> 8;
				index = (0xD0 - (index & 0xF0)) | (index & 0x0F);
				IThistleDevice device = machine.getComponentSelector().getComponent(index);
				if (device != null)
					return device.readThistle(machine.getContext(), address & 0xFF) & 0xFF;
			} else {
				BankSelector banksel = machine.getBankSelector();
				int memaddr = (banksel.bankSelect[select] << 12) | (address & 0xFFF);
				if (memaddr < machine.getMemsize())
					return machine.readMem(memaddr) & 0xFF;
			}
			return 0;
		}
		for (Device device : deviceList) {
			MemoryRange memoryRange = device.getMemoryRange();
			if (address >= memoryRange.startAddress && address <= memoryRange.endAddress) {
				return device.read(address - memoryRange.startAddress) & 0xFF;
			}
		}
		return 0;
	}

	public void write(int address, int data) {
		address &= 0xFFFF;
		data &= 0xFF;
		if (address < 0xE000 || address >= EEPROM_CODE_BASE) {
			int select = address >>> 12;
			int bankMask = machine.getComponentSelector().getMask();
			if (select == 15 && (bankMask & (1 << 4)) == 0) {
				machine.getEEPROM().write(address - EEPROM_DATA_BASE, data); // This reads eeprom code but offset must be data
			} else if (select >= 10 && (bankMask & (1 << (select - 10))) == 0) {
				int index = address >>> 8;
				index = (0xD0 - (index & 0xF0)) | (index & 0x0F);
				IThistleDevice device = machine.getComponentSelector().getComponent(index);
				if (device != null)
					device.writeThistle(machine.getContext(), address & 0xFF, data);
			} else {
				BankSelector banksel = machine.getBankSelector();
				int memaddr = (banksel.bankSelect[select] << 12) | (address & 0xFFF);
				if (memaddr < machine.getMemsize())
					machine.writeMem(memaddr, (byte) data);
			}
			return;
		}
		for (Device device : deviceList) {
			MemoryRange memoryRange = device.getMemoryRange();
			if (address >= memoryRange.startAddress && address <= memoryRange.endAddress) {
				device.write(address - memoryRange.startAddress, data);
				return;
			}
		}
	}

	public void onSignal(Signal signal) {
		for (Device device : deviceList)
			device.onSignal(signal);
	}

	public void load(NBTTagCompound nbt) {
		for (Device device : deviceList)
			device.load(nbt);
	}

	public void save(NBTTagCompound nbt) {
		for (Device device : deviceList)
			device.save(nbt);
	}

	public void assertIrq() {
		machine.getCpu().assertIrq();
	}

	public void clearIrq() {
		machine.getCpu().clearIrq();
	}

	public void assertNmi() {
		machine.getCpu().assertNmi();
	}

	public void clearNmi() {
		machine.getCpu().clearNmi();
	}

	public ThistleMachine getMachine() {
		return machine;
	}

	public void setMachine(ThistleMachine machine) {
		this.machine = machine;
	}

	public void setCpu(Cpu cpu) {
		cpu.setBus(this);
	}

	public void addDevice(Device device) {
		deviceList.add(device);
		device.setBus(this);
	}
}
