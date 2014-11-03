package gamax92.ocsymon.devices;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class BankSwitcher extends Device {

	public static final int BNKSWCH_SIZE = 1;
	
	private int baseAddress;
	private Bank bankMemory;

	public BankSwitcher(int startAddress, Bank bankMemory) throws MemoryRangeException {
		super(startAddress, startAddress + BNKSWCH_SIZE - 1, "Bank Switcher");
		this.baseAddress = startAddress;
		this.bankMemory = bankMemory;
	}

	@Override
	public void write(int address, int data) throws MemoryAccessException {
		bankMemory.setBank(data);
	}

	@Override
	public int read(int address) throws MemoryAccessException {
		return bankMemory.getBank();
	}

	@Override
	public String toString() {
		return "BankSwitcher@" + String.format("%04X", baseAddress);
	}

}
