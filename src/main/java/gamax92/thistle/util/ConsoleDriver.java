package gamax92.thistle.util;

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

	private LinkedList<Integer> databuf = new LinkedList<Integer>();

	private int X = 1;
	private int Y = 1;
	private int W;
	private int H;

	// Parser Variables {
	private boolean parseANSI = false;
	private boolean ansiDetect = false;
	private StringBuffer ansiCode = new StringBuffer();
	// }

	// Command Runtime Variables {
	private boolean brightFG = false;
	private boolean brightBG = false;
	private int textFG = 7;
	private int textBG = 0;
	// This is the cursor blink flag. If it's true, the cursor is showing right now.
	private boolean cursor = false;
	private boolean showCursor = true;
	// }

	// This actually means 'is setup still *ongoing* right now'.
	private boolean setup = true;
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
			updateComponents();
		}
	}

	private void updateComponents() {
		if (gpuADDR != null && this.machine.node().network().node(gpuADDR) == null)
			gpuADDR = null;
		if (screenADDR != null && this.machine.node().network().node(screenADDR) == null)
			screenADDR = null;
		if (gpuADDR == null || screenADDR == null ) {
			canWrite = false;
			cursor = false;
			for (Map.Entry<String, String> entry : this.machine.components().entrySet()) {
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
				for (int i = -6; i <= -1; i++)
					databuf.addFirst(i);
				setup = true;
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
			this.canWrite = consoleTag.getBoolean("canWrite");
			this.gpuADDR = getString(consoleTag, "gpuADDR");
			this.screenADDR = getString(consoleTag, "screenADDR");
			this.cursor = consoleTag.getBoolean("cursor");
			this.showCursor = consoleTag.getBoolean("showCursor");
			this.setup = consoleTag.getBoolean("setup");
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
		consoleTag.setBoolean("canWrite", this.canWrite);
		setString(consoleTag, "gpuADDR", this.gpuADDR);
		setString(consoleTag, "screenADDR", this.screenADDR);
		consoleTag.setBoolean("cursor", this.cursor);
		consoleTag.setBoolean("showCursor", this.showCursor);
		consoleTag.setBoolean("setup", this.setup);
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

	public void write(int character) {
		updateComponents();
		if (canWrite) {
			if (!parseANSI) {
				// Not in ANSI mode, normal processing
				if (ansiDetect) {
					ansiDetect = false;
					if (character == 91) {
						// This character is the second half of an ANSI escape. Inhibit & turn on ANSI processing.
						parseANSI = true;
						ansiCode.setLength(0);
						return;
					} else {
						// It wasn't an ANSI escape at all, forward the inhibited 27 character and process this character normally.
						databuf.add(27);
					}
				}
				// Special characters
				if (character == 27) {
					// First half of ANSI escape, inhibit until completed.
					ansiDetect = true;
					return;
				} else if (character == 155) {
					// Immediate ANSI escape ; inhibit now!
					parseANSI = true;
					ansiCode.setLength(0);
					return;
				}
				databuf.add(character);
			} else if ((character >= 65 && character <= 90) || (character >= 97 && character <= 122)) {
				// End of sequence
				parseANSI = false;
				String ansiCode = this.ansiCode.toString();
				String[] ansiParts = this.ansiCode.toString().split(";", -1);
				try {
					switch (character) {
					case 'A':
						databuf.add(-511);
						databuf.add(-parseCode(ansiCode));
						break;
					case 'B':
						databuf.add(-511);
						databuf.add(parseCode(ansiCode));
						break;
					case 'C':
						databuf.add(-510);
						databuf.add(-parseCode(ansiCode));
						break;
					case 'D':
						databuf.add(-510);
						databuf.add(parseCode(ansiCode));
						break;
					case 'E':
						databuf.add(-520);
						databuf.add(1);

						databuf.add(-511);
						databuf.add(parseCode(ansiCode));
						break;
					case 'F':
						databuf.add(-520);
						databuf.add(1);

						databuf.add(-511);
						databuf.add(-parseCode(ansiCode));
						break;
					case 'G':
						databuf.add(-520);
						databuf.add(parseCode(ansiCode));
						break;
					case 'H':
						if (ansiParts.length > 2) {
							break;
						}
						{
							int targetX = 1;
							int targetY = 1;
							if (ansiParts.length >= 1)
								targetY = clampH(Integer.parseInt(ansiParts[0]));
							if (ansiParts.length == 2)
								targetX = clampW(Integer.parseInt(ansiParts[1]));

							databuf.add(-520);
							databuf.add(targetX);

							databuf.add(-521);
							databuf.add(targetY);
						}
						break;
					case 'J':
						if (ansiCode.length() == 0 || ansiCode.equals("0")) {
							databuf.add(-500);
						} else if (ansiCode.equals("1")) {
							databuf.add(-501);
						} else if (ansiCode.equals("2"))
							databuf.add(-6);
						break;
					case 'K':
						if (ansiCode.length() == 0 || ansiCode.equals("0"))
							databuf.add(-502);
						else if (ansiCode.equals("1"))
							databuf.add(-503);
						else if (ansiCode.equals("2"))
							databuf.add(-504);
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
							databuf.add(-505);
						break;
					case 'l':
						if (ansiCode.equals("?25"))
							databuf.add(-506);
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
						if (newBrightFG != this.brightFG || newTextFG != this.textFG) {
							this.brightFG = newBrightFG;
							this.textFG = newTextFG;
							databuf.add(-5);
						}
						if (newBrightBG != this.brightBG || newTextBG != this.textBG) {
							this.brightBG = newBrightBG;
							this.textBG = newTextBG;
							databuf.add(-4);
						}
						break;
					default:
						databuf.add(27);
						databuf.add(91);
						for (int i = 0; i < ansiCode.length(); i++)
							databuf.add((int) ansiCode.charAt(i));
					}
				} catch (NumberFormatException e) {
					databuf.add(27);
					databuf.add(91);
					for (int i = 0; i < ansiCode.length(); i++)
						databuf.add((int) ansiCode.charAt(i));
				}
			} else {
				// It shouldn't be possible to have a valid ANSI code above 256 characters,
				//  even if you account for the window title extensions and etc.,
				//  so it's probably either someone trying to eat all server memory,
				//  or an accident
				if (ansiCode.length() < 256)
					ansiCode.append((char) character);
			}
		}
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
		updateComponents();
		if (canWrite) {
			try {
				if (showCursor && ((System.currentTimeMillis() - this.lastTime) >= 500) && !setup && (X >= 1 && X <= W && Y >= 1 && Y <= H)) {
					lastTime = System.currentTimeMillis();
					databuf.addFirst(-1001);
				}
				while (!databuf.isEmpty()) {
					// This is the command ID.
					int character = databuf.getFirst();
					// This is the length of the command (used after it has finished)
					int cmdLen = 1;
					// Commands that should be executed immediately after must be added here.
					LinkedList<Integer> chained = new LinkedList<Integer>();

					if ((character < -1004 || character > -1000) && cursor) {
						// When performing any operation that isn't one of the 4 cursor toggle operations, temporarily hide the cursor.
						// This implies that cursor control operations start with the cursor already hidden.
						// This is written in such a way as to cause -1001 to be executed, followed by the rest of that command, before the original command.
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
						setup = false;
						break;

					case -400: // Scroll Left 1. Move 
						machine.invoke(gpuADDR, "copy", new Object[] { 1, 1, this.W, this.H, 1, 0 });
						chained.add(-401);
						break;
					case -401: // Scroll Left 2. Fill
						machine.invoke(gpuADDR, "fill", new Object[] { 1, 1, 1, this.H, " " });
						break;
					case -410: // Scroll Right 1. Move 
						machine.invoke(gpuADDR, "copy", new Object[] { 1, 1, this.W, this.H, -1, 0 });
						chained.add(-411);
						break;
					case -411: // Scroll Right 2. Fill
						machine.invoke(gpuADDR, "fill", new Object[] { this.W, 1, 1, this.H, " " });
						break;
					case -420: // Scroll Up 1. Move 
						machine.invoke(gpuADDR, "copy", new Object[] { 1, 1, this.W, this.H, 0, 1 });
						chained.add(-421);
						break;
					case -421: // Scroll Up 2. Fill
						machine.invoke(gpuADDR, "fill", new Object[] { 1, 1, this.W, 1, " " });
						break;
					case -430: // Scroll Down 1. Move 
						machine.invoke(gpuADDR, "copy", new Object[] { 1, 1, this.W, this.H, 0, -1 });
						chained.add(-431);
						break;
					case -431: // Scroll Down 2. Fill
						machine.invoke(gpuADDR, "fill", new Object[] { 1, this.H, this.W, 1, " " });
						break;

					case -500: // ANSI 0J
						if (this.Y < this.H)
							machine.invoke(gpuADDR, "fill", new Object[] { this.X, this.Y + 1, this.W, (this.H - this.Y), " " });
						chained.add(-502);
						break;
					case -501: // ANSI 1J
						if (this.Y > 1)
							machine.invoke(gpuADDR, "fill", new Object[] { 1, 1, this.W, this.Y - 1, " " });
						chained.add(-503);
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
					case -505: // ANSI ?25h (Cursor Enable)
						showCursor = true;
						break;
					case -506: // ANSI ?25l (Cursor Disable)
						showCursor = false;
						// The cursor is already implicitly hidden (see the cursor automatic disabler)
						break;

					case -510: // Offset Cursor X
						{
							int param = databuf.get(cmdLen);
							cmdLen++;
							X = clampW(X + param);
						}
						break;
					case -511: // Offset Cursor Y
						{
							int param = databuf.get(cmdLen);
							cmdLen++;
							Y = clampH(Y + param);
						}
						break;
					case -520: // Set Cursor X
						{
							int param = databuf.get(cmdLen);
							cmdLen++;
							X = clampW(param);
						}
						break;
					case -521: // Set Cursor Y
						{
							int param = databuf.get(cmdLen);
							cmdLen++;
							Y = clampH(param);
						}
						break;

					case -1001: // Cursor 1. GET
						Object[] response2 = machine.invoke(gpuADDR, "get", new Object[] { this.X, this.Y });
						this.cursorChar = (Character) response2[0];
						this.cursorFG = (Integer) response2[2];
						this.cursorBG = (Integer) response2[1];
						chained.add(-1002);
						break;
					case -1002: // Cursor 2. Set BG
						machine.invoke(gpuADDR, "setBackground", new Object[] { this.cursorBG });
						chained.add(-1003);
						break;
					case -1003: // Cursor 3. Set FG
						machine.invoke(gpuADDR, "setForeground", new Object[] { this.cursorFG });
						chained.add(-1004);
						break;
					case -1004: // Cursor 4. Set Character
						machine.invoke(gpuADDR, "set", new Object[] { this.X, this.Y, Character.toString(this.cursorChar) });
						this.cursor = !this.cursor;
						if (!this.cursor) {
							if (this.cursorBG != colors[(this.brightBG ? 8 : 0) + this.textBG])
								chained.add(-4);
							if (this.cursorFG != colors[(this.brightFG ? 8 : 0) + this.textFG])
								chained.add(-5);
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
						this.Y = this.Y + 1;
						if (this.Y > this.H) {
							this.Y = this.H;
							chained.add(-430);
						}
						break;
					case 13:
						this.X = 1;
						break;
					default:
						machine.invoke(gpuADDR, "set", new Object[] { this.X, this.Y, Character.toString((char) character) });
						this.X = this.X + 1;
						if (this.X > this.W) {
							this.Y = this.Y + 1;
							this.X = 1;
							if (this.Y > this.H) {
								this.Y = this.H;
								chained.add(-430);
							}
						}
					}
					// The command executed successfully, clean up:

					for (int i = 0; i < cmdLen; i++)
						databuf.removeFirst();

					int chainPos = 0;
					for (Integer i : chained) {
						databuf.add(chainPos, i);
						chainPos++;
					}
				}
			} catch (LimitReachedException e) {
				// The rest of the data will be written during the next flush
			}
		}
	}
}
