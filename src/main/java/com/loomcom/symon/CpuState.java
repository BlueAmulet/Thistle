package com.loomcom.symon;

import com.loomcom.symon.util.Utils;

/**
 * A compact, struct-like representation of CPU state.
 */
public class CpuState {
	/**
	 * Accumulator
	 */
	public int a;

	/**
	 * X index regsiter
	 */
	public int x;

	/**
	 * Y index register
	 */
	public int y;

	/**
	 * Stack Pointer
	 */
	public int sp;

	/**
	 * Program Counter
	 */
	public int pc;

	/**
	 * Last Loaded Instruction Register
	 */
	public int ir;

	/**
	 * Last Loaded Instruction Arguments
	 */
	public int[] args = new int[2];

	public int instSize;
	public boolean irqAsserted;
	public boolean nmiAsserted;
	public int lastPc;

	/* Status Flag Register bits */
	public boolean carryFlag;
	public boolean negativeFlag;
	public boolean zeroFlag;
	public boolean irqDisableFlag;
	public boolean decimalModeFlag;
	public boolean breakFlag;
	public boolean overflowFlag;

	// Processor lockup
	public boolean dead = false;

	// Processor sleep
	public boolean sleep = false;

	/**
	 * Returns a string formatted for the trace log.
	 *
	 * @return a string formatted for the trace log.
	 */
	public String toTraceEvent() {
		String opcode = Cpu.disassembleOp(ir, args);
		return getInstructionByteStatus() + "  " + String.format("%-14s", opcode) + "A:" + Utils.byteToHex(a) + " " + "X:" + Utils.byteToHex(x) + " " + "Y:" + Utils.byteToHex(y) + " " + "F:" + Utils.byteToHex(getStatusFlag()) + " " + "S:1" + Utils.byteToHex(sp) + " " + getProcessorStatusString();
	}

	/**
	 * @return The value of the Process Status Register, as a byte.
	 */
	public int getStatusFlag() {
		int status = 0x20;
		if (carryFlag) {
			status |= Cpu.P_CARRY;
		}
		if (zeroFlag) {
			status |= Cpu.P_ZERO;
		}
		if (irqDisableFlag) {
			status |= Cpu.P_IRQ_DISABLE;
		}
		if (decimalModeFlag) {
			status |= Cpu.P_DECIMAL;
		}
		if (breakFlag) {
			status |= Cpu.P_BREAK;
		}
		if (overflowFlag) {
			status |= Cpu.P_OVERFLOW;
		}
		if (negativeFlag) {
			status |= Cpu.P_NEGATIVE;
		}
		return status;
	}

	public String getInstructionByteStatus() {
		switch (InstructionTable.instructionModes[ir].getLength()) {
		case 0:
		case 1:
			return Utils.wordToHex(lastPc) + "  " + Utils.byteToHex(ir) + "      ";
		case 2:
			return Utils.wordToHex(lastPc) + "  " + Utils.byteToHex(ir) + " " + Utils.byteToHex(args[0]) + "   ";
		case 3:
			return Utils.wordToHex(lastPc) + "  " + Utils.byteToHex(ir) + " " + Utils.byteToHex(args[0]) + " " + Utils.byteToHex(args[1]);
		default:
			return null;
		}
	}

	/**
	 * @return A string representing the current status register state.
	 */
	public String getProcessorStatusString() {
		return "[" + (negativeFlag ? 'N' : '.') + (overflowFlag ? 'V' : '.') + "-" + (breakFlag ? 'B' : '.') + (decimalModeFlag ? 'D' : '.') + (irqDisableFlag ? 'I' : '.') + (zeroFlag ? 'Z' : '.') + (carryFlag ? 'C' : '.') + "]";
	}
}
