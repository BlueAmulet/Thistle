package gamax92.ocsymon;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import net.minecraft.nbt.NBTTagCompound;

import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;

@Architecture.Name("6502 Symon")
public class SymonArchitecture implements Architecture {
	private final Machine machine;

	private SymonVM vm;
	private ConsoleDriver console;

	/** The constructor must have exactly this signature. */
	public SymonArchitecture(Machine machine) {
		this.machine = machine;
	}

	public boolean isInitialized() {
		return true;
	}

	public void recomputeMemory() {
	}

	public boolean initialize() {
		// Set up new VM here
		console = new ConsoleDriver(machine);
		vm = new SymonVM();
		vm.simulator.console = console;
		return true;
	}

	public void close() {
		vm = null;
	}

	public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
		try {
			if (!isSynchronizedReturn) {
				// Since our machine is a memory mapped one, parse signals here
				// TODO: Signal device
				Signal signal = null;
				while (true) {
					signal = machine.popSignal();
					if (signal != null) {
						if (signal.name().equals("key_down")) {
							int character = (int) (double) (Double) signal.args()[1]; // castception
							if (character != 0) // Not a character
								console.pushChar(character);
						}
					} else
						break;
				}
			}
			vm.run();
			console.flush();

			return new ExecutionResult.Sleep(0);
		} catch (Throwable t) {
			return new ExecutionResult.Error(t.toString());
		}
	}

	public void runSynchronized() {
		try {
			vm.run();
			console.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onConnect() {
	}

	// TODO: Needs more things
	public void load(NBTTagCompound nbt) {
		// Restore Machine

		// Restore Acia
		if (nbt.hasKey("acia")) {
			Acia mACIA = vm.simulator.machine.getAcia();
			NBTTagCompound aciaTag = nbt.getCompoundTag("acia");
			mACIA.setBaudRate(aciaTag.getInteger("baudRate"));
		}

		// Restore Cpu
		if (nbt.hasKey("acia")) {
			Cpu mCPU = vm.simulator.machine.getCpu();
			NBTTagCompound cpuTag = nbt.getCompoundTag("cpu");
			mCPU.setAccumulator(cpuTag.getInteger("rA"));
			mCPU.setProcessorStatus(cpuTag.getInteger("rP"));
			mCPU.setProgramCounter(cpuTag.getInteger("rPC"));
			mCPU.setStackPointer(cpuTag.getInteger("rSP"));
			mCPU.setXRegister(cpuTag.getInteger("rX"));
			mCPU.setYRegister(cpuTag.getInteger("rY"));
		}

		// Restore Ram
		if (nbt.hasKey("ram")) {
			Memory mRAM = vm.simulator.machine.getRam();
			NBTTagCompound ramTag = nbt.getCompoundTag("ram");
			int[] mem = ramTag.getIntArray("mem");
			System.arraycopy(mem, 0, mRAM.getDmaAccess(), 0, mem.length);
		}

		// Restore Rom
		if (nbt.hasKey("rom")) {
			Memory mROM = vm.simulator.machine.getRom();
			NBTTagCompound romTag = nbt.getCompoundTag("rom");
			int[] mem = romTag.getIntArray("mem");
			System.arraycopy(mem, 0, mROM.getDmaAccess(), 0, mem.length);
		}

		this.console.load(nbt);
	}

	// TODO: Needs more things
	public void save(NBTTagCompound nbt) {
		// Persist Machine

		// Persist Acia
		Acia mACIA = vm.simulator.machine.getAcia();
		if (mACIA != null) {
			NBTTagCompound aciaTag = new NBTTagCompound();
			aciaTag.setInteger("baudRate", mACIA.getBaudRate());
			nbt.setTag("acia", aciaTag);
		}

		// Persist Cpu
		Cpu mCPU = vm.simulator.machine.getCpu();
		if (mCPU != null) {
			NBTTagCompound cpuTag = new NBTTagCompound();
			cpuTag.setInteger("rA", mCPU.getAccumulator());
			cpuTag.setInteger("rP", mCPU.getProcessorStatus());
			cpuTag.setInteger("rPC", mCPU.getProgramCounter());
			cpuTag.setInteger("rSP", mCPU.getStackPointer());
			cpuTag.setInteger("rX", mCPU.getXRegister());
			cpuTag.setInteger("rY", mCPU.getYRegister());
			nbt.setTag("cpu", cpuTag);
		}

		// Persist Ram
		Memory mRAM = vm.simulator.machine.getRam();
		if (mRAM != null) {
			NBTTagCompound ramTag = new NBTTagCompound();
			int[] mem = mRAM.getDmaAccess();
			ramTag.setIntArray("mem", mem);
			nbt.setTag("ram", ramTag);
		}

		// Persist Rom
		Memory mROM = vm.simulator.machine.getRom();
		if (mROM != null) {
			NBTTagCompound romTag = new NBTTagCompound();
			int[] mem = mROM.getDmaAccess();
			romTag.setIntArray("mem", mem);
			nbt.setTag("rom", romTag);
		}

		this.console.save(nbt);
	}
}
