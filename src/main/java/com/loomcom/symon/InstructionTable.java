/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
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

package com.loomcom.symon;

public interface InstructionTable {

	/**
	 * Enumeration of Addressing Modes.
	 */
	enum Mode {
		ACC("Accumulator", 1),
		IMM("Immediate", 2),
		ZPG("Zeropage", 2),
		ZPX("Zeropage, X-indexed", 2),
		ZPY("Zeropage, Y-indexed", 2),
		ABS("Absolute", 3),
		ABX("Absolute, X-indexed", 3),
		ABY("Absolute, Y-indexed", 3),
		IMP("Implied", 1),
		REL("Relative", 2),
		XIN("X-indexed Indirect", 2),
		INY("Indirect, Y-indexed", 2),
		IND("Absolute Indirect", 3),
		IAX("Absolute Indirect, X-indexed", 3),
		IZP("Zeropage Indirect", 2),
		ZPR("Zeropage Relative", 3);

		private final String text;
		private final int length;

		private Mode(final String text, final int length) {
			this.text = text;
			this.length = length;
		}

		public int getLength() {
			return length;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	// 65C02 opcodes.

	/**
	 * Instruction opcode names.
	 */
	String[] opcodeNames = {
		"BRK", "ORA", "NOP", "NOP", "TSB", "ORA", "ASL", "RMB",
		"PHP", "ORA", "ASL", "NOP", "TSB", "ORA", "ASL", "BBR",
		"BPL", "ORA", "ORA", "NOP", "TRB", "ORA", "ASL", "RMB",
		"CLC", "ORA", "INC", "NOP", "TRB", "ORA", "ASL", "BBR",
		"JSR", "AND", "NOP", "NOP", "BIT", "AND", "ROL", "RMB",
		"PLP", "AND", "ROL", "NOP", "BIT", "AND", "ROL", "BBR",
		"BMI", "AND", "AND", "NOP", "BIT", "AND", "ROL", "RMB",
		"SEC", "AND", "DEC", "NOP", "BIT", "AND", "ROL", "BBR",
		"RTI", "EOR", "NOP", "NOP", "NOP", "EOR", "LSR", "RMB",
		"PHA", "EOR", "LSR", "NOP", "JMP", "EOR", "LSR", "BBR",
		"BVC", "EOR", "EOR", "NOP", "NOP", "EOR", "LSR", "RMB",
		"CLI", "EOR", "PHY", "NOP", "NOP", "EOR", "LSR", "BBR",
		"RTS", "ADC", "NOP", "NOP", "STZ", "ADC", "ROR", "RMB",
		"PLA", "ADC", "ROR", "NOP", "JMP", "ADC", "ROR", "BBR",
		"BVS", "ADC", "ADC", "NOP", "STZ", "ADC", "ROR", "RMB",
		"SEI", "ADC", "PLY", "NOP", "JMP", "ADC", "ROR", "BBR",
		"BRA", "STA", "NOP", "NOP", "STY", "STA", "STX", "SMB",
		"DEY", "BIT", "TXA", "NOP", "STY", "STA", "STX", "BBS",
		"BCC", "STA", "STA", "NOP", "STY", "STA", "STX", "SMB",
		"TYA", "STA", "TXS", "NOP", "STZ", "STA", "STZ", "BBS",
		"LDY", "LDA", "LDX", "NOP", "LDY", "LDA", "LDX", "SMB",
		"TAY", "LDA", "TAX", "NOP", "LDY", "LDA", "LDX", "BBS",
		"BCS", "LDA", "LDA", "NOP", "LDY", "LDA", "LDX", "SMB",
		"CLV", "LDA", "TSX", "NOP", "LDY", "LDA", "LDX", "BBS",
		"CPY", "CMP", "NOP", "NOP", "CPY", "CMP", "DEC", "SMB",
		"INY", "CMP", "DEX", "NOP", "CPY", "CMP", "DEC", "BBS",
		"BNE", "CMP", "CMP", "NOP", "NOP", "CMP", "DEC", "SMB",
		"CLD", "CMP", "PHX", "NOP", "NOP", "CMP", "DEC", "BBS",
		"CPX", "SBC", "NOP", "NOP", "CPX", "SBC", "INC", "SMB",
		"INX", "SBC", "NOP", "NOP", "CPX", "SBC", "INC", "BBS",
		"BEQ", "SBC", "SBC", "NOP", "NOP", "SBC", "INC", "SMB",
		"SED", "SBC", "PLX", "NOP", "NOP", "SBC", "INC", "BBS"
	};

	/**
	 * Instruction addressing modes.
	 */
	Mode[] instructionModes = {
		Mode.IMP,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x00-0x07
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0x08-0x0f
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPG,Mode.ZPX,Mode.ZPX,Mode.ZPG, // 0x10-0x17
		Mode.IMP,Mode.ABY,Mode.ACC,Mode.IMP,Mode.ABS,Mode.ABX,Mode.ABX,Mode.ZPR, // 0x18-0x1f
		Mode.ABS,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x20-0x27
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0x28-0x2f
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPG, // 0x30-0x37
		Mode.IMP,Mode.ABY,Mode.ACC,Mode.IMP,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ZPR, // 0x38-0x3f
		Mode.IMP,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x40-0x47
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0x48-0x4f
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPG, // 0x50-0x57
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABX,Mode.ABX,Mode.ZPR, // 0x58-0x5f
		Mode.IMP,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x60-0x67
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMP,Mode.IND,Mode.ABS,Mode.ABS,Mode.ZPR, // 0x68-0x6f
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPG, // 0x70-0x77
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.IMP,Mode.IAX,Mode.ABX,Mode.ABX,Mode.ZPR, // 0x78-0x7f
		Mode.REL,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x80-0x87
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0x88-0x8f
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPY,Mode.ZPG, // 0x90-0x97
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABX,Mode.ABX,Mode.ZPR, // 0x98-0x9f
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0xa0-0xa7
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0xa8-0xaf
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPY,Mode.ZPG, // 0xb0-0xb7
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.IMP,Mode.ABX,Mode.ABX,Mode.ABY,Mode.ZPR, // 0xb8-0xbf
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0xc0-0xc7
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0xc8-0xcf
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPG, // 0xd0-0xd7
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABX,Mode.ABX,Mode.ZPR, // 0xd8-0xdf
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.IMP,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0xe0-0xe7
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ZPR, // 0xe8-0xef
		Mode.REL,Mode.INY,Mode.IZP,Mode.IMP,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPG, // 0xf0-0xf7
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.IMP,Mode.ABS,Mode.ABX,Mode.ABX,Mode.ZPR  // 0xf8-0xff
	};

	/**
	 * Number of clock cycles required for each instruction
	 */
	int[] instructionClocks = {
		7, 6, 2, 1, 5, 3, 5, 5, 3, 2, 2, 1, 6, 4, 6, 5,
		2, 5, 5, 1, 5, 4, 6, 5, 2, 4, 2, 1, 6, 4, 6, 5,
		6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 4, 4, 6, 5,
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 2, 1, 4, 4, 6, 5,
		6, 6, 2, 1, 3, 3, 5, 5, 3, 2, 2, 1, 3, 4, 6, 5,
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 3, 1, 8, 4, 6, 5,
		6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 6, 4, 6, 5,
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 4, 1, 6, 4, 6, 5,
		3, 6, 2, 1, 3, 3, 3, 5, 2, 2, 2, 1, 4, 4, 4, 5,
		2, 6, 5, 1, 4, 4, 4, 5, 2, 5, 2, 1, 4, 5, 5, 5,
		2, 6, 2, 1, 3, 3, 3, 5, 2, 2, 2, 1, 4, 4, 4, 5,
		2, 5, 5, 1, 4, 4, 4, 5, 2, 4, 2, 1, 4, 4, 4, 5,
		2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 3, 4, 4, 6, 5,
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 3, 3, 4, 4, 7, 5,
		2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 1, 4, 4, 6, 5,
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 4, 1, 4, 4, 7, 5
	};

}
