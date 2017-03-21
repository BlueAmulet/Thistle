package gamax92.thistle.devices;

import com.loomcom.symon.Bus;
import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryRangeException;

import gamax92.thistle.util.UUIDHelper;
import li.cil.oc.api.machine.Machine;
import net.minecraft.nbt.NBTTagCompound;

public class ComputerInfo extends Device {

	private byte[] computerUUID;
	private byte[] tmpfsUUID;

	public ComputerInfo(int address) throws MemoryRangeException {
		super(address, 32, "Computer Info");
	}

	@Override
	public int read(int address) {
		if (address < 16) {
			return computerUUID[address] & 0xFF;
		} else {
			return tmpfsUUID[address - 16] & 0xFF;
		}
	}

	@Override
	public void write(int address, int data) {}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("compinfo")) {
			NBTTagCompound infoTag = nbt.getCompoundTag("banksel");
			byte[] computerUUID = infoTag.getByteArray("computer");
			if (computerUUID.length == 16)
				this.computerUUID = computerUUID;
			byte[] tmpfsUUID = infoTag.getByteArray("tmpfs");
			if (tmpfsUUID.length == 16)
				this.tmpfsUUID = computerUUID;
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound infoSel = new NBTTagCompound();
		infoSel.setByteArray("computer", computerUUID);
		infoSel.setByteArray("tmpfs", tmpfsUUID);
		nbt.setTag("compinfo", infoSel);
	}

	@Override
	public void setBus(Bus bus) {
		super.setBus(bus);
		Machine machine = (Machine) bus.getMachine().getContext();

		computerUUID = UUIDHelper.encodeUUID(machine.node().address());
		tmpfsUUID = UUIDHelper.encodeUUID(machine.tmpAddress());
	}
}
