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
	 * Enumeration of valid CPU behaviors. These determine what behavior and instruction
	 * set will be simulated, depending on desired version of 6502.
	 *
	 * TODO: As of version 0.6, this is still not used! All CPUs are "idealized" NMOS 6502 only.
	 */
	enum CpuBehavior {
		/**
		 * The earliest NMOS 6502 includes a bug that causes the ROR instruction
		 * to behave like an ASL that does not affect the carry bit. This version
		 * is very rare in the wild.
		 *
		 * NB: Does NOT implement "unimplemented" NMOS instructions.
		 */
		NMOS_WITH_ROR_BUG,

		/**
		 * All NMOS 6502's have a bug with the indirect JMP instruction. If the
		 *
		 * NB: Does NOT implement "unimplemented" NMOS instructions.
		 */
		NMOS_WITH_INDIRECT_JMP_BUG,

		/**
		 * Emulate an NMOS 6502 without the indirect JMP bug. This type of 6502
		 * does not actually exist in the wild.
		 *
		 * NB: Does NOT implement "unimplemented" NMOS instructions.
		 */
		NMOS_WITHOUT_INDIRECT_JMP_BUG,

		/**
		 * Emulate a CMOS 65C02, with all CMOS instructions and addressing modes.
		 */
		CMOS
	}

	/**
	 * Enumeration of Addressing Modes.
	 */
	enum Mode {
		ACC("Accumulator"),
		ABS("Absolute"),
		ABX("Absolute, X-indexed"),
		ABY("Absolute, Y-indexed"),
		IMM("Immediate"),
		IMP("Implied"),
		IND("Indirect"),
		XIN("X-indexed Indirect"),
		INY("Indirect, Y-indexed"),
		REL("Relative"),
		ZPG("Zeropage"),
		ZPX("Zeropage, X-indexed"),
		ZPY("Zeropage, Y-indexed");

		private final String text;

		private Mode(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	// 6502 opcodes.  No 65C02 opcodes implemented.

	/**
	 * Instruction opcode names.
	 */
	String[] opcodeNames = {
		"BRK", "ORA", "KIL", "SLO", "DOP", "ORA", "ASL", "SLO",
		"PHP", "ORA", "ASL", "ANC", "TOP", "ORA", "ASL", "SLO",
		"BPL", "ORA", "KIL", "SLO", "DOP", "ORA", "ASL", "SLO",
		"CLC", "ORA", "NOP", "SLO", "TOP", "ORA", "ASL", "SLO",
		"JSR", "AND", "KIL", "RLA", "BIT", "AND", "ROL", "RLA",
		"PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA",
		"BMI", "AND", "KIL", "RLA", "DOP", "AND", "ROL", "RLA",
		"SEC", "AND", "NOP", "RLA", "TOP", "AND", "ROL", "RLA",
		"RTI", "EOR", "KIL", "SRE", "DOP", "EOR", "LSR", "SRE",
		"PHA", "EOR", "LSR", "ALR", "JMP", "EOR", "LSR", "SRE",
		"BVC", "EOR", "KIL", "SRE", "DOP", "EOR", "LSR", "SRE",
		"CLI", "EOR", "NOP", "SRE", "TOP", "EOR", "LSR", "SRE",
		"RTS", "ADC", "KIL", "RRA", "DOP", "ADC", "ROR", "RRA",
		"PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA",
		"BVS", "ADC", "KIL", "RRA", "DOP", "ADC", "ROR", "RRA",
		"SEI", "ADC", "NOP", "RRA", "TOP", "ADC", "ROR", "RRA",
		"DOP", "STA", "DOP", "SAX", "STY", "STA", "STX", "SAX",
		"DEY", "DOP", "TXA", "XAA", "STY", "STA", "STX", "SAX",
		"BCC", "STA", "KIL", "AXA", "STY", "STA", "STX", "SAX",
		"TYA", "STA", "TXS", "TAS", "SAY", "STA", "XAS", "AXA",
		"LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX",
		"TAY", "LDA", "TAX", "OAL", "LDY", "LDA", "LDX", "LAX",
		"BCS", "LDA", "KIL", "LAX", "LDY", "LDA", "LDX", "LAX",
		"CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX",
		"CPY", "CMP", "DOP", "DCP", "CPY", "CMP", "DEC", "DCP",
		"INY", "CMP", "DEX", "AXS", "CPY", "CMP", "DEC", "DCP",
		"BNE", "CMP", "KIL", "DCP", "DOP", "CMP", "DEC", "DCP",
		"CLD", "CMP", "NOP", "DCP", "TOP", "CMP", "DEC", "DCP",
		"CPX", "SBC", "DOP", "ISB", "CPX", "SBC", "INC", "ISB",
		"INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISB",
		"BEQ", "SBC", "KIL", "ISB", "DOP", "SBC", "INC", "ISB",
		"SED", "SBC", "NOP", "ISB", "TOP", "SBC", "INC", "ISB"
	};

	/**
	 * Instruction addressing modes.
	 */
	Mode[] instructionModes = {
		Mode.IMP,Mode.XIN,Mode.IMP,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x00-0x07
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0x08-0x0f
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPX, // 0x10-0x17
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ABX, // 0x18-0x1f
		Mode.ABS,Mode.XIN,Mode.IMP,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x20-0x27
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0x28-0x2f
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPX, // 0x30-0x37
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ABX, // 0x38-0x3f
		Mode.IMP,Mode.XIN,Mode.IMP,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x40-0x47
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0x48-0x4f
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPX, // 0x50-0x57
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ABX, // 0x58-0x5f
		Mode.IMP,Mode.XIN,Mode.IMP,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x60-0x67
		Mode.IMP,Mode.IMM,Mode.ACC,Mode.IMM,Mode.IND,Mode.ABS,Mode.ABS,Mode.ABS, // 0x68-0x6f
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPX, // 0x70-0x77
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ABX, // 0x78-0x7f
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0x80-0x87
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0x88-0x8f
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPY,Mode.ZPY, // 0x90-0x97
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABY,Mode.ABY, // 0x98-0x9f
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0xa0-0xa7
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0xa8-0xaf
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPY,Mode.ZPY, // 0xb0-0xb7
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABY,Mode.ABY, // 0xb8-0xbf
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0xc0-0xc7
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0xc8-0xcf
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPX, // 0xd0-0xd7
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ABX, // 0xd8-0xdf
		Mode.IMM,Mode.XIN,Mode.IMM,Mode.XIN,Mode.ZPG,Mode.ZPG,Mode.ZPG,Mode.ZPG, // 0xe0-0xe7
		Mode.IMP,Mode.IMM,Mode.IMP,Mode.IMM,Mode.ABS,Mode.ABS,Mode.ABS,Mode.ABS, // 0xe8-0xef
		Mode.REL,Mode.INY,Mode.IMP,Mode.INY,Mode.ZPX,Mode.ZPX,Mode.ZPX,Mode.ZPX, // 0xf0-0xf7
		Mode.IMP,Mode.ABY,Mode.IMP,Mode.ABY,Mode.ABX,Mode.ABX,Mode.ABX,Mode.ABX, // 0xf8-0xff
	};

	/**
	 * Size, in bytes, required for each instruction.
	 */
	int[] instructionSizes = {
		1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		3, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 1, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3
	};

	/**
	 * Number of clock cycles required for each instruction
	 */
	int[] instructionClocks = {
		7, 6, 0, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
		2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
		6, 6, 0, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
		2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
		6, 6, 0, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
		2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
		6, 6, 0, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
		2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
		2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
		2, 6, 0, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
		2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
		2, 5, 0, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
		2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
		2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
		2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
		2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7
	};

}
