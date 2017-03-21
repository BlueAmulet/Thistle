package gamax92.thistle.api;

import li.cil.oc.api.network.Environment;
import net.minecraft.nbt.NBTTagCompound;

public abstract class ThistleWrapper implements IThistleDevice {

	private Environment host;

	public ThistleWrapper(Environment host) {
		this.host = host;
	}

	public Environment host() {
		return host;
	}

	public void load(NBTTagCompound nbt) {
	}

	public void save(NBTTagCompound nbt) {
	}
}
