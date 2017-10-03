package gamax92.thistle.devices;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.input.Keyboard;

import com.google.common.base.Charsets;
import com.google.common.collect.EvictingQueue;
import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Device;
import gamax92.thistle.util.ConsoleDriver;
import gamax92.thistle.util.TSFHelper;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import net.minecraft.nbt.NBTTagCompound;

public class GeneralIO extends Device {

	private Context context;
	private EvictingQueue<Byte> inputbuf = EvictingQueue.create(255);
	private LinkedList<byte[]> signalbuf = new LinkedList<byte[]>();
	private Queue<Byte> queuebuf = new LinkedList<Byte>();
	private int signalpos = 0;
	private int queuestat = 0;
	private int irqmask = 0;
	private int nmimask = 0;

	private ConsoleDriver console;
	private byte[] utf8buf = new byte[6]; // length, pos, b1, b2, b3, b4

	private static Pattern uuidtest = Pattern.compile("^([0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})$");

	static final int GIO_INPUTCHK_REG = 0;
	static final int GIO_INPUT_REG = 1;
	static final int GIO_OUTPUTCHK_REG = 2;
	static final int GIO_OUTPUT_REG = 3;
	static final int GIO_SIGNALCHK_REG = 4;
	static final int GIO_SIGNAL_REG = 5;
	static final int GIO_QUEUESTAT_REG = 6;
	static final int GIO_QUEUE_REG = 7;
	static final int GIO_IRQMASK_REG = 8;
	static final int GIO_NMIMASK_REG = 9;

	public GeneralIO(int address) {
		super(address, 16, "General IO");
	}

	private void utf8clear(int len, int data) {
		if (utf8buf[0] > 0) {
			for (int i = 0; i < utf8buf[1]; i++)
				console.write(utf8buf[i+2] & 0xFF);
			for (int i = 0; i < utf8buf.length; i++)
				utf8buf[i]=0;
		}
		utf8buf[0]=(byte) len;
		utf8buf[2]=(byte) data;
	}

	@Override
	public int read(int address) {
		switch (address) {
		case GIO_INPUTCHK_REG:
			return inputbuf.size();
		case GIO_INPUT_REG:
			if (inputbuf.size() > 0)
				return (inputbuf.remove() & 0xFF);
			else
				return 0;
		case GIO_OUTPUTCHK_REG:
			return 0; // TODO
		case GIO_SIGNALCHK_REG:
			return signalbuf.size();
		case GIO_SIGNAL_REG:
			if (signalbuf.size() > 0) {
				byte[] signaldata = signalbuf.getFirst();
				int data = (signaldata[signalpos] & 0xFF);
				if (++signalpos >= signaldata.length) {
					signalbuf.removeFirst();
					signalpos = 0;
				}
				return data;
			} else {
				return 0;
			}
		case GIO_QUEUESTAT_REG:
			return queuestat;
		case GIO_IRQMASK_REG:
			return irqmask;
		case GIO_NMIMASK_REG:
			return nmimask;
		default:
			return 0;
		}
	}

	@Override
	public void write(int address, int data) {
		switch (address) {
		case GIO_INPUT_REG:
			if (data == 0) {
				inputbuf.clear();
			} else {
				while (data-- > 0)
					inputbuf.poll();
			}
			break;
		case GIO_OUTPUT_REG:
			if (data >= 128 && data < 192) { // Continuation
				if (utf8buf[0] == 0)
					console.write(data);
				else {
					utf8buf[utf8buf[1]+3] = (byte) data;
					utf8buf[1]++;
					if (utf8buf[0] == utf8buf[1]) {
						int unicode = ((utf8buf[2] & 0xFF) % (int) Math.pow(2, 6-utf8buf[0])) << (6*utf8buf[0]);
						for (int i=1; i<=(utf8buf[0]); i++)
							unicode += (utf8buf[i+2] & 0x3F) << (6*(utf8buf[0]-i));
						console.write(unicode);
						for (int i = 0; i < utf8buf.length; i++)
							utf8buf[i]=0;
					}
				}
			} else if (data >= 192 && data < 224) { // 2 byte
				utf8clear(1, data);
			} else if (data >= 224 && data < 240) { // 3 byte
				utf8clear(2, data);
			} else if (data >= 240 && data < 248) { // 4 byte
				utf8clear(3, data);
			} else { // Ascii or invalid
				utf8clear(0, 0);
				console.write(data);
			}
			break;
		case GIO_SIGNAL_REG:
			signalpos = 0;
			if (data == 0 || data >= signalbuf.size()) {
				signalbuf.clear();
			} else {
				while (data-- > 0)
					signalbuf.poll();
			}
			break;
		case GIO_QUEUESTAT_REG:
			if (data == 0) {
				Object[] tsfdata = TSFHelper.readArray(queuebuf, context, true);
				if (tsfdata == null || tsfdata.length < 1 || !(tsfdata[0] instanceof String)) {
					queuestat = 2;
					break;
				}
				queuestat = this.getBus().getMachine().getContext().signal((String) tsfdata[0], Arrays.copyOfRange(tsfdata, 1, tsfdata.length)) ? 0 : 1;
			} else {
				queuestat = 0xFF;
			}
			queuebuf.clear();
			break;
		case GIO_QUEUE_REG:
			queuebuf.add((byte) data);
			break;
		case GIO_IRQMASK_REG:
			irqmask = data;
			break;
		case GIO_NMIMASK_REG:
			nmimask = data;
			break;
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		console.load(nbt);
		if (nbt.hasKey("genio")) {
			NBTTagCompound genioTag = nbt.getCompoundTag("genio");
			this.signalpos = genioTag.getInteger("sigpos");
			this.queuestat = genioTag.getInteger("queuestat");
			this.irqmask = genioTag.getInteger("irqmask");
			this.nmimask = genioTag.getInteger("nmimask");
			byte[] utf8buf = genioTag.getByteArray("utf8");
			if (utf8buf.length == 6)
				this.utf8buf = utf8buf;
			signalbuf.clear();
			NBTTagCompound sigTag = genioTag.getCompoundTag("signals");
			int length = sigTag.getInteger("length");
			for (int i = 0; i < length; i++)
				signalbuf.add(sigTag.getByteArray("S" + i));
			inputbuf.clear();
			inputbuf.addAll(Arrays.asList(ArrayUtils.toObject(genioTag.getByteArray("input"))));
			queuebuf.clear();
			queuebuf.addAll(Arrays.asList(ArrayUtils.toObject(genioTag.getByteArray("queue"))));
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		console.save(nbt);
		NBTTagCompound genioTag = new NBTTagCompound();
		genioTag.setInteger("sigpos", this.signalpos);
		genioTag.setInteger("queuestat", this.queuestat);
		genioTag.setInteger("irqmask", this.irqmask);
		genioTag.setInteger("nmimask", this.nmimask);
		genioTag.setByteArray("utf8", this.utf8buf);
		NBTTagCompound sigTag = new NBTTagCompound();
		int length = signalbuf.size();
		sigTag.setInteger("length", length);
		for (int i = 0; i < length; i++)
			sigTag.setByteArray("S" + i, signalbuf.get(i));
		genioTag.setTag("signals", sigTag);
		genioTag.setByteArray("input", ArrayUtils.toPrimitive(inputbuf.toArray(new Byte[0])));
		genioTag.setByteArray("queue", ArrayUtils.toPrimitive(queuebuf.toArray(new Byte[0])));
		nbt.setTag("genio", genioTag);
	}

	public void flush() throws Exception {
		console.flush();
	}

	@Override
	public void onSignal(Signal signal) {
		String name = signal.name();
		Object[] args = signal.args();
		Cpu cpu = this.getBus().getMachine().getCpu();
		if (name.equals("key_down")) {
			int character = ((Double) args[1]).intValue();
			int lwjglcode = ((Double) args[2]).intValue();

			// Fix various key issues
			if (character == 13) // Make \r into \n
				character = 10;
			if (lwjglcode == Keyboard.KEY_BACK) // Normalize Backspace
				character = 8;
			if (lwjglcode == Keyboard.KEY_DELETE) // Normalize Delete
				character = 127;

			if (character == 0) {
				inputbuf.add((byte) 0);
				inputbuf.add((byte) lwjglcode);
			} else if (character > 0 && character < 128) {
				inputbuf.add((byte) character);
			} else {
				byte[] utf8 = String.valueOf(Character.toChars(character)).getBytes(Charsets.UTF_8);
				for (byte data : utf8)
					inputbuf.add(data);
			}
			if ((nmimask & 1) != 0)
				cpu.assertNmi();
			else if ((irqmask & 1) != 0)
				cpu.assertIrq();
		} else if (name.equals("clipboard")) {
			byte[] paste = ((String) args[1]).getBytes(Charsets.UTF_8);
			if (paste.length > 0) {
				for (byte data : paste)
					inputbuf.add(data);
				if ((nmimask & 1) != 0)
					cpu.assertNmi();
				else if ((irqmask & 1) != 0)
					cpu.assertIrq();
			}
		}
		if (signalbuf.size() < 255) {
			Queue<Byte> signaldata = new LinkedList<Byte>();
			TSFHelper.writeString(signaldata, name);
			TSFHelper.writeArray(signaldata, args, context, 0x08);
			signalbuf.add(ArrayUtils.toPrimitive(signaldata.toArray(new Byte[0])));
			if ((nmimask & 2) != 0)
				cpu.assertNmi();
			else if ((irqmask & 2) != 0)
				cpu.assertIrq();
		}
	}

	@Override
	public void setBus(Bus bus) {
		super.setBus(bus);
		this.context = getBus().getMachine().getContext();
		this.console = new ConsoleDriver((Machine) this.context);
	}
}
