package gamax92.ocsymon;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;

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

	public ConsoleDriver(Machine machine) {
		this.machine = machine;
		Map<String, String> components = this.machine.components();
		Iterator<Map.Entry<String, String>> entries = components.entrySet().iterator();

		Object[] response;

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
				// Attempt to setup the GPU
				machine.invoke(gpuADDR, "bind", new Object[] { screenADDR });
				response = machine.invoke(gpuADDR, "getResolution", new Object[] {});
				this.W = (Integer) response[0];
				this.H = (Integer) response[1];
				machine.invoke(gpuADDR, "setResolution", new Object[] { (double) this.W, (double) this.H });
				machine.invoke(gpuADDR, "setBackground", new Object[] { (double) 0x000000 });
				machine.invoke(gpuADDR, "setForeground", new Object[] { (double) 0xFFFFFF });
				machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) 1, (double) this.W, (double) this.H, " " });
				canWrite = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void scroll() {
		// Try to scroll the screen upwards.
		try {
			machine.invoke(gpuADDR, "copy", new Object[] { (double) 1, (double) 1, (double) this.W, (double) this.H, (double) 0, (double) -1 });
			machine.invoke(gpuADDR, "fill", new Object[] { (double) 1, (double) this.H, (double) this.W, (double) 1, " " });
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.Y = this.H;
	}

	// TODO: needs ANSI escape codes.
	public void write(int character) {
		if (canWrite) {
			databuf.add(character);
		}
	}

	public void flush() {
		if (canWrite) {
			try {
				while (!databuf.isEmpty()) {
					int character = databuf.getFirst();
					switch (character) {
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
					databuf.removeFirst();
				}
			} catch (LimitReachedException e) {} catch (Exception e) {
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
