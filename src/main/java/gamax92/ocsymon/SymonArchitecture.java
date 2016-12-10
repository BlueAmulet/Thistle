package gamax92.ocsymon;

import gamax92.ocsymon.devices.Bank;

import java.util.ArrayList;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.server.PacketSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;

@Architecture.Name("6502 Symon")
public class SymonArchitecture implements Architecture {
	private final Machine machine;

	private SymonVM vm;
	private ConsoleDriver console;

	private boolean initialized = false;

	/** The constructor must have exactly this signature. */
	public SymonArchitecture(Machine machine) {
		this.machine = machine;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	private static int calculateMemory(Iterable<ItemStack> components) {
		int memory = 0;
		for (ItemStack component : components) {
			if (Driver.driverFor(component) instanceof li.cil.oc.api.driver.item.Memory) {
				li.cil.oc.api.driver.item.Memory memdrv = (li.cil.oc.api.driver.item.Memory) Driver.driverFor(component);
				memory += memdrv.amount(component) * 1024;
			}
		}
		return Math.min(Math.max(memory, 0), Settings.get().maxTotalRam());
	}

	@Override
	public boolean recomputeMemory(Iterable<ItemStack> components) {
		int memory = calculateMemory(components);
		if (vm != null) // OpenComputers, why are you calling this before initialize?
			vm.machine.getBank().resize(memory);
		return true;
	}

	@Override
	public boolean initialize() {
		// Set up new VM here
		console = new ConsoleDriver(machine);
		vm = new SymonVM(this.machine);
		vm.console = console;
		vm.machine.getBank().init(calculateMemory(machine.host().internalComponents()));
		initialized = true;
		return true;
	}

	@Override
	public void close() {
		vm = null;
	}

	@Override
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
						} else if (signal.name().equals("clipboard")) {
							char[] paste = ((String) signal.args()[1]).toCharArray();
							for (char character : paste)
								console.pushChar(character);
						}
						vm.machine.getSigDev().queue(signal.name(), signal.args());
					} else
						break;
				}
			}
			vm.run();
			console.flush();

			return new ExecutionResult.Sleep(0);
		} catch (Throwable t) {
			t.printStackTrace();
			return new ExecutionResult.Error(t.toString());
		}
	}

	@Override
	public void runSynchronized() {
		try {
			vm.run();
			console.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onSignal() {
	}

	@Override
	public void onConnect() {
		try {
			PacketSender.sendSound(machine.host().world(), machine.host().xPosition(), machine.host().yPosition(), machine.host().zPosition(), ".");
		} catch (Throwable e) {
		}
	}

	// TODO: Needs more things
	@Override
	public void load(NBTTagCompound nbt) {
		// Restore Machine

		// Restore Acia
		if (nbt.hasKey("acia")) {
			Acia mACIA = vm.machine.getAcia();
			NBTTagCompound aciaTag = nbt.getCompoundTag("acia");
			mACIA.setBaudRate(aciaTag.getInteger("baudRate"));
		}

		// Restore Cpu
		if (nbt.hasKey("cpu")) {
			Cpu mCPU = vm.machine.getCpu();
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
			Memory mRAM = vm.machine.getRam();
			NBTTagCompound ramTag = nbt.getCompoundTag("ram");
			int[] mem = ramTag.getIntArray("mem");
			System.arraycopy(mem, 0, mRAM.getDmaAccess(), 0, mem.length);
		}

		// Restore Rom
		if (nbt.hasKey("rom")) {
			Memory mROM = vm.machine.getRom();
			NBTTagCompound romTag = nbt.getCompoundTag("rom");
			int[] mem = romTag.getIntArray("mem");
			System.arraycopy(mem, 0, mROM.getDmaAccess(), 0, mem.length);
		}

		// Restore Banked Ram
		if (nbt.hasKey("bank")) {
			Bank mBANK = vm.machine.getBank();
			NBTTagCompound bankTag = nbt.getCompoundTag("bank");
			mBANK.setBank(bankTag.getInteger("bank"));
			mBANK.setBankSize(bankTag.getInteger("bankSize"));
			mBANK.setMemsize(bankTag.getInteger("size"));
			int[] mem = bankTag.getIntArray("mem");
			ArrayList<Integer> almem = mBANK.getDmaAccess();
			almem.clear();
			for (int v : mem)
				almem.add(v);
		}

		this.console.load(nbt);
	}

	// TODO: Needs more things
	@Override
	public void save(NBTTagCompound nbt) {
		// Persist Machine

		// Persist Acia
		Acia mACIA = vm.machine.getAcia();
		if (mACIA != null) {
			NBTTagCompound aciaTag = new NBTTagCompound();
			aciaTag.setInteger("baudRate", mACIA.getBaudRate());
			nbt.setTag("acia", aciaTag);
		}

		// Persist Cpu
		Cpu mCPU = vm.machine.getCpu();
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
		Memory mRAM = vm.machine.getRam();
		if (mRAM != null) {
			NBTTagCompound ramTag = new NBTTagCompound();
			int[] mem = mRAM.getDmaAccess();
			ramTag.setIntArray("mem", mem);
			nbt.setTag("ram", ramTag);
		}

		// Persist Rom
		Memory mROM = vm.machine.getRom();
		if (mROM != null) {
			NBTTagCompound romTag = new NBTTagCompound();
			int[] mem = mROM.getDmaAccess();
			romTag.setIntArray("mem", mem);
			nbt.setTag("rom", romTag);
		}

		// Persist Banked Ram
		Bank mBANK = vm.machine.getBank();
		if (mBANK != null) {
			NBTTagCompound bankTag = new NBTTagCompound();
			bankTag.setInteger("bank", mBANK.getBank());
			bankTag.setInteger("bankSize", mBANK.getBankSize());
			bankTag.setInteger("size", mBANK.getMemsize());
			ArrayList<Integer> almem = mBANK.getDmaAccess();
			int mem[] = new int[almem.size()];
			int i = 0;
			for (int v : almem)
				mem[i++] = v;
			bankTag.setIntArray("mem", mem);
			nbt.setTag("bank", bankTag);
		}

		this.console.save(nbt);
	}
}
