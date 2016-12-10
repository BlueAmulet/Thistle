package gamax92.ocsymon.devices;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class Bank extends Device {

	private final static Logger logger = Logger.getLogger(Bank.class.getName());

	private ArrayList<Integer> mem = new ArrayList<Integer>();
	private int bank = 0;
	private int bankSize;
	private int memsize = 0;

	public Bank(int startAddress, int bankSize) throws MemoryRangeException {
		super(startAddress, bankSize, "Switchable Memory");
		setBankSize(bankSize);
	}

	public void init(int size) {
		mem.clear();
		for (int i = 0; i < size; i++)
			mem.add(0);
		assert size == mem.size();
		memsize = size;
	}

	public void resize(int newsize) {
		if (newsize > memsize)
			for (int i = memsize; i < newsize; i++)
				mem.add(0);
		else if (newsize < memsize)
			for (int i = memsize; i > newsize; i--)
				mem.remove(i - 1);
		assert newsize == mem.size();
		memsize = newsize;
	}

	@Override
	public void write(int address, int data) throws MemoryAccessException {
		int realAddress = (bank * bankSize) + address;
		if (realAddress < memsize)
			mem.set(realAddress, data);
		else
			throw new MemoryAccessException(String.format("Bus write failed. No banked memory at address $%04X, bank %d", address, bank));
	}

	@Override
	public int read(int address) throws MemoryAccessException {
		int realAddress = (bank * bankSize) + address;
		if (realAddress < memsize)
			return mem.get(realAddress);
		else
			throw new MemoryAccessException(String.format("Bus read failed. No banked memory at address $%04X, bank %d", address, bank));
	}

	@Override
	public String toString() {
		return "BankMemory: " + getMemoryRange().toString();
	}

	public ArrayList<Integer> getDmaAccess() {
		return mem;
	}

	public int getBank() {
		return bank;
	}

	public void setBank(int newbank) {
		bank = Math.max(Math.min((memsize / bankSize) - 1, newbank), 0);
	}

	public int getBankSize() {
		return bankSize;
	}

	public void setBankSize(int newSize) {
		bankSize = newSize;
	}

	public int getMemsize() {
		return memsize;
	}

	public void setMemsize(int size) {
		memsize = size;
	}
}
