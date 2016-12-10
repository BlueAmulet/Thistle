package gamax92.ocsymon.devices;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class BankSwitcher extends Device {

	public static final int BNKSWCH_SIZE = 2;

	private int baseAddress;
	private Bank bankMemory;

	static final int CTRL_REG = 0;
	static final int STAT_REG = 1;

	public BankSwitcher(int startAddress, Bank bankMemory) throws MemoryRangeException {
		super(startAddress, BNKSWCH_SIZE, "Bank Switcher");
		this.baseAddress = startAddress;
		this.bankMemory = bankMemory;
	}

	@Override
	public void write(int address, int data) throws MemoryAccessException {
		bankMemory.setBank(data);
	}

	@Override
	public int read(int address) throws MemoryAccessException {
		switch (address) {
		case CTRL_REG:
			return bankMemory.getBank();
		case STAT_REG:
			return bankMemory.getMemsize() / bankMemory.getBankSize();
		default:
			throw new MemoryAccessException("No register.");
		}
	}

	@Override
	public String toString() {
		return "BankSwitcher@" + String.format("%04X", baseAddress);
	}

}
