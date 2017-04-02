package gamax92.thistle.devices;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Device;
import net.minecraft.nbt.NBTTagCompound;

public class CopyEngine extends Device {

	private int status = 0;
	private int dmaAddress = 0;
	private int dmaTarget = 0;
	private int dmaLength = 0;
	private boolean working = false;

	static final int COPYENG_STATUS_REG = 0;
	static final int COPYENG_ADDRESS_REG_L = 1;
	static final int COPYENG_ADDRESS_REG_H = 2;
	static final int COPYENG_TARGET_REG_L = 3;
	static final int COPYENG_TARGET_REG_H = 4;
	static final int COPYENG_LENGTH_REG_L = 5;
	static final int COPYENG_LENGTH_REG_H = 6;

	public CopyEngine(int address) {
		super(address, 16, "Copy Engine");
	}

	@Override
	public int read(int address) {
		switch (address) {
		case COPYENG_STATUS_REG:
			return this.status;
		case COPYENG_ADDRESS_REG_L:
			return dmaAddress & 0xFF;
		case COPYENG_ADDRESS_REG_H:
			return dmaAddress >>> 8;
		case COPYENG_TARGET_REG_L:
			return dmaTarget & 0xFF;
		case COPYENG_TARGET_REG_H:
			return dmaTarget >>> 8;
		case COPYENG_LENGTH_REG_L:
			return dmaLength & 0xFF;
		case COPYENG_LENGTH_REG_H:
			return dmaLength >>> 8;
		default:
			return 0;
		}
	}

	@Override
	public void write(int address, int data) {
		switch (address) {
		case COPYENG_STATUS_REG:
			if (working)
				break;
			if (data > 7) {
				status = 0xff;
				break;
			}
			status = 0;
			working = true;
			boolean sUnpack = (data & 0x02) != 0;
			boolean tUnpack = (data & 0x01) != 0;
			Bus bus = this.getBus();
			Cpu cpu = bus.getMachine().getCpu();
			if ((data & 0x04) != 0) { // backwards
				for (int i = dmaLength-1; i >= 0; i--) {
					int si = sUnpack ? i : 0;
					int ti = tUnpack ? i : 0;
					bus.write(dmaTarget+ti, bus.read(dmaAddress+si));
				}
				cpu.addCycles(-dmaLength*2);
			} else { // forwards
				for (int i = 0; i < dmaLength; i++) {
					int si = sUnpack ? i : 0;
					int ti = tUnpack ? i : 0;
					bus.write(dmaTarget+ti, bus.read(dmaAddress+si));
				}
				cpu.addCycles(-dmaLength*2);
			}
			working = false;
			break;
		case COPYENG_ADDRESS_REG_L:
			this.dmaAddress = (this.dmaAddress & 0xFF00) | (data & 0xFF);
			break;
		case COPYENG_ADDRESS_REG_H:
			this.dmaAddress = (this.dmaAddress & 0xFF) | ((data & 0xFF) << 8);
			break;
		case COPYENG_TARGET_REG_L:
			this.dmaTarget = (this.dmaTarget & 0xFF00) | (data & 0xFF);
			break;
		case COPYENG_TARGET_REG_H:
			this.dmaTarget = (this.dmaTarget & 0xFF) | ((data & 0xFF) << 8);
			break;
		case COPYENG_LENGTH_REG_L:
			this.dmaLength = (this.dmaLength & 0xFF00) | (data & 0xFF);
			break;
		case COPYENG_LENGTH_REG_H:
			this.dmaLength = (this.dmaLength & 0xFF) | ((data & 0xFF) << 8);
			break;
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("copyeng")) {
			NBTTagCompound copyTag = nbt.getCompoundTag("copyeng");
			this.status = copyTag.getInteger("status");
			this.dmaAddress = copyTag.getInteger("address");
			this.dmaLength = copyTag.getInteger("length");
			this.dmaTarget = copyTag.getInteger("target");
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound copyTag = new NBTTagCompound();
		copyTag.setInteger("status", this.status);
		copyTag.setInteger("address", this.dmaAddress);
		copyTag.setInteger("length", this.dmaLength);
		copyTag.setInteger("target", this.dmaTarget);
		nbt.setTag("copyeng", copyTag);
	}
}
