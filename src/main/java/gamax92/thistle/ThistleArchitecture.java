package gamax92.thistle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.loomcom.symon.Cpu;
import gamax92.thistle.devices.BankSelector;
import gamax92.thistle.exceptions.CallSynchronizedException;
import gamax92.thistle.exceptions.CallSynchronizedException.Cleanup;
import gamax92.thistle.util.ValueManager;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.machine.Callbacks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import scala.Option;

@Architecture.Name("6502 Thistle")
public class ThistleArchitecture implements Architecture {
	private final Machine machine;

	private ThistleVM vm;

	private boolean initialized = false;

	private boolean inSynchronizedCall = false;

	private CallSynchronizedException syncCall;

	/** The constructor must have exactly this signature. */
	public ThistleArchitecture(Machine machine) {
		this.machine = machine;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	private static int calculateMemory(Iterable<ItemStack> components) {
		int memory = 0;
		for (ItemStack component : components) {
			if (Driver.driverFor(component) instanceof Memory) {
				Memory memdrv = (Memory) Driver.driverFor(component);
				memory += memdrv.amount(component) * 1024;
			}
		}
		return Math.min(Math.max(memory, 0), Settings.get().maxTotalRam());
	}

	@Override
	public boolean recomputeMemory(Iterable<ItemStack> components) {
		if (vm != null) {
			int memory = calculateMemory(components);
			vm.machine.resize(memory);
		}
		return true;
	}

	@Override
	public boolean initialize() {
		// Set up new VM here
		vm = new ThistleVM(machine);
		BankSelector banksel = this.vm.machine.getBankSelector();
		int memory = calculateMemory(this.machine.host().internalComponents());
		vm.machine.resize(memory);
		vm.machine.reset();
		for (ItemStack component : machine.host().internalComponents()) {
			if (Driver.driverFor(component) instanceof Processor) {
				vm.cyclesPerTick = ThistleConfig.debugCpuSlowDown ? 10 : (Driver.driverFor(component).tier(component) + 1) * ThistleConfig.clocksPerTick;
				break;
			}
		}
		try {
			PacketSender.sendSound(machine.host().world(), machine.host().xPosition(), machine.host().yPosition(), machine.host().zPosition(), ".");
		} catch (Throwable e) {
		}
		initialized = true;
		return true;
	}

	@Override
	public void close() {
		ValueManager.removeAll(this.machine);
		vm = null;
	}

	@Override
	public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
		try {
			if (!isSynchronizedReturn) {
				// Since our machine is a memory mapped one, parse signals here
				Signal signal = null;
				while (true) {
					signal = machine.popSignal();
					if (signal != null) {
						vm.machine.getBus().onSignal(signal);
					} else
						break;
				}
			}
			vm.run();

			return new ExecutionResult.Sleep(0);
		} catch (CallSynchronizedException e) {
			if (e.getCleanup() != null) {
				if (ThistleConfig.debugCpuTraceLog) // Exceptions thrown cause ThistleVM to skip trace logging.
					Thistle.log.info("[Cpu] " + vm.machine.getCpu().getCpuState().toTraceEvent());
				syncCall = e;
			}
			return new ExecutionResult.SynchronizedCall();
		} catch (LimitReachedException e) {
			return new ExecutionResult.SynchronizedCall();
		} catch (Throwable t) {
			t.printStackTrace();
			return new ExecutionResult.Error(t.toString());
		}
	}

	@Override
	public void runSynchronized() {
		if (syncCall != null) {
			// Nice clean method for us to avoid multiple bus writes
			Object thing = syncCall.getThing();
			Cleanup cleanup = syncCall.getCleanup();
			try {
				Object[] results = null;
				if (thing instanceof String)
					results = machine.invoke((String) thing, syncCall.getMethod(), syncCall.getArgs());
				else if (thing instanceof Value)
					results = machine.invoke((Value) thing, syncCall.getMethod(), syncCall.getArgs());
				cleanup.run(results);
			} catch (Exception e) {
				cleanup.error(e);
			}
			cleanup.finish();
			syncCall = null;
		} else {
			// Attempt to invoke again by re-executing the last instruction
			inSynchronizedCall = true;
			Cpu cpu = vm.machine.getCpu();
			cpu.getCpuState().pc = cpu.getCpuState().lastPc;
			cpu.step();
			inSynchronizedCall = false;
		}
	}

	@Override
	public void onSignal() {
	}

	@Override
	public void onConnect() {
	}

	public Object[] invoke(String address, String method, Object[] args) throws Exception {
		if (!inSynchronizedCall) {
			Node node = machine.node().network().node(address);
			if (node instanceof Component) {
				Callback callback = ((Component) node).annotation(method);
				if (callback != null && !callback.direct())
					throw new CallSynchronizedException(address, method, args);
			}
		}
		try {
			return machine.invoke(address, method, args);
		} catch (LimitReachedException e) {
			throw new CallSynchronizedException(address, method, args);
		}
	}

	public Object[] invoke(Value value, String method, Object[] args) throws Exception {
		if (!inSynchronizedCall) {
			Option<Callbacks.Callback> option = Callbacks.apply(value).get(method);
			if (option != null) {
				Callbacks.Callback callback = option.get();
				if (callback != null && !callback.annotation().direct())
					throw new CallSynchronizedException(value, method, args);
			}
		}
		try {
			return machine.invoke(value, method, args);
		} catch (LimitReachedException e) {
			throw new CallSynchronizedException(value, method, args);
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		// Restore Machine

		// Restore Memory
		byte[] gzmem = SaveHandler.load(nbt, this.machine.node().address() + "_memory");
		if (gzmem.length > 0) {
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(gzmem);
				GZIPInputStream gzis = new GZIPInputStream(bais);
				byte[] mem = IOUtils.toByteArray(gzis);
				IOUtils.closeQuietly(gzis);
				vm.machine.resize(mem.length);
				for (int i = 0; i < mem.length; i++)
					vm.machine.writeMem(i, mem[i]);
			} catch (IOException e) {
				Thistle.log.error("Failed to decompress memory from disk.");
				e.printStackTrace();
			}
		}

		// Restore CPU
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

		// Restore Values
		if (nbt.hasKey("values"))
			ValueManager.load(nbt.getCompoundTag("values"));

		vm.machine.getBus().load(nbt);
	}

	@Override
	public void save(NBTTagCompound nbt) {
		// Persist Machine

		// Persist Memory
		byte mem[] = new byte[vm.machine.getMemsize()];
		for (int i = 0; i < mem.length; i++)
			mem[i] = vm.machine.readMem(i);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos);
			gzos.write(mem);
			gzos.close();
			SaveHandler.scheduleSave(machine.host(), nbt, machine.node().address() + "_memory", baos.toByteArray());
		} catch (IOException e) {
			Thistle.log.error("Failed to compress memory to disk");
			e.printStackTrace();
		}

		// Persist CPU
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

		// Persist Values
		NBTTagCompound valueTag = new NBTTagCompound();
		ValueManager.save(valueTag);
		nbt.setTag("values", valueTag);

		vm.machine.getBus().save(nbt);
	}
}
