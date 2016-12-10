package gamax92.ocsymon.devices;

import gamax92.ocsymon.OCSymon;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Level;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class Signals extends Device {

	public static final int SIGDEV_SIZE = 4;

	private int baseAddress;

	LinkedList<List<Object>> signals = new LinkedList<List<Object>>();

	static final int STATCTRL_REG = 0;
	static final int PARTSWCH_REG = 1;
	static final int PARTADDR_REG = 2;
	static final int PART_REG = 3;

	private int curPart = 0;
	private int partPos = 0;
	private byte[] part = new byte[0];
	private boolean haspart = false;

	public Signals(int startAddress) throws MemoryRangeException {
		super(startAddress, SIGDEV_SIZE, "OCSignals");
		this.baseAddress = startAddress;
	}

	public boolean queue(String name, Object[] args) {
		if (this.signals.size() < 255) {
			List<Object> signal = new ArrayList<Object>();
			signal.add(name);
			int i = 1;
			for (Object v : args)
				signal.add(v);
			signals.add(signal);
			return true;
		} else
			return false;
	}

	@Override
	public void write(int address, int data) throws MemoryAccessException {
		switch (address) {
		case STATCTRL_REG:
			signals.removeFirst();
			curPart = 0;
			haspart = false;
			break;
		case PARTSWCH_REG:
			List<Object> signal = signals.getFirst();
			int newPart = Math.min(signal.size() - 1, data);
			if (!haspart || newPart != curPart) {
				// Load the new part
				Object part = signal.get(newPart);
				if (part instanceof Integer)
					this.part = ByteBuffer.allocate(5).put((byte) 1).putInt((Integer) part).array();
				if (part instanceof Double)
					this.part = ByteBuffer.allocate(5).put((byte) 1).putInt((int) (double) (Double) part).array();
				else if (part instanceof String)
					this.part = ByteBuffer.allocate(1 + ((String) part).length()).put((byte) 2).put(((String) part).getBytes()).array();
				else {
					OCSymon.log.log(Level.WARN, "Cannot convert " + part.getClass().getSimpleName());
					this.part = new byte[0];
				}
				haspart = true;
			}
			partPos = 0;
			curPart = newPart;
			break;
		case PARTADDR_REG:
			break;
		default:
			throw new MemoryAccessException("No register.");
		}
	}

	@Override
	public int read(int address) throws MemoryAccessException {
		switch (address) {
		case STATCTRL_REG:
			return signals.size();
		case PARTSWCH_REG:
			return signals.getFirst().size();
		case PARTADDR_REG:
			return this.part.length;
		case PART_REG:
			if (partPos >= this.part.length)
				return 0;
			else
				return this.part[partPos++] & 0xFF;
		default:
			throw new MemoryAccessException("No register.");
		}
	}

	@Override
	public String toString() {
		return "Signals@" + String.format("%04X", baseAddress);
	}

}
