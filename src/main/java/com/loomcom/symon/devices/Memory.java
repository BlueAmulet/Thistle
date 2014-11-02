/*
 * Copyright (c) 2014 Seth J. Morabito <web@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon.devices;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class Memory extends Device {

	private final static Logger logger = Logger.getLogger(Memory.class.getName());

	private boolean readOnly;
	private int[] mem;

	/* Initialize all locations to 0x00 (BRK) */
	private static final int DEFAULT_FILL = 0x00;

	public Memory(int startAddress, int endAddress, boolean readOnly) throws MemoryRangeException {
		super(startAddress, endAddress, (readOnly ? "RO Memory" : "RW Memory"));
		this.readOnly = readOnly;
		this.mem = new int[this.size];
		this.fill(DEFAULT_FILL);
	}

	public Memory(int startAddress, int endAddress) throws MemoryRangeException {
		this(startAddress, endAddress, false);
	}

	public static Memory makeROM(int startAddress, int endAddress, InputStream is) throws MemoryRangeException, IOException {
		Memory memory = new Memory(startAddress, endAddress, true);
		memory.loadFromStream(is);
		return memory;
	}

	public static Memory makeRAM(int startAddress, int endAddress) throws MemoryRangeException {
		Memory memory = new Memory(startAddress, endAddress, false);
		return memory;
	}

	public void write(int address, int data) throws MemoryAccessException {
		if (readOnly) {
			throw new MemoryAccessException("Cannot write to read-only memory at address " + address);
		} else {
			this.mem[address] = data;
		}
	}

	/**
	 * Load the memory from a file.
	 *
	 * @param file The file to read an array of bytes from.
	 * @throws MemoryRangeException if the file and memory size do not match.
	 * @throws IOException if the file read fails.
	 */
	public void loadFromStream(InputStream is) throws MemoryRangeException, IOException {
		for (int i = 0; i <= mem.length; i++) {
			int data = is.read();
			if (data == -1)
				break;
			mem[i] = data;
		}
		if (is.read() != -1)
			logger.log(Level.WARNING, "Truncating boot.rom to " + mem.length);
	}

	public int read(int address) throws MemoryAccessException {
		return this.mem[address];
	}

	public void fill(int val) {
		Arrays.fill(this.mem, val);
	}

	public String toString() {
		return "Memory: " + getMemoryRange().toString();
	}

	public int[] getDmaAccess() {
		return mem;
	}
}
