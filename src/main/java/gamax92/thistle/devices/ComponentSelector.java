package gamax92.thistle.devices;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;

import com.loomcom.symon.Bus;
import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryRangeException;

import gamax92.thistle.api.IThistleDevice;
import gamax92.thistle.api.ThistleWrapper;
import gamax92.thistle.api.WrapperRegistry;
import gamax92.thistle.util.TSFHelper;
import gamax92.thistle.util.ValueManager;
import gamax92.thistle.wrapper.GenericDevice;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTTagCompound;

public class ComponentSelector extends Device {

	private Machine machine;
	private int status = 0;
	private int select = 0;
	private int info = 0;
	private boolean fSpecific = false;
	private int bankMask = 0x07; // 00111b

	private Queue<Byte> inputbuf = new LinkedList<Byte>();
	private Queue<Byte> outputbuf = new LinkedList<Byte>();
	private IThistleDevice[] components = new IThistleDevice[64];

	private NBTTagCompound[] delayConnectNBT;

	static final int COMPSEL_STATCMD_REG = 0;
	static final int COMPSEL_INFOSEL_REG = 1;
	static final int COMPSEL_IO_REG = 2;
	static final int COMPSEL_FLAG_REG = 3;
	static final int COMPSEL_MEM_REG_L = 8;
	static final int COMPSEL_MEM_REG_H = 9;
	static final int COMPSEL_MASK_REG = 10;

	public ComponentSelector(int address) throws MemoryRangeException {
		super(address, 16, "Component Selector");
	}

	public void checkDelay() {
		if (delayConnectNBT != null) {
			for (int i = 0; i < components.length; i++) {
				if (delayConnectNBT[i] != null) {
					mapComponent(delayConnectNBT[i].getString("_address"), i, delayConnectNBT[i].getBoolean("_generic"));
					if (components[i] instanceof ThistleWrapper)
						((ThistleWrapper) components[i]).load(delayConnectNBT[i]);
				}
			}
			delayConnectNBT = null;
		}
	}

	private Environment getEnvironment(int index) {
		IThistleDevice component = components[select];
		if (component instanceof Environment)
			return (Environment) component;
		else if (component instanceof ThistleWrapper)
			return ((ThistleWrapper) component).host();
		return null;
	}

	private Object parseTSF(Queue<Byte> buffer, boolean useSelect) {
		if (buffer.size() == 0) {
			if (useSelect) {
				Environment environment = getEnvironment(select);
				return (environment != null) ? environment.node().address() : null;
			} else {
				return select;
			}
		}
		Object[] tsfdata = TSFHelper.readArray(buffer, machine, false);
		if (tsfdata == null || tsfdata.length != 1 || !(tsfdata[0] instanceof String || tsfdata[0] instanceof UUID)) {
			return null;
		} else {
			return tsfdata[0];
		}
	}

	private boolean mapComponent(String address, int select, boolean specific) {
		Node node = machine.node().network().node(address);
		if (node == null || !(node instanceof Component))
			return false;
		Environment host = node.host();
		if (!specific) {
			components[select] = new GenericDevice(host);
			return true;
		}
		if (host instanceof IThistleDevice) {
			components[select] = (IThistleDevice) host;
			return true;
		}
		Class<? extends ThistleWrapper> wrapper = WrapperRegistry.getWrapper(host.getClass());
		if (wrapper != null) {
			try {
				components[select] = wrapper.getDeclaredConstructor(Environment.class).newInstance(host);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		components[select] = new GenericDevice(host);
		return true;
	}

	@Override
	public int read(int address) {
		switch (address) {
		case COMPSEL_STATCMD_REG:
			return status;
		case COMPSEL_INFOSEL_REG:
			return info;
		case COMPSEL_IO_REG:
			Byte data = outputbuf.poll();
			return data != null ? data : 0;
		case COMPSEL_FLAG_REG:
			return fSpecific ? 1 : 0;
		case COMPSEL_MEM_REG_L:
			int memavail = Math.min(this.getBus().getMachine().getMemsize() / 0x1000, 0xFFFF);
			return memavail & 0xFF;
		case COMPSEL_MEM_REG_H:
			memavail = Math.min(this.getBus().getMachine().getMemsize() / 0x1000, 0xFFFF);
			return memavail >>> 8;
		case COMPSEL_MASK_REG:
			return bankMask;
		default:
			return 0;
		}
	}

	@Override
	public void write(int address, int data) {
		switch (address) {
		case COMPSEL_STATCMD_REG:
			info = 0;
			outputbuf.clear();
			switch (data) {
			case 0: // map
				Object tsfdata = parseTSF(inputbuf, false);
				if (tsfdata == null || tsfdata instanceof Integer) {
					status = 2;
				} else if (tsfdata instanceof String) {
					String name = (String) tsfdata;
					for (Map.Entry<String, String> entry : machine.components().entrySet()) {
						if (entry.getValue().equals(name)) {
							status = mapComponent(entry.getKey(), select, fSpecific) ? 0 : 1;
							break;
						}
					}
				} else { // Has to be UUID
					String uuid = ((UUID) tsfdata).toString();
					for (Map.Entry<String, String> entry : machine.components().entrySet()) {
						if (entry.getKey().equals(uuid)) {
							status = mapComponent(entry.getKey(), select, fSpecific) ? 0 : 1;
							break;
						}
					}
				}
				break;
			case 1: // unmap
				status = 0;
				tsfdata = parseTSF(inputbuf, false);
				if (tsfdata == null) {
					status = 1;
				} else if (tsfdata instanceof String) {
					String name = (String) tsfdata;
					for (int i = 0; i < components.length; i++) {
						Environment environment = getEnvironment(i);
						if (environment != null && ((Component) environment.node()).name().equals(name)) {
							info++;
							components[i] = null;
						}
					}
				} else if (tsfdata instanceof UUID) {
					String uuid = ((UUID) tsfdata).toString();
					for (int i = 0; i < components.length; i++) {
						Environment environment = getEnvironment(i);
						if (environment != null && environment.node().address().equals(uuid)) {
							info++;
							components[i] = null;
						}
					}
				} else {
					if (components[select] != null) {
						info++;
						components[select] = null;
					}
				}
				break;
			case 2: // reset
				status = 0;
				for (int i = 0; i < components.length; i++) {
					if (components[i] != null)
						info++;
					components[i] = null;
				}
				break;
			case 3: // list
				status = 0;
				tsfdata = null;
				if (inputbuf.size() > 0) {
					Object[] tsfdataz = TSFHelper.readArray(inputbuf, machine, false);
					if (tsfdataz == null || tsfdataz.length != 1 || !(tsfdataz[0] instanceof String || tsfdataz[0] instanceof UUID || tsfdataz[0] instanceof Number)) {
						status = 1;
						break;
					} else {
						tsfdata = tsfdataz[0];
					}
				}
				if (tsfdata instanceof Number) {
					int select = ((Number) tsfdata).intValue() & 0x3F;
					Environment environment = getEnvironment(select);
					if (environment != null) {
						info++;
						TSFHelper.writeUUID(outputbuf, UUID.fromString(environment.node().address()));
						TSFHelper.writeString(outputbuf, ((Component) environment.node()).name());
					}
				} else {
					for (Map.Entry<String, String> entry : machine.components().entrySet()) {
						if (tsfdata == null ||
						    tsfdata instanceof String && entry.getValue().equals(tsfdata) ||
						    tsfdata instanceof UUID && entry.getKey().equals(((UUID) tsfdata).toString()))
						{
							info++;
							TSFHelper.writeUUID(outputbuf, UUID.fromString(entry.getKey()));
							TSFHelper.writeString(outputbuf, entry.getValue());
						}
					}
				}
				break;
			case 4: // destroy value
				status = 0;
				Object[] tsfarray = TSFHelper.readArray(inputbuf, machine);
				if (tsfarray == null || tsfarray.length != 1 || !(tsfarray[0] instanceof Value)) {
					status = 1;
					break;
				}
				ValueManager.removeValue((Value) tsfarray[0], this.machine);
				break;
			default:
				status = 0xFF;
			}
			TSFHelper.writeEnd(outputbuf);
			inputbuf.clear();
			break;
		case COMPSEL_INFOSEL_REG:
			select = data & 0x3F;
			break;
		case COMPSEL_IO_REG:
			inputbuf.add((byte) data);
			break;
		case COMPSEL_FLAG_REG:
			fSpecific = (data & 1) != 0;
			break;
		case COMPSEL_MASK_REG:
			bankMask = data;
			break;
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("compsel")) {
			NBTTagCompound compTag = nbt.getCompoundTag("compsel");
			this.status = compTag.getInteger("status");
			this.info = compTag.getInteger("info");
			this.fSpecific = compTag.getBoolean("flag");
			this.bankMask = compTag.getInteger("mask");
			inputbuf.clear();
			inputbuf.addAll(Arrays.asList(ArrayUtils.toObject(compTag.getByteArray("input"))));
			outputbuf.clear();
			outputbuf.addAll(Arrays.asList(ArrayUtils.toObject(compTag.getByteArray("output"))));
			delayConnectNBT = new NBTTagCompound[components.length];
			// Components must be delay loaded as the computer is currently not in a network.
			for (int i = 0; i < components.length; i++) {
				if (compTag.hasKey("comp" + i))
					delayConnectNBT[i] = compTag.getCompoundTag("comp" + i);
			}
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound compTag = new NBTTagCompound();
		compTag.setInteger("status", this.status);
		compTag.setInteger("info", this.info);
		compTag.setBoolean("flag", this.fSpecific);
		compTag.setInteger("mask", this.bankMask);
		compTag.setByteArray("input", ArrayUtils.toPrimitive(inputbuf.toArray(new Byte[0])));
		compTag.setByteArray("output", ArrayUtils.toPrimitive(outputbuf.toArray(new Byte[0])));
		for (int i = 0; i < components.length; i++) {
			IThistleDevice component = components[i];
			if (component != null) {
				NBTTagCompound deviceTag = new NBTTagCompound();
				if (component instanceof ThistleWrapper)
					((ThistleWrapper) component).save(nbt);
				deviceTag.setString("_address", getEnvironment(i).node().address());
				deviceTag.setBoolean("_generic", !(component instanceof GenericDevice));
				compTag.setTag("comp" + i, deviceTag);
			}
		}
		nbt.setTag("compsel", compTag);
	}

	@Override
	public void setBus(Bus bus) {
		super.setBus(bus);
		this.machine = (Machine) getBus().getMachine().getContext();
	}

	public int getMask() {
		return bankMask;
	}

	public IThistleDevice getComponent(int index) {
		return components[index & 0x3F];
	}
}
