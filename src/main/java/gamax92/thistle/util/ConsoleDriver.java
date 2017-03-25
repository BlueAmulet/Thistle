package gamax92.thistle.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.PackedColor.MutablePaletteFormat;
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

	private LinkedList<Integer> databuf = new LinkedList<Integer>();

	private int X = 1;
	private int Y = 1;
	private int W;
	private int H;

	private boolean parseANSI = false;
	private boolean ansiDetect = false;
	private StringBuffer ansiCode = new StringBuffer();

	private boolean brightFG = false;
	private boolean brightBG = false;
	private int textFG = 7;
	private int textBG = 0;

	private boolean showCursor = true;
	private boolean cursor = false;
	private long lastTime = System.currentTimeMillis();
	private int cursorFG;
	private int cursorBG;
	private char cursorChar;

	private double[] colors = {
		0x000000,0xFF3333,0x336600,0x663300,0x333399,0x9933CC,0x336699,0xCCCCCC,
		0x333333,0xFF6699,0x33CC33,0xFFFF33,0x6699FF,0xCC66CC,0xFFCC33,0xFFFFFF
	};

	public ConsoleDriver(Machine machine) {
		MutablePaletteFormat test = new MutablePaletteFormat();
		int[] palette = test.palette();
		for (int i=0; i<16; i++) {
			colors[i] = palette[15-i];
		}

		this.machine = machine;
		if (!machine.isRunning()) {
			Map<String, String> components = this.machine.components();
			Iterator<Map.Entry<String, String>> entries = components.entrySet().iterator();

			while (entries.hasNext()) {
				Map.Entry<String, String> entry = entries.next();
				String address = entry.getKey();
				String type = entry.getValue();
				if (type.equals("gpu") && gpuADDR == null)
					gpuADDR = address;
				if (type.equals("screen") && screenADDR == null)
					screenADDR = address;
			}
			if (gpuADDR != null && screenADDR != null) {
				canWrite = true;
				// Attempt to setup the GPU
				for (int i = -1; i >= -6; i--)
					databuf.add(i);
			}
		}
	}

	private String getString(NBTTagCompound nbt, String key) {
		if (nbt.hasKey(key))
			return nbt.getString(key);
		else
			return null;
	}

	private void setString(NBTTagCompound nbt, String key, String value) {
		if (value != null)
			nbt.setString(key, value);
	}

	// TODO: Needs more things
	public void load(NBTTagCompound nbt) {
		// Restore Console
		if (nbt.hasKey("console")) {
			NBTTagCompound consoleTag = nbt.getCompoundTag("console");
			this.canRead = consoleTag.getBoolean("canRead");
			this.canWrite = consoleTag.getBoolean("canWrite");
			this.gpuADDR = getString(consoleTag, "gpuADDR");
			this.screenADDR = getString(consoleTag, "screenADDR");
			this.cursor = consoleTag.getBoolean("cursor");
			this.X = consoleTag.getInteger("X");
			this.Y = consoleTag.getInteger("Y");
			this.W = consoleTag.getInteger("W");
			this.H = consoleTag.getInteger("H");
			this.brightFG = consoleTag.getBoolean("brightFG");
			this.brightBG = consoleTag.getBoolean("brightBG");
			this.textFG = consoleTag.getInteger("textFG");
			this.textBG = consoleTag.getInteger("textBG");
			this.ansiDetect = consoleTag.getBoolean("ansiDetect");
			this.parseANSI = consoleTag.getBoolean("parseANSI");
			this.ansiCode.setLength(0);
			this.ansiCode.append(consoleTag.getString("ansiCode"));
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
		setString(consoleTag, "gpuADDR", this.gpuADDR);
		setString(consoleTag, "screenADDR", this.screenADDR);
		consoleTag.setBoolean("cursor", this.cursor);
		consoleTag.setInteger("X", this.X);
		consoleTag.setInteger("Y", this.Y);
		consoleTag.setInteger("W", this.W);
		consoleTag.setInteger("H", this.H);
		consoleTag.setBoolean("brightFG", this.brightFG);
		consoleTag.setBoolean("brightBG", this.brightBG);
		consoleTag.setInteger("textFG", this.textFG);
		consoleTag.setInteger("textBG", this.textBG);
		consoleTag.setBoolean("ansiDetect", this.ansiDetect);
		consoleTag.setBoolean("parseANSI", this.parseANSI);
		consoleTag.setString("ansiCode", this.ansiCode.toString());
		int[] databuf = new int[this.databuf.size()];
		int i = 0;
		for (int v : this.databuf)
			databuf[i++] = v;
		consoleTag.setIntArray("data", databuf);
		nbt.setTag("console", consoleTag);
	}

	private void scroll() {
		// Scroll the screen upwards.
		databuf.add(1, -8);
		databuf.add(1, -7);
	}

	public void write(int character) {
		if (canWrite)
			databuf.add(character);
	}

	private int parseCode(String ansiCode) {
		return ansiCode.length() > 0 ? Integer.parseInt(ansiCode) : 1;
	}

	private int clampW(int X) {
		return Math.max(Math.min(X, W), 1);
	}

	private int clampH(int Y) {
		return Math.max(Math.min(Y, H), 1);
	}

	// TODO: Combine multiple characters in one "set"
	public void flush() throws Exception {
		if (canWrite) {
			try {
				if (showCursor && System.currentTimeMillis() - this.lastTime >= 500 && !parseANSI && !ansiDetect) {
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
							String[] ansiParts = this.ansiCode.toString().split(";", -1);
							try {
								switch (character) {
								case 'A':
									Y = Math.max(Y - parseCode(ansiCode), 1);
									break;
								case 'B':
									Y = Math.min(Y + parseCode(ansiCode), H);
									break;
								case 'C':
									X = Math.min(X + parseCode(ansiCode), W);
									break;
								case 'D':
									X = Math.max(X - parseCode(ansiCode), 1);
									break;
								case 'E':
									Y = Math.min(Y + parseCode(ansiCode), H);
									X = 1;
									break;
								case 'F':
									Y = Math.max(Y - parseCode(ansiCode), 1);
									X = 1;
									break;
								case 'G':
									X = clampW(parseCode(ansiCode));
									break;
								case 'H':
									if (ansiParts.length > 2) {
										break;
									}
									X = 1;
									Y = 1;
									if (ansiParts.length >= 1)
										Y = clampH(Integer.parseInt(ansiParts[0]));
									if (ansiParts.length == 2)
										X = clampW(Integer.parseInt(ansiParts[1]));
									break;
								case 'J':
									if (ansiCode.length() == 0 || ansiCode.equals("0")) {
										if (this.Y < this.H)
											databuf.add(1, -500);
										databuf.add(1, -502);
									} else if (ansiCode.equals("1")) {
										if (this.Y > 1)
											databuf.add(1, -501);
										databuf.add(1, -503);
									} else if (ansiCode.equals("2"))
										databuf.add(1, -6);
									break;
								case 'K':
									if (ansiCode.length() == 0 || ansiCode.equals("0"))
										databuf.add(1, -502);
									else if (ansiCode.equals("1"))
										databuf.add(1, -503);
									else if (ansiCode.equals("2"))
										databuf.add(1, -504);
									break;
								case 'L':
									// TODO: Insert n lines
									break;
								case 'M':
									// TODO: Delete n lines
									break;
								case 'P':
									// TODO: Shift characters past cursor left n (Delete)
									break;
								case 'S':
									// TODO: Scroll display up n lines
									break;
								case 'T':
									// TODO: Scroll display down n lines
									break;
								case 'X':
									// TODO: Set n characters including cursor to space (Erase)
									break;
								case 'h':
									if (ansiCode.equals("?25"))
										showCursor = true;
									break;
								case 'l':
									if (ansiCode.equals("?25")) {
										showCursor = false;
										if (cursor) {
											databuf.add(1, -1004);
											databuf.add(1, -1003);
											databuf.add(1, -1002);
											databuf.add(1, -1001);
										}
									}
									break;
								case 'm':
									boolean newBrightFG = this.brightFG;
									boolean newBrightBG = this.brightBG;
									int newTextFG = this.textFG;
									int newTextBG = this.textBG;
									for (String part : ansiParts) {
										int ipart = Integer.parseInt(part);
										if (ipart == 0) {
											newBrightFG = false;
											newBrightBG = false;
											newTextFG = 7;
											newTextBG = 0;
										} else if (ipart == 1)
											newBrightFG = true;
										else if (ipart == 5)
											newBrightBG = true;
										else if (ipart >= 30 && ipart <= 37)
											newTextFG = ipart - 30;
										else if (ipart >= 40 && ipart <= 47)
											newTextBG = ipart - 40;
									}
									if (newBrightBG != this.brightBG || newTextBG != this.textBG) {
										this.brightBG = newBrightBG;
										this.textBG = newTextBG;
										databuf.add(1, -4);
									}
									if (newBrightFG != this.brightFG || newTextFG != this.textFG) {
										this.brightFG = newBrightFG;
										this.textFG = newTextFG;
										databuf.add(1, -5);
									}
									break;
								default:
									for (int i = ansiCode.length()-1; i >= 0; i--) {
										databuf.addFirst((int) ansiCode.charAt(i));
									}
									databuf.addFirst(91);
									databuf.addFirst(-27);
									databuf.addFirst(-1000);
								}
							} catch (NumberFormatException e) {
								for (int i = ansiCode.length()-1; i >= 0; i--) {
									databuf.addFirst((int) ansiCode.charAt(i));
								}
								databuf.addFirst(91);
								databuf.addFirst(-27);
								databuf.addFirst(-1000);
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
							machine.invoke(gpuADDR, "setResolution", new Object[] { this.W, this.H });
							break;
						case -4:
							machine.invoke(gpuADDR, "setBackground", new Object[] { colors[(this.brightBG ? 8 : 0) + this.textBG] });
							break;
						case -5:
							machine.invoke(gpuADDR, "setForeground", new Object[] { colors[(this.brightFG ? 8 : 0) + this.textFG] });
							break;
						case -6:
							machine.invoke(gpuADDR, "fill", new Object[] { 1, 1, this.W, this.H, " " });
							break;
						case -7:
							machine.invoke(gpuADDR, "copy", new Object[] { 1, 1, this.W, this.H, 0, -1 });
							break;
						case -8:
							machine.invoke(gpuADDR, "fill", new Object[] { 1, this.H, this.W, 1, " " });
							this.Y = this.H;
							break;
						case -500: // ANSI 0J
							machine.invoke(gpuADDR, "fill", new Object[] { this.X, this.Y + 1, this.W, (this.H - this.Y), " " });
							break;
						case -501: // ANSI 1J
							machine.invoke(gpuADDR, "fill", new Object[] { 1, 1, this.W, this.Y - 1, " " });
							break;
						case -502: // ANSI 0K
							machine.invoke(gpuADDR, "fill", new Object[] { this.X, this.Y, (this.W - this.X + 1), 1, " " });
							break;
						case -503: // ANSI 1K
							machine.invoke(gpuADDR, "fill", new Object[] { 1, this.Y, this.X, 1, " " });
							break;
						case -504: // ANSI 2K
							machine.invoke(gpuADDR, "fill", new Object[] { 1, this.Y, this.W, 1, " " });
							break;
						case -1001: // Cursor GET
							Object[] response2 = machine.invoke(gpuADDR, "get", new Object[] { this.X, this.Y });
							this.cursorChar = (Character) response2[0];
							this.cursorFG = (Integer) response2[2];
							this.cursorBG = (Integer) response2[1];
							break;
						case -1002: // Cursor Set BG
							machine.invoke(gpuADDR, "setBackground", new Object[] { this.cursorBG });
							break;
						case -1003: // Cursor Set FG
							machine.invoke(gpuADDR, "setForeground", new Object[] { this.cursorFG });
							break;
						case -1004: // Cursor Set Character
							machine.invoke(gpuADDR, "set", new Object[] { this.X, this.Y, Character.toString(this.cursorChar) });
							this.cursor = !this.cursor;
							if (!this.cursor) {
								if (this.cursorBG != colors[(this.brightBG ? 8 : 0) + this.textBG])
									databuf.add(1, -4);
								if (this.cursorFG != colors[(this.brightFG ? 8 : 0) + this.textFG])
									databuf.add(1, -5);
							}
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
								else
									dX = 1;
							}
							machine.invoke(gpuADDR, "set", new Object[] { dX, dY, " " });
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
							machine.invoke(gpuADDR, "set", new Object[] { this.X, this.Y, Character.toString((char) character) });
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
				// The rest of the data will be written during the next flush
			}
		}
	}
}
