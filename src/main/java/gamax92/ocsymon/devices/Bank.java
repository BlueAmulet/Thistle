package gamax92.ocsymon.devices;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class Bank extends Device {

	private final static Logger logger = Logger.getLogger(Bank.class.getName());

	private ArrayList<Integer> mem = new ArrayList<Integer>();
	private int bank = 1;
	private int bankSize;
	private int memsize = 0;

	public Bank(int startAddress, int endAddress, int bankSize) throws MemoryRangeException {
		super(startAddress, endAddress, "Switchable Memory");
		this.setBankSize(bankSize);
	}

	public void init(int size) {
		this.mem.clear();
		for (int i = 0; i < size; i++)
			this.mem.add(0);
		this.setMemsize(size);
	}

	public void resize(int newsize) {
		if (newsize > this.getMemsize())
			for (int i = this.getMemsize(); i < newsize; i++)
				this.mem.add(0);
		else if (newsize < this.getMemsize())
			for (int i = this.getMemsize(); i > newsize; i--)
				this.mem.remove(i - 1);
		this.setMemsize(newsize);
	}

	public void write(int address, int data) throws MemoryAccessException {
		this.mem.set((this.getBank() * this.getBankSize()) + address, data);
	}

	public int read(int address) throws MemoryAccessException {
		return this.mem.get((this.getBank() * this.getBankSize()) + address);
	}

	public String toString() {
		return "BankMemory: " + getMemoryRange().toString();
	}

	public ArrayList<Integer> getDmaAccess() {
		return mem;
	}

	public int getBank() {
		return bank;
	}

	public void setBank(int bank) {
		this.bank = Math.min(this.memsize / this.bankSize, bank);
	}

	public int getBankSize() {
		return bankSize;
	}

	public void setBankSize(int bankSize) {
		this.bankSize = bankSize;
	}

	public int getMemsize() {
		return memsize;
	}

	public void setMemsize(int memsize) {
		this.memsize = memsize;
	}
}