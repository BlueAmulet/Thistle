package gamax92.ocsymon;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.server.PacketSender;
import net.minecraft.nbt.NBTTagCompound;

/*
 * So, Symon communicates via a dummy terminal (It used JTerminal, which acts
 * like a VT100 terminal) However, Because I don't feel like importing all of
 * that, and because I can't display a Swing Terminal on top of the screen, we
 * use this instead.
 */
public class ConsoleDriver {

	private Machine machine;

	private String gpuADDR;
	private String screenADDR;

	private boolean canWrite = false;
	private boolean canRead = false;

	private LinkedList<Integer> fifo = new LinkedList<Integer>();
	private LinkedList<Integer> databuf = new LinkedList<Integer>();

	private int X = 1;
	private int Y = 1;
	private int W;
	private int H;

	private boolean parseANSI = false;
	private boolean ansiDetect = false;
	private StringBuffer ansiCode = new StringBuffer();
	
	private boolean cursor = false;
	private long lastTime = System.currentTimeMillis();
	private int cursorBG;
	private int cursorFG;
	private char cursorChar;

	public ConsoleDriver(Machine machine) {
		this.machine = machine;
		if (!machine.isRunning()) {
			Map<String, String> components = this.machine.components();
			Iterator<Map.Entry<String, String>> entries = components.entrySet().iterator();

			while (entries.hasNext()) {
				Map.Entry<String, String> entry = entries.next();
				String address = entry.getKey();
				String type = entry.getValue();
				if (type.equals("gpu") && gpuADDR == null) {
					gpuADDR = address;
				}
				if (type.equals("screen") && screenADDR == null) {
					screenADDR = address;
				}
			}
			if (gpuADDR != null && screenADDR != null) {
				try {
					canWrite = true;
					// Attempt to setup the GPU
					for (int i = -1; i >= -6; i--)
						databuf.add(i);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// TODO: Needs more things
	public void load(NBTTagCompound nbt) {
		// Restore Console
		if (nbt.hasKey("console")) {
			NBTTagCompound consoleTag = nbt.getCompoundTag("console");
			this.canRead = consoleTag.getBoolean("canRead");
			this.canWrite = consoleTag.getBoolean("canWrite");
			this.gpuADDR = consoleTag.getString("gpuADDR");
			this.screenADDR = consoleTag.getString("screenADDR");
			this.cursor = consoleTag.getBoolean("cursor");
			this.X = consoleTag.getInteger("X");
			this.Y = consoleTag.getInteger("Y");
			this.W = consoleTag.getInteger("W");
			this.H = consoleTag.getInteger("H");
			int[] fifo = consoleTag.getIntArray("fifo");
			this.fifo.clear();
			for (int v : fifo)
				this.fifo.add(v);
			int[] databuf = consoleTag.getIntArray("data");
			this.databuf.clear();
			for (int v : databuf)
				this.databuf.add(v);
		}
	}

	public void save(NBTTagCompound nbt) {
		// Persist Console
		NBTTagCompound consoleTag = new NBTTagCompound();
		consoleTag.setBoolean("canRead", this.canRead);
		consoleTag.setBoolean("canWrite", this.canWrite);
		consoleTag.setString("gpuADDR", this.gpuADDR);
		consoleTag.setString("screenADDR", this.screenADDR);
		consoleTag.setBoolean("cursor", this.cursor);
		consoleTag.setInteger("X", this.X);
		consoleTag.setInteger("Y", this.Y);
		consoleTag.setInteger("W", this.W);
		consoleTag.setInteger("H", this.H);
		int[] fifo = new int[this.fifo.size()];
		int i = 0;
		for (int v : this.fifo)
			fifo[i++] = v;
		consoleTag.setIntArray("fifo", fifo);
		int[] databuf = new int[this.fifo.size()];
		i = 0;
		for (int v : this.databuf)
			databuf[i++] = v;
		consoleTag.setIntArray("databuf", databuf);
		nbt.setTag("console", consoleTag);
	}

	private void scroll() {
		// Scroll the screen upwards.
		databuf.add(1, -8);
		databuf.add(1, -7);
	}

	public void write(int character) {
		if (canWrite) {
			databuf.add(character);
		}
	}

	// TODO: Combine multiple characters in one "set"
	public void flush() {
		if (canWrite) {
			try {
				if (System.currentTimeMillis() - this.lastTime >= 500 && !parseANSI && !ansiDetect) {
					lastTime = System.currentTimeMillis();
					databuf.addFirst(-1004);
					databuf.addFirst(-1003);
					databuf.addFirst(-1002);
					databuf.addFirst(-1001);
				}
				while (!databuf.isEmpty()) {
					int character = databuf.getFirst();
					if (parseANSI) {
						if ((character >= 65 && character <= 90) || (character >= 97 && character <= 122)) {
							// End of sequence
							parseANSI = false;
							String ansiCode = this.ansiCode.toString();
							String[] ansiParts = this.ansiCode.toString().split(";");
							switch (character) {
							case 'A':
								if (ansiCode.length() > 0)
									Y = Math.max(Y - Integer.parseInt(ansiCode), 1);
								else
									Y = Math.max(Y - 1, 1);
								break;
							case 'B':
								if (ansiCode.length() > 0)
									Y = Math.min(Y + Integer.parseInt(ansiCode), H);
								else
									Y = Math.min(Y + 1, 1);
								break;
							case 'C':
								if (ansiCode.length() > 0)
									X = Math.min(X + Integer.parseInt(ansiCode), W);
								else
									X = Math.min(X + 1, 1);
								break;
							case 'D':
								if (ansiCode.length() > 0)
									X = Math.max(X - Integer.parseInt(ansiCode), 1);
								else
									X = Math.max(X - 1, 1);
								break;
							case 'E':
								if (ansiCode.length() > 0)
									Y = Math.min(Y + Integer.parseInt(ansiCode), H);
								else
									Y = Math.min(Y + 1, 1);
								X = 1;
								break;
							case 'F':
								if (ansiCode.length() > 0)
									Y = Math.max(Y - Integer.parseInt(ansiCode), 1);
								else
									Y = Math.max(Y - 1, 1);
								X = 1;
								break;
							case 'G':
								if (ansiCode.length() > 0)
									X = Math.min(Integer.parseInt(ansiCode), W);
								else
									X = 1;
								break;
							case 'H':
								if (ansiCode.length() == 0) {
									X = 1;
									Y = 1;
								} else if (ansiParts.length == 2) {
									X = Math.max(Math.min(Integer.parseInt(ansiParts[0]), W), 1);
									Y = Math.max(Math.min(Integer.parseInt(ansiParts[1]), H), 1);
								}
								break;
							case 'J':
								if (ansiCode.length() == 0 || ansiCode.equals("0")) {
									if (this.Y < this.H)
										databuf.add(1,-500);
									databuf.add(1,-502);
								} else if (ansiCode.equals("1")) {
									if (this.Y > 1)
										databuf.add(1,-501);
									databuf.add(1,-503);
								} else if (ansiCode.equals("2"))
									databuf.add(1, -6);
								break;
							case 'K':
								if (ansiCode.length() == 0 || ansiCode.equals("0"))
									databuf.add(1,-502);
								else if (ansiCode.equals("1"))
									databuf.add(1,-503);
								else if (ansiCode.equals("2"))
									databuf.add(1,-504);
								break;
							case 'L':
								if (ansiCode.length() > 0)
									; // TODO: Insert ????? lines
								else
									; // TODO: Insert one line
								break;
							case 'M':
								if (ansiCode.length() > 0)
									; // TODO: Delete ????? lines
								else
									; // TODO: Delete one line
								break;
							case 'P':
								if (ansiCode.length() > 0)
									; // TODO: Shift characters past cursor left ????? (Delete)
								else
									; // TODO: Shift characters past cursor left 1 (Delete)
								break;
							case 'S':
								if (ansiCode.length() > 0)
									; // TODO: Scroll display up ????? lines
								else
									; // TODO: Scroll display up 1 line
								break;
							case 'T':
								if (ansiCode.length() > 0)
									; // TODO: Scroll display down ????? lines
								else
									; // TODO: Scroll display down 1 line
								break;
							case 'X':
								if (ansiCode.length() > 0)
									; // TODO: Set ????? characters including cursor to space (Erase)
								else
									; // TODO: Set cursor to space
								break;
							case 'm':
								// TODO: All of these (0,1,5,30-37,40-47)
								break;
							}
						} else
							ansiCode.append((char) character);
					} else {
						if (ansiDetect) {
							if (character == 91) {
								parseANSI = true;
								character = -1000;
								ansiCode.setLength(0);
							} else {
								databuf.addFirst(-27);
								character = -27;
							}
							ansiDetect = false;
						}
						if ((character < -1004 || character > -1001) && cursor) {
							databuf.addFirst(-1004);
							databuf.addFirst(-1003);
							databuf.addFirst(-1002);
							databuf.addFirst(-1001);
							character = -1001;
						}
						switch (character) {
						// Special cases, characters are not negative
						case -1000:
							// Generic NOP
							break;
						case -1:
							machine.invoke(gpuADDR, "bind", new Object[] { screenADDR });
							break;
						case -2:
							Object[] response = machine.invoke(gpuADDR, "getResolution", new Object[] {});
							this.W = (Integer) response[0];
							this.H = (Integer) response[1];
							break;
						case -3:
							machine.invoke(gpuADDR, "setResolution", new Object[] { (double) this.W, (double) this.H });
							break;
						case -4:
							machine.invoke(gpuADDR, "setBackground", new Object[] { (double) 0x000000 });
							break;
						case -5:
							machine.invoke(gpuADDR, "setForeground", new Object[] { (double) 0xFFFFFF });
							break;
						case -6:
							machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) 1, (double) this.W, (double) this.H, " " });
							break;
						case -7:
							machine.invoke(gpuADDR, "copy", new Object[] { (double) 1, (double) 1, (double) this.W, (double) this.H, (double) 0, (double) -1 });
							break;
						case -8:
							machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) this.H, (double) this.W, (double) 1, " " });
							this.Y = this.H;
							break;
						case -500: // ANSI 0J
							machine.invoke(gpuADDR, "fill", new Object[] { (double) this.X, (double) this.Y + 1, (double) this.W, (double) (this.H - this.Y), " " });
							break;
						case -501: // ANSI 1J
							machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) 1, (double) this.W, (double) this.Y - 1, " " });
							break;
						case -502: // ANSI 0K
							machine.invoke(gpuADDR, "fill", new Object[] { (double) this.X, (double) this.Y, (double) (this.W - this.X + 1), (double) 1, " " });
							break;
						case -503: // ANSI 1K
							machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) this.Y, (double) this.X, (double) 1, " " });
							break;
						case -504: // ANSI 2K
							machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) this.Y, (double) this.W, (double) 1, " " });
							break;
						case -1001: // Cursor GET
							Object[] response2 = machine.invoke(gpuADDR, "get", new Object[] {this.X, this.Y});
							this.cursorChar = (Character) response2[0];
							this.cursorFG = (Integer) response2[2];
							this.cursorBG = (Integer) response2[1];
							break;
						case -1002: // Cursor Set BG
							machine.invoke(gpuADDR, "setBackground", new Object[] { (double) this.cursorBG });
							break;
						case -1003: // Cursor Set FG
							machine.invoke(gpuADDR, "setForeground", new Object[] { (double) this.cursorFG });
							break;
						case -1004: // Cursor Set Character
							machine.invoke(gpuADDR, "set", new Object[] { (double) this.X, (double) this.Y, Character.toString(this.cursorChar) });
							this.cursor = !this.cursor;
							break;
						case 7:
							PacketSender.sendSound(machine.host().world(), machine.host().xPosition(), machine.host().yPosition(), machine.host().zPosition(), "-");
							break;
						case 8:
							int dX = this.X - 1;
							int dY = this.Y;
							if (dX <= 0) {
								dX = this.W;
								if (dY > 1)
									dY = dY - 1;
								else {
									dX = 1;
								}
							}
							machine.invoke(gpuADDR, "set", new Object[] { (double) dX, (double) dY, " " });
							this.X = dX;
							this.Y = dY;
							break;
						case 10:
							this.X = 1;
							this.Y = this.Y + 1;
							if (this.Y > this.H)
								scroll();
							break;
						case 13:
							break;
						case 27:
							ansiDetect = true;
							break;
						case 155:
							parseANSI = true;
							ansiCode.setLength(0);
							break;
						case -27:
							character = 27;
						default:
							machine.invoke(gpuADDR, "set", new Object[] { (double) this.X, (double) this.Y, Character.toString((char) character) });
							this.X = this.X + 1;
							if (this.X > this.W) {
								this.Y = this.Y + 1;
								this.X = 1;
								if (this.Y > this.H)
									scroll();
							}
						}
					}
					databuf.removeFirst();
				}
			} catch (LimitReachedException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void pushChar(int character) {
		fifo.add(character);
	}

	public boolean hasInput() {
		return !fifo.isEmpty();
	}

	public int read() {
		return fifo.removeFirst();
	}
}
