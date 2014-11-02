package gamax92.ocsymon;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTTagCompound;

/** This is the class you implement; Architecture is from the OC API. */
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
		// Set up new VM here, and register all API callbacks you want to
		// provide to it.
		console = new ConsoleDriver(machine);
		vm = new SymonVM();
		vm.simulator.console = console;
		vm.setApiFunction("invoke", new SymonNativeFunction() {
			public Object invoke(Object[] args) {
				final String address = (String) args[0];
				final String method = (String) args[1];
				final Object[] params = (Object[]) args[2];
				try {
					return new Object[] { true, machine.invoke(address, method, params) };
				} catch (LimitReachedException e) {
					// Perform logic also used to sleep / perform synchronized calls.
					// In this example we'll follow a protocol where if this returns
					// (true, something) the call succeeded, if it returns (false)
					// the limit was reached.
					// The script running in the VM is then supposed to return control
					// to the caller initiating the current execution (e.g. by yielding
					// if supported, or just returning, when in an event driven system).
					return new Object[] { false };
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return new Object[] { false };
				}
			}
		});
		vm.setApiFunction("isDirect", new SymonNativeFunction() {
			public Object invoke(Object[] args) {
				final String address = (String) args[0];
				final String method = (String) args[1];
				final Node node = machine.node().network().node(address);
				if (node instanceof Component) {
					final Component component = (Component) node;
					if (component.canBeSeenFrom(machine.node())) {
						final Callback callback = machine.methods(node.host()).get(method);
						if (callback != null) {
							return callback.direct();
						}
					}
				}
				return false;
			}
		});
		// ... more callbacks.
		return true;
	}

	public void close() {
		vm = null;
	}

	public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
		try {
			Signal signal = null;
			if (!isSynchronizedReturn) {
				// Since our machine is a memory mapped one, parse signals here
				while (true) {
					signal = machine.popSignal();
					if (signal != null) {
						if (signal.name().equals("key_down")) {
							int character = (int)(double)(Double)signal.args()[1]; // castception
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

	// Use this to load the VM state, if it can be persisted.
	public void load(NBTTagCompound nbt) {
	}

	// Use this to save the VM state, if it can be persisted.
	public void save(NBTTagCompound nbt) {
	}
}
