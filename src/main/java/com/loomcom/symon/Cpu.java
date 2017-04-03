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

import com.loomcom.symon.util.Utils;

/**
 * This class provides a simulation of the MOS 65C02 CPU's state machine.
 * A simple interface allows this 65C02 to read and write to a simulated bus,
 * and exposes some of the internal state for inspection and debugging.
 */
public class Cpu implements InstructionTable {

	/* Process status register mnemonics */
	public static final int P_CARRY = 0x01;
	public static final int P_ZERO = 0x02;
	public static final int P_IRQ_DISABLE = 0x04;
	public static final int P_DECIMAL = 0x08;
	public static final int P_BREAK = 0x10;
	// Bit 5 is always '1'
	public static final int P_OVERFLOW = 0x40;
	public static final int P_NEGATIVE = 0x80;

	// NMI vector
	public static final int NMI_VECTOR_L = 0xfffa;
	public static final int NMI_VECTOR_H = 0xfffb;
	// Reset vector
	public static final int RST_VECTOR_L = 0xfffc;
	public static final int RST_VECTOR_H = 0xfffd;
	// IRQ vector
	public static final int IRQ_VECTOR_L = 0xfffe;
	public static final int IRQ_VECTOR_H = 0xffff;

	/* The Bus */
	private Bus bus;

	/* The CPU state */
	private final CpuState state = new CpuState();

	/* CPU Cycles available */
	private int cycles = 0;

	/**
	 * Set the bus reference for this CPU.
	 */
	public void setBus(Bus bus) {
		this.bus = bus;
	}

	/**
	 * Return the Bus that this CPU is associated with.
	 */
	public Bus getBus() {
		return bus;
	}

	/**
	 * Reset the CPU to known initial values.
	 */
	public void reset() {
		/* TODO: In reality, the stack pointer could be anywhere
		   on the stack after reset. This non-deterministic behavior might be
		   worth while to simulate. */
		state.sp = 0xff;

		// Set the PC to the address stored in the reset vector
		state.pc = Utils.address(bus.read(RST_VECTOR_L), bus.read(RST_VECTOR_H));

		// Clear instruction register.
		state.ir = 0;

		// Clear status register bits.
		state.carryFlag = false;
		state.zeroFlag = false;
		state.irqDisableFlag = false;
		state.decimalModeFlag = false;
		state.breakFlag = false;
		state.overflowFlag = false;
		state.negativeFlag = false;

		state.irqAsserted = false;

		// Reset Processor lockup
		state.dead = false;

		// Reset Sleeping state
		state.sleep = false;

		// Reset registers.
		state.a = 0;
		state.x = 0;
		state.y = 0;
	}

	public void step(int num) {
		for (int i = 0; i < num; i++) {
			step();
		}
	}

	public int getCycles() {
		return cycles;
	}

	public void addCycles(int count) {
		cycles += count;
	}

	/**
	 * Performs an individual instruction cycle.
	 */
	public void step() {
		// Store the address from which the IR was read, for debugging
		state.lastPc = state.pc;

		if (state.dead) {
			cycles = 0;
			return;
		}

		// Check for Interrupts before doing anything else.
		// This will set the PC and jump to the interrupt vector.
		if (state.nmiAsserted) {
			handleNmi();
		} else if (state.irqAsserted && !getIrqDisableFlag()) {
			handleIrq(state.pc);
		}

		if (state.sleep) {
			cycles = 0;
			return;
		}

		// Fetch memory location for this instruction.
		state.ir = bus.read(state.pc);
		int irAddressMode = (state.ir >> 2) & 0x07; // Bits 3-5 of IR:  [ | | |X|X|X| | ]
		int irOpMode = state.ir & 0x03; // Bits 6-7 of IR:  [ | | | | | |X|X]

		cycles -= Cpu.instructionClocks[state.ir];

		incrementPC();

		// Decode the instruction and operands
		state.instSize = Cpu.instructionModes[state.ir].getLength();
		for (int i = 0; i < state.instSize - 1; i++) {
			state.args[i] = bus.read(state.pc);
			// Increment PC after reading
			incrementPC();
		}

		// Get the data from the effective address (if any)
		int effectiveAddress = 0;
		int tmp; // Temporary storage

		switch (Cpu.instructionModes[state.ir]) {
		case ACC: // Accumulator
		case IMM: // #Immediate
		case IMP: // Implied
		case REL: // Relative
		case ZPR: // Zero Page Relative
			// Not Applicable
			break;
		case ZPG: // Zero Page
			effectiveAddress = state.args[0];
			break;
		case ZPX: // Zero Page,X
			effectiveAddress = (state.args[0] + state.x) & 0xff;
			break;
		case ZPY: // Zero Page,Y
			effectiveAddress = (state.args[0] + state.y) & 0xff;
			break;
		case ABS: // Absolute
			effectiveAddress = Utils.address(state.args[0], state.args[1]);
			break;
		case ABX: // Absolute,X
			effectiveAddress = (Utils.address(state.args[0], state.args[1]) + state.x) & 0xffff;
			break;
		case ABY: // Absolute,Y
			effectiveAddress = (Utils.address(state.args[0], state.args[1]) + state.y) & 0xffff;
			break;
		case XIN: // (Zero Page,X)
			tmp = (state.args[0] + state.x) & 0xff;
			effectiveAddress = Utils.address(bus.read(tmp), bus.read(tmp + 1));
			break;
		case INY: // (Zero Page),Y
			tmp = Utils.address(bus.read(state.args[0]), bus.read((state.args[0] + 1) & 0xff));
			effectiveAddress = (tmp + state.y) & 0xffff;
			break;
		case IND: // (Absolute)
			tmp = Utils.address(state.args[0], state.args[1]);
			effectiveAddress = Utils.address(bus.read(tmp), bus.read(tmp + 1));
			break;
		case IAX: // (Absolute,X)
			tmp = (Utils.address(state.args[0], state.args[1]) + state.x) & 0xffff;
			effectiveAddress = Utils.address(bus.read(tmp), bus.read(tmp + 1));
			break;
		case IZP: // (Zero Page)
			effectiveAddress = Utils.address(bus.read(state.args[0]), bus.read((state.args[0] + 1) & 0xff));
			break;
		}

		// Execute
		switch (state.ir) {

		/** Single Byte Instructions **/
		case 0x00: // BRK - Force Interrupt - Implied
			handleBrk(state.pc + 1);
			break;
		case 0x08: // PHP - Push Processor Status - Implied
			// Break flag is always set in the stack value.
			stackPush(state.getStatusFlag() | 0x10);
			break;
		case 0x10: // BPL - Branch if Positive - Relative
			if (!getNegativeFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0x18: // CLC - Clear Carry Flag - Implied
			clearCarryFlag();
			break;
		case 0x20: // JSR - Jump to Subroutine - Absolute
			stackPush((state.pc - 1 >> 8) & 0xff); // PC high byte
			stackPush(state.pc - 1 & 0xff); // PC low byte
			state.pc = Utils.address(state.args[0], state.args[1]);
			break;
		case 0x28: // PLP - Pull Processor Status - Implied
			setProcessorStatus(stackPop());
			break;
		case 0x30: // BMI - Branch if Minus - Relative
			if (getNegativeFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0x38: // SEC - Set Carry Flag - Implied
			setCarryFlag();
			break;
		case 0x40: // RTI - Return from Interrupt - Implied
			setProcessorStatus(stackPop());
			int lo = stackPop();
			int hi = stackPop();
			setProgramCounter(Utils.address(lo, hi));
			break;
		case 0x48: // PHA - Push Accumulator - Implied
			stackPush(state.a);
			break;
		case 0x50: // BVC - Branch if Overflow Clear - Relative
			if (!getOverflowFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0x58: // CLI - Clear Interrupt Disable - Implied
			clearIrqDisableFlag();
			break;
		case 0x5a: // PHY - Push Y Register - Implied
			stackPush(state.y);
			break;
		case 0x60: // RTS - Return from Subroutine - Implied
			lo = stackPop();
			hi = stackPop();
			setProgramCounter((Utils.address(lo, hi) + 1) & 0xffff);
			break;
		case 0x68: // PLA - Pull Accumulator - Implied
			state.a = stackPop();
			setArithmeticFlags(state.a);
			break;
		case 0x70: // BVS - Branch if Overflow Set - Relative
			if (getOverflowFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0x78: // SEI - Set Interrupt Disable - Implied
			setIrqDisableFlag();
			break;
		case 0x7a: // PLY - Pull Y Register - Implied
			state.y = stackPop();
			setArithmeticFlags(state.y);
			break;
		case 0x80: // BRA - Branch Always - Relative
			state.pc = relAddress(state.args[0]);
			break;
		case 0x88: // DEY - Decrement Y Register - Implied
			state.y = --state.y & 0xff;
			setArithmeticFlags(state.y);
			break;
		case 0x8a: // TXA - Transfer X to Accumulator - Implied
			state.a = state.x;
			setArithmeticFlags(state.a);
			break;
		case 0x90: // BCC - Branch if Carry Clear - Relative
			if (!getCarryFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0x98: // TYA - Transfer Y to Accumulator - Implied
			state.a = state.y;
			setArithmeticFlags(state.a);
			break;
		case 0x9a: // TXS - Transfer X to Stack Pointer - Implied
			setStackPointer(state.x);
			break;
		case 0xa8: // TAY - Transfer Accumulator to Y - Implied
			state.y = state.a;
			setArithmeticFlags(state.y);
			break;
		case 0xaa: // TAX - Transfer Accumulator to X - Implied
			state.x = state.a;
			setArithmeticFlags(state.x);
			break;
		case 0xb0: // BCS - Branch if Carry Set - Relative
			if (getCarryFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0xb8: // CLV - Clear Overflow Flag - Implied
			clearOverflowFlag();
			break;
		case 0xba: // TSX - Transfer Stack Pointer to X - Implied
			state.x = getStackPointer();
			setArithmeticFlags(state.x);
			break;
		case 0xc8: // INY - Increment Y Register - Implied
			state.y = ++state.y & 0xff;
			setArithmeticFlags(state.y);
			break;
		case 0xca: // DEX - Decrement X Register - Implied
			state.x = --state.x & 0xff;
			setArithmeticFlags(state.x);
			break;
		case 0xcb: // WAI - Wait For Interrupt - Implied
			setSleepState();
			break;
		case 0xd0: // BNE - Branch if Not Equal to Zero - Relative
			if (!getZeroFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0xd8: // CLD - Clear Decimal Mode - Implied
			clearDecimalModeFlag();
			break;
		case 0xda: // PHX - Push X Register - Implied
			stackPush(state.x);
			break;
		case 0xdb: // STP - Stop The Processor - Implied
			// TODO
			setDeadState();
			break;
		case 0xe8: // INX - Increment X Register - Implied
			state.x = ++state.x & 0xff;
			setArithmeticFlags(state.x);
			break;
		case 0xea: // NOP - No Operation - Implied
			// Do nothing.
			break;
		case 0xf0: // BEQ - Branch if Equal to Zero - Relative
			if (getZeroFlag()) {
				state.pc = relAddress(state.args[0]);
			}
			break;
		case 0xf8: // SED - Set Decimal Flag - Implied
			setDecimalModeFlag();
			break;
		case 0xfa: // PLX - Pull X Register - Implied
			state.x = stackPop();
			setArithmeticFlags(state.x);
			break;

		/** ORA - Logical Inclusive Or ******************************************/
		case 0x09: // #Immediate
			state.a |= state.args[0];
			setArithmeticFlags(state.a);
			break;
		case 0x01: // (Zero Page,X)
		case 0x05: // Zero Page
		case 0x0d: // Absolute
		case 0x11: // (Zero Page),Y
		case 0x12: // (Zero Page)
		case 0x15: // Zero Page,X
		case 0x19: // Absolute,Y
		case 0x1d: // Absolute,X
			state.a |= bus.read(effectiveAddress);
			setArithmeticFlags(state.a);
			break;

		/** TSB - Test and Set Bit **********************************************/
		case 0x04: // Zero Page
		case 0x0c: // Absolute
			tmp = bus.read(effectiveAddress);
			setZeroFlag((state.a & tmp) == 0);
			bus.write(effectiveAddress, state.a | tmp);
			break;

		/** ASL - Arithmetic Shift Left *****************************************/
		case 0x0a: // Accumulator
			state.a = asl(state.a);
			setArithmeticFlags(state.a);
			break;
		case 0x06: // Zero Page
		case 0x0e: // Absolute
		case 0x16: // Zero Page,X
		case 0x1e: // Absolute,X
			tmp = asl(bus.read(effectiveAddress));
			bus.write(effectiveAddress, tmp);
			setArithmeticFlags(tmp);
			break;

		/** TRB - Test and Reset Bit ********************************************/
		case 0x14: // Zero Page
		case 0x1c: // Absolute
			tmp = bus.read(effectiveAddress);
			setZeroFlag((state.a & tmp) == 0);
			bus.write(effectiveAddress, state.a & ~tmp);
			break;

		/** BIT - Bit Test ******************************************************/
		case 0x89: // #Immediate
			setZeroFlag((state.a & state.args[0]) == 0);
			setNegativeFlag((state.args[0] & 0x80) != 0);
			setOverflowFlag((state.args[0] & 0x40) != 0);
			break;
		case 0x24: // Zero Page
		case 0x2c: // Absolute
		case 0x34: // Zero Page,X
		case 0x3c: // Absolute,X
			tmp = bus.read(effectiveAddress);
			setZeroFlag((state.a & tmp) == 0);
			setNegativeFlag((tmp & 0x80) != 0);
			setOverflowFlag((tmp & 0x40) != 0);
			break;

		/** AND - Logical AND ***************************************************/
		case 0x29: // #Immediate
			state.a &= state.args[0];
			setArithmeticFlags(state.a);
			break;
		case 0x21: // (Zero Page,X)
		case 0x25: // Zero Page
		case 0x2d: // Absolute
		case 0x31: // (Zero Page),Y
		case 0x32: // (Zero Page)
		case 0x35: // Zero Page,X
		case 0x39: // Absolute,Y
		case 0x3d: // Absolute,X
			state.a &= bus.read(effectiveAddress);
			setArithmeticFlags(state.a);
			break;

		/** ROL - Rotate Left ***************************************************/
		case 0x2a: // Accumulator
			state.a = rol(state.a);
			setArithmeticFlags(state.a);
			break;
		case 0x26: // Zero Page
		case 0x2e: // Absolute
		case 0x36: // Zero Page,X
		case 0x3e: // Absolute,X
			tmp = rol(bus.read(effectiveAddress));
			bus.write(effectiveAddress, tmp);
			setArithmeticFlags(tmp);
			break;

		/** EOR - Exclusive OR **************************************************/
		case 0x49: // #Immediate
			state.a ^= state.args[0];
			setArithmeticFlags(state.a);
			break;
		case 0x41: // (Zero Page,X)
		case 0x45: // Zero Page
		case 0x4d: // Absolute
		case 0x51: // (Zero Page),Y
		case 0x52: // (Zero Page)
		case 0x55: // Zero Page,X
		case 0x59: // Absolute,Y
		case 0x5d: // Absolute,X
			state.a ^= bus.read(effectiveAddress);
			setArithmeticFlags(state.a);
			break;

		/** LSR - Logical Shift Right *******************************************/
		case 0x4a: // Accumulator
			state.a = lsr(state.a);
			setArithmeticFlags(state.a);
			break;
		case 0x46: // Zero Page
		case 0x4e: // Absolute
		case 0x56: // Zero Page,X
		case 0x5e: // Absolute,X
			tmp = lsr(bus.read(effectiveAddress));
			bus.write(effectiveAddress, tmp);
			setArithmeticFlags(tmp);
			break;

		/** JMP - Jump **********************************************************/
		case 0x4c: // Absolute
		case 0x6c: // (Absolute)
		case 0x7c: // (Absolute,X)
			state.pc = effectiveAddress;
			break;

		/** ADC - Add with Carry ************************************************/
		case 0x69: // #Immediate
			if (state.decimalModeFlag) {
				state.a = adcDecimal(state.a, state.args[0]);
			} else {
				state.a = adc(state.a, state.args[0]);
			}
			break;
		case 0x61: // (Zero Page,X)
		case 0x65: // Zero Page
		case 0x6d: // Absolute
		case 0x71: // (Zero Page),Y
		case 0x72: // (Zero Page)
		case 0x75: // Zero Page,X
		case 0x79: // Absolute,Y
		case 0x7d: // Absolute,X
			if (state.decimalModeFlag) {
				state.a = adcDecimal(state.a, bus.read(effectiveAddress));
			} else {
				state.a = adc(state.a, bus.read(effectiveAddress));
			}
			break;

		/** STZ - Store Zero ****************************************************/
		case 0x64: // Zero Page
		case 0x74: // Zero Page,X
		case 0x9c: // Absolute
		case 0x9e: // Absolute,X
			bus.write(effectiveAddress, 0);
			break;

		/** ROR - Rotate Right **************************************************/
		case 0x6a: // Accumulator
			state.a = ror(state.a);
			setArithmeticFlags(state.a);
			break;
		case 0x66: // Zero Page
		case 0x6e: // Absolute
		case 0x76: // Zero Page,X
		case 0x7e: // Absolute,X
			tmp = ror(bus.read(effectiveAddress));
			bus.write(effectiveAddress, tmp);
			setArithmeticFlags(tmp);
			break;

		/** STA - Store Accumulator *********************************************/
		case 0x81: // (Zero Page,X)
		case 0x85: // Zero Page
		case 0x8d: // Absolute
		case 0x91: // (Zero Page),Y
		case 0x92: // (Zero Page)
		case 0x95: // Zero Page,X
		case 0x99: // Absolute,Y
		case 0x9d: // Absolute,X
			bus.write(effectiveAddress, state.a);
			break;

		/** STY - Store Y Register **********************************************/
		case 0x84: // Zero Page
		case 0x8c: // Absolute
		case 0x94: // Zero Page,X
			bus.write(effectiveAddress, state.y);
			break;

		/** STX - Store X Register **********************************************/
		case 0x86: // Zero Page
		case 0x8e: // Absolute
		case 0x96: // Zero Page,Y
			bus.write(effectiveAddress, state.x);
			break;

		/** LDY - Load Y Register ***********************************************/
		case 0xa0: // #Immediate
			state.y = state.args[0];
			setArithmeticFlags(state.y);
			break;
		case 0xa4: // Zero Page
		case 0xac: // Absolute
		case 0xb4: // Zero Page,X
		case 0xbc: // Absolute,X
			state.y = bus.read(effectiveAddress);
			setArithmeticFlags(state.y);
			break;

		/** LDX - Load X Register ***********************************************/
		case 0xa2: // #Immediate
			state.x = state.args[0];
			setArithmeticFlags(state.x);
			break;
		case 0xa6: // Zero Page
		case 0xae: // Absolute
		case 0xb6: // Zero Page,Y
		case 0xbe: // Absolute,Y
			state.x = bus.read(effectiveAddress);
			setArithmeticFlags(state.x);
			break;

		/** LDA - Load Accumulator **********************************************/
		case 0xa9: // #Immediate
			state.a = state.args[0];
			setArithmeticFlags(state.a);
			break;
		case 0xa1: // (Zero Page,X)
		case 0xa5: // Zero Page
		case 0xad: // Absolute
		case 0xb1: // (Zero Page),Y
		case 0xb2: // (Zero Page)
		case 0xb5: // Zero Page,X
		case 0xb9: // Absolute,Y
		case 0xbd: // Absolute,X
			state.a = bus.read(effectiveAddress);
			setArithmeticFlags(state.a);
			break;

		/** CPY - Compare Y Register ********************************************/
		case 0xc0: // #Immediate
			cmp(state.y, state.args[0]);
			break;
		case 0xc4: // Zero Page
		case 0xcc: // Absolute
			cmp(state.y, bus.read(effectiveAddress));
			break;

		/** CMP - Compare Accumulator *******************************************/
		case 0xc9: // #Immediate
			cmp(state.a, state.args[0]);
			break;
		case 0xc1: // (Zero Page,X)
		case 0xc5: // Zero Page
		case 0xcd: // Absolute
		case 0xd1: // (Zero Page),Y
		case 0xd2: // (Zero Page)
		case 0xd5: // Zero Page,X
		case 0xd9: // Absolute,Y
		case 0xdd: // Absolute,X
			cmp(state.a, bus.read(effectiveAddress));
			break;

		/** DEC - Decrement Memory **********************************************/
		case 0x3a: // Accumulator
			state.a = (state.a - 1) & 0xff;
			break;
		case 0xc6: // Zero Page
		case 0xce: // Absolute
		case 0xd6: // Zero Page,X
		case 0xde: // Absolute,X
			tmp = (bus.read(effectiveAddress) - 1) & 0xff;
			bus.write(effectiveAddress, tmp);
			setArithmeticFlags(tmp);
			break;

		/** CPX - Compare X Register ********************************************/
		case 0xe0: // #Immediate
			cmp(state.x, state.args[0]);
			break;
		case 0xe4: // Zero Page
		case 0xec: // Absolute
			cmp(state.x, bus.read(effectiveAddress));
			break;

		/** SBC - Subtract with Carry (Borrow) **********************************/
		case 0xe9: // #Immediate
			if (state.decimalModeFlag) {
				state.a = sbcDecimal(state.a, state.args[0]);
			} else {
				state.a = sbc(state.a, state.args[0]);
			}
			break;
		case 0xe1: // (Zero Page,X)
		case 0xe5: // Zero Page
		case 0xed: // Absolute
		case 0xf1: // (Zero Page),Y
		case 0xf2: // (Zero Page)
		case 0xf5: // Zero Page,X
		case 0xf9: // Absolute,Y
		case 0xfd: // Absolute,X
			if (state.decimalModeFlag) {
				state.a = sbcDecimal(state.a, bus.read(effectiveAddress));
			} else {
				state.a = sbc(state.a, bus.read(effectiveAddress));
			}
			break;

		/** INC - Increment Memory **********************************************/
		case 0x1a: // Accumulator
			state.a = (state.a + 1) & 0xff;
			break;
		case 0xe6: // Zero Page
		case 0xee: // Absolute
		case 0xf6: // Zero Page,X
		case 0xfe: // Absolute,X
			tmp = (bus.read(effectiveAddress) + 1) & 0xff;
			bus.write(effectiveAddress, tmp);
			setArithmeticFlags(tmp);
			break;

		// Rockwell 65C02 Extensions

		/** RMB - Reset Memory Bit **********************************************/
		case 0x07: // Zero Page
		case 0x17: // Zero Page
		case 0x27: // Zero Page
		case 0x37: // Zero Page
		case 0x47: // Zero Page
		case 0x57: // Zero Page
		case 0x67: // Zero Page
		case 0x77: // Zero Page
			tmp = 1 << ((state.ir & 0x70) >> 4);
			bus.write(effectiveAddress, bus.read(effectiveAddress) & ~tmp);
			break;

		/** SMB - Set Memory Bit ************************************************/
		case 0x87: // Zero Page
		case 0x97: // Zero Page
		case 0xa7: // Zero Page
		case 0xb7: // Zero Page
		case 0xc7: // Zero Page
		case 0xd7: // Zero Page
		case 0xe7: // Zero Page
		case 0xf7: // Zero Page
			tmp = 1 << ((state.ir & 0x70) >> 4);
			bus.write(effectiveAddress, bus.read(effectiveAddress) | tmp);
			break;

		/** BBR - Branch on Bit Reset *******************************************/
		case 0x0f: // Zero Page Relative
		case 0x1f: // Zero Page Relative
		case 0x2f: // Zero Page Relative
		case 0x3f: // Zero Page Relative
		case 0x4f: // Zero Page Relative
		case 0x5f: // Zero Page Relative
		case 0x6f: // Zero Page Relative
		case 0x7f: // Zero Page Relative
			tmp = 1 << ((state.ir & 0x70) >> 4);
			if ((bus.read(state.args[0]) & tmp) == 0)
				state.pc = relAddress(state.args[1]);
			break;

		/** BBS - Branch on Bit Set *********************************************/
		case 0x8f: // Zero Page Relative
		case 0x9f: // Zero Page Relative
		case 0xaf: // Zero Page Relative
		case 0xbf: // Zero Page Relative
		case 0xcf: // Zero Page Relative
		case 0xdf: // Zero Page Relative
		case 0xef: // Zero Page Relative
		case 0xff: // Zero Page Relative
			tmp = 1 << ((state.ir & 0x70) >> 4);
			if ((bus.read(state.args[0]) & tmp) != 0)
				state.pc = relAddress(state.args[1]);
			break;

		/** Unimplemented Instructions ****************************************/
		default:
			setDeadState();
			break;
		}
	}

	private void handleBrk(int returnPc) {
		handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H, true);
		clearIrq();
	}

	private void handleIrq(int returnPc) {
		handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H, false);
		clearIrq();
	}

	private void handleNmi() {
		handleInterrupt(state.pc, NMI_VECTOR_L, NMI_VECTOR_H, false);
		clearNmi();
	}

	/**
	 * Handle the common behavior of BRK, /IRQ, and /NMI
	 *
	 * @throws MemoryAccessException
	 */
	private void handleInterrupt(int returnPc, int vectorLow, int vectorHigh, boolean isBreak) {
		// Wake the processor up if it's asleep.
		clearSleepState();
		// Set the break flag accordingly.
		if (isBreak) {
			setBreakFlag();
		} else {
			clearBreakFlag();
		}
		// Push program counter + 1 onto the stack
		stackPush((returnPc >> 8) & 0xff); // PC high byte
		stackPush(returnPc & 0xff); // PC low byte
		stackPush(state.getStatusFlag());
		// Set the Interrupt Disabled flag.  RTI will clear it.
		setIrqDisableFlag();
		// The 65C02 clears the Decimal flag on interrupts.
		clearDecimalModeFlag();

		// Load interrupt vector address into PC
		state.pc = Utils.address(bus.read(vectorLow), bus.read(vectorHigh));
	}

	/**
	 * Add with Carry, used by all addressing mode implementations of ADC.
	 * As a side effect, this will set the overflow and carry flags as
	 * needed.
	 *
	 * @param acc     The current value of the accumulator
	 * @param operand The operand
	 * @return The sum of the accumulator and the operand
	 */
	private int adc(int acc, int operand) {
		int result = (operand & 0xff) + (acc & 0xff) + getCarryBit();
		int carry6 = (operand & 0x7f) + (acc & 0x7f) + getCarryBit();
		setCarryFlag((result & 0x100) != 0);
		setOverflowFlag(state.carryFlag ^ ((carry6 & 0x80) != 0));
		result &= 0xff;
		setArithmeticFlags(result);
		return result;
	}

	/**
	 * Add with Carry (BCD).
	 */

	private int adcDecimal(int acc, int operand) {
		int l, h, result;
		l = (acc & 0x0f) + (operand & 0x0f) + getCarryBit();
		if ((l & 0xff) > 9)
			l += 6;
		h = (acc >> 4) + (operand >> 4) + (l > 15 ? 1 : 0);
		if ((h & 0xff) > 9)
			h += 6;
		result = (l & 0x0f) | (h << 4);
		result &= 0xff;
		setCarryFlag(h > 15);
		setZeroFlag(result == 0);
		setNegativeFlag(false); // BCD is never negative
		setOverflowFlag(false); // BCD never sets overflow flag
		this.cycles--;
		return result;
	}

	/**
	 * Common code for Subtract with Carry.  Just calls ADC of the
	 * one's complement of the operand.  This lets the N, V, C, and Z
	 * flags work out nicely without any additional logic.
	 */
	private int sbc(int acc, int operand) {
		int result;
		result = adc(acc, ~operand);
		setArithmeticFlags(result);
		return result;
	}

	/**
	 * Subtract with Carry, BCD mode.
	 */
	private int sbcDecimal(int acc, int operand) {
		int l, h, result;
		l = (acc & 0x0f) - (operand & 0x0f) - (state.carryFlag ? 0 : 1);
		if ((l & 0x10) != 0)
			l -= 6;
		h = (acc >> 4) - (operand >> 4) - ((l & 0x10) != 0 ? 1 : 0);
		if ((h & 0x10) != 0)
			h -= 6;
		result = (l & 0x0f) | (h << 4);
		setCarryFlag((h & 0xff) < 15);
		setZeroFlag(result == 0);
		setNegativeFlag(false); // BCD is never negative
		setOverflowFlag(false); // BCD never sets overflow flag
		this.cycles--;
		return (result & 0xff);
	}

	/**
	 * Compare two values, and set carry, zero, and negative flags
	 * appropriately.
	 */
	private void cmp(int reg, int operand) {
		int tmp = (reg - operand) & 0xff;
		setCarryFlag(reg >= operand);
		setZeroFlag(tmp == 0);
		setNegativeFlag((tmp & 0x80) != 0); // Negative bit set
	}

	/**
	 * Set the Negative and Zero flags based on the current value of the
	 * register operand.
	 */
	private void setArithmeticFlags(int reg) {
		state.zeroFlag = (reg == 0);
		state.negativeFlag = (reg & 0x80) != 0;
	}

	/**
	 * Shifts the given value left by one bit, and sets the carry
	 * flag to the high bit of the initial value.
	 *
	 * @param m The value to shift left.
	 * @return the left shifted value (m * 2).
	 */
	private int asl(int m) {
		setCarryFlag((m & 0x80) != 0);
		return (m << 1) & 0xff;
	}

	/**
	 * Shifts the given value right by one bit, filling with zeros,
	 * and sets the carry flag to the low bit of the initial value.
	 */
	private int lsr(int m) {
		setCarryFlag((m & 0x01) != 0);
		return (m & 0xff) >>> 1;
	}

	/**
	 * Rotates the given value left by one bit, setting bit 0 to the value
	 * of the carry flag, and setting the carry flag to the original value
	 * of bit 7.
	 */
	private int rol(int m) {
		int result = ((m << 1) | getCarryBit()) & 0xff;
		setCarryFlag((m & 0x80) != 0);
		return result;
	}

	/**
	 * Rotates the given value right by one bit, setting bit 7 to the value
	 * of the carry flag, and setting the carry flag to the original value
	 * of bit 1.
	 */
	private int ror(int m) {
		int result = ((m >>> 1) | (getCarryBit() << 7)) & 0xff;
		setCarryFlag((m & 0x01) != 0);
		return result;
	}

	/**
	 * Return the current Cpu State.
	 *
	 * @return the current Cpu State.
	 */
	public CpuState getCpuState() {
		return state;
	}

	/**
	 * @return the negative flag
	 */
	public boolean getNegativeFlag() {
		return state.negativeFlag;
	}

	/**
	 * @param negativeFlag the negative flag to set
	 */
	public void setNegativeFlag(boolean negativeFlag) {
		state.negativeFlag = negativeFlag;
	}

	public void setNegativeFlag() {
		state.negativeFlag = true;
	}

	public void clearNegativeFlag() {
		state.negativeFlag = false;
	}

	/**
	 * @return the carry flag
	 */
	public boolean getCarryFlag() {
		return state.carryFlag;
	}

	/**
	 * @return 1 if the carry flag is set, 0 if it is clear.
	 */
	public int getCarryBit() {
		return (state.carryFlag ? 1 : 0);
	}

	/**
	 * @param carryFlag the carry flag to set
	 */
	public void setCarryFlag(boolean carryFlag) {
		state.carryFlag = carryFlag;
	}

	/**
	 * Sets the Carry Flag
	 */
	public void setCarryFlag() {
		state.carryFlag = true;
	}

	/**
	 * Clears the Carry Flag
	 */
	public void clearCarryFlag() {
		state.carryFlag = false;
	}

	/**
	 * @return the zero flag
	 */
	public boolean getZeroFlag() {
		return state.zeroFlag;
	}

	/**
	 * @param zeroFlag the zero flag to set
	 */
	public void setZeroFlag(boolean zeroFlag) {
		state.zeroFlag = zeroFlag;
	}

	/**
	 * Sets the Zero Flag
	 */
	public void setZeroFlag() {
		state.zeroFlag = true;
	}

	/**
	 * Clears the Zero Flag
	 */
	public void clearZeroFlag() {
		state.zeroFlag = false;
	}

	/**
	 * @return the irq disable flag
	 */
	public boolean getIrqDisableFlag() {
		return state.irqDisableFlag;
	}

	public void setIrqDisableFlag() {
		state.irqDisableFlag = true;
	}

	public void clearIrqDisableFlag() {
		state.irqDisableFlag = false;
	}

	/**
	 * @return the decimal mode flag
	 */
	public boolean getDecimalModeFlag() {
		return state.decimalModeFlag;
	}

	/**
	 * Sets the Decimal Mode Flag to true.
	 */
	public void setDecimalModeFlag() {
		state.decimalModeFlag = true;
	}

	/**
	 * Clears the Decimal Mode Flag.
	 */
	public void clearDecimalModeFlag() {
		state.decimalModeFlag = false;
	}

	/**
	 * @return the break flag
	 */
	public boolean getBreakFlag() {
		return state.breakFlag;
	}

	/**
	 * Sets the Break Flag
	 */
	public void setBreakFlag() {
		state.breakFlag = true;
	}

	/**
	 * Clears the Break Flag
	 */
	public void clearBreakFlag() {
		state.breakFlag = false;
	}

	/**
	 * @return the overflow flag
	 */
	public boolean getOverflowFlag() {
		return state.overflowFlag;
	}

	/**
	 * @param overflowFlag the overflow flag to set
	 */
	public void setOverflowFlag(boolean overflowFlag) {
		state.overflowFlag = overflowFlag;
	}

	/**
	 * Sets the Overflow Flag
	 */
	public void setOverflowFlag() {
		state.overflowFlag = true;
	}

	/**
	 * Clears the Overflow Flag
	 */
	public void clearOverflowFlag() {
		state.overflowFlag = false;
	}

	/**
	 * Set the processor lockup state
	 */
	public void setDeadState() {
		state.dead = true;
	}

	/**
	 * Clear the processor lockup state
	 */
	public void clearDeadState() {
		state.dead = false;
	}

	/**
	 * Set the processor sleeping state
	 */
	public void setSleepState() {
		state.sleep = true;
	}

	/**
	 * Clear the processor sleeping state
	 */
	public void clearSleepState() {
		state.sleep = false;
	}

	public int getAccumulator() {
		return state.a;
	}

	public void setAccumulator(int val) {
		state.a = val;
	}

	public int getXRegister() {
		return state.x;
	}

	public void setXRegister(int val) {
		state.x = val;
	}

	public int getYRegister() {
		return state.y;
	}

	public void setYRegister(int val) {
		state.y = val;
	}

	public int getProgramCounter() {
		return state.pc;
	}

	public void setProgramCounter(int addr) {
		state.pc = addr;
	}

	public int getStackPointer() {
		return state.sp;
	}

	public void setStackPointer(int offset) {
		state.sp = offset;
	}

	public int getInstruction() {
		return state.ir;
	}

	/**
	 * @value The value of the Process Status Register bits to be set.
	 */
	public void setProcessorStatus(int value) {
		if ((value & P_CARRY) != 0)
			setCarryFlag();
		else
			clearCarryFlag();

		if ((value & P_ZERO) != 0)
			setZeroFlag();
		else
			clearZeroFlag();

		if ((value & P_IRQ_DISABLE) != 0)
			setIrqDisableFlag();
		else
			clearIrqDisableFlag();

		if ((value & P_DECIMAL) != 0)
			setDecimalModeFlag();
		else
			clearDecimalModeFlag();

		if ((value & P_BREAK) != 0)
			setBreakFlag();
		else
			clearBreakFlag();

		if ((value & P_OVERFLOW) != 0)
			setOverflowFlag();
		else
			clearOverflowFlag();

		if ((value & P_NEGATIVE) != 0)
			setNegativeFlag();
		else
			clearNegativeFlag();
	}

	public String getAccumulatorStatus() {
		return "$" + Utils.byteToHex(state.a);
	}

	public String getXRegisterStatus() {
		return "$" + Utils.byteToHex(state.x);
	}

	public String getYRegisterStatus() {
		return "$" + Utils.byteToHex(state.y);
	}

	public String getProgramCounterStatus() {
		return "$" + Utils.wordToHex(state.pc);
	}

	public String getStackPointerStatus() {
		return "$" + Utils.byteToHex(state.sp);
	}

	public int getProcessorStatus() {
		return state.getStatusFlag();
	}

	/**
	 * Simulate transition from logic-high to logic-low on the INT line.
	 */
	public void assertIrq() {
		state.irqAsserted = true;
	}

	/**
	 * Simulate transition from logic-low to logic-high of the INT line.
	 */
	public void clearIrq() {
		state.irqAsserted = false;
	}

	/**
	 * Simulate transition from logic-high to logic-low on the NMI line.
	 */
	public void assertNmi() {
		state.nmiAsserted = true;
	}

	/**
	 * Simulate transition from logic-low to logic-high of the NMI line.
	 */
	public void clearNmi() {
		state.nmiAsserted = false;
	}

	/**
	 * Push an item onto the stack, and decrement the stack counter.
	 * Will wrap-around if already at the bottom of the stack (This
	 * is the same behavior as the real 65C02)
	 */
	void stackPush(int data) {
		bus.write(0x100 + state.sp, data);

		if (state.sp == 0) {
			state.sp = 0xff;
		} else {
			--state.sp;
		}
	}

	/**
	 * Pre-increment the stack pointer, and return the top of the stack.
	 * Will wrap-around if already at the top of the stack (This
	 * is the same behavior as the real 65C02)
	 */
	int stackPop() {
		if (state.sp == 0xff) {
			state.sp = 0x00;
		} else {
			++state.sp;
		}

		return bus.read(0x100 + state.sp);
	}

	/**
	 * Peek at the value currently at the top of the stack
	 */
	int stackPeek() {
		return bus.read(0x100 + state.sp + 1);
	}

	/*
	* Increment the PC, rolling over if necessary.
	*/
	void incrementPC() {
		if (state.pc == 0xffff) {
			state.pc = 0;
		} else {
			++state.pc;
		}
	}

	/**
	 * Given a single byte, compute the offset address.
	 */
	int relAddress(int offset) {
		// Cast the offset to a signed byte to handle negative offsets
		int newpc = (state.pc + (byte) offset) & 0xffff;
		this.cycles -= ((newpc & 0xFF00) != (state.pc & 0xFF00)) ? 2 : 1;
		return newpc;
	}

	/**
	 * Given a single byte, compute the Zero Page,Y offset address.
	 */
	int zpyAddress(int zp) {
		return (zp + state.y) & 0xff;
	}

	/**
	 * Return a formatted string representing the last instruction and
	 * operands that were executed.
	 *
	 * @return A string representing the mnemonic and operands of the instruction
	 */
	public static String disassembleOp(int opCode, int[] args) {
		String mnemonic = opcodeNames[opCode];

		if (mnemonic == null) {
			return "KIL";
		}

		StringBuilder sb = new StringBuilder(mnemonic);

		switch (instructionModes[opCode]) {
		case ABS:
			sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1])));
			break;
		case ABX:
			sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",X");
			break;
		case ABY:
			sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",Y");
			break;
		case IMM:
			sb.append(" #$").append(Utils.byteToHex(args[0]));
			break;
		case IND:
			sb.append(" ($").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(")");
			break;
		case XIN:
			sb.append(" ($").append(Utils.byteToHex(args[0])).append(",X)");
			break;
		case INY:
			sb.append(" ($").append(Utils.byteToHex(args[0])).append("),Y");
			break;
		case REL:
			sb.append(" ").append(Utils.byteToString(args[0]));
			break;
		case ZPG:
			sb.append(" $").append(Utils.byteToHex(args[0]));
			break;
		case ZPX:
			sb.append(" $").append(Utils.byteToHex(args[0])).append(",X");
			break;
		case ZPY:
			sb.append(" $").append(Utils.byteToHex(args[0])).append(",Y");
			break;
		case IAX:
			sb.append(" ($").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",X)");
			break;
		case IZP:
			sb.append(" ($").append(Utils.byteToHex(args[0])).append(")");
			break;
		case ZPR:
			sb.append(" $").append(Utils.byteToHex(args[0])).append(",").append(Utils.byteToString(args[0]));
			break;
		default:
			break;
		}

		return sb.toString();
	}

	/**
	 * @param address Address to disassemble
	 * @return String containing the disassembled instruction and operands.
	 */
	public String disassembleOpAtAddress(int address) {
		int opCode = bus.read(address);
		int args[] = new int[2];
		int size = Cpu.instructionModes[opCode].getLength();
		for (int i = 1; i < size; i++) {
			args[i - 1] = bus.read(address + i);
		}

		return disassembleOp(opCode, args);
	}
}
