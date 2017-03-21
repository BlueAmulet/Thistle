package gamax92.thistle.devices;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryRangeException;

import net.minecraft.nbt.NBTTagCompound;

public class BankSelector extends Device {

	public int[] bankSelect = new int[16];

	public BankSelector(int address) throws MemoryRangeException {
		super(address, 32, "Bank Selector");
		for (int i = 0; i < bankSelect.length; i++)
			bankSelect[i] = i;
	}

	@Override
	public int read(int address) {
		int select = address/2;
		if (address % 2 == 0) {
			return (bankSelect[select] & 0xFF);
		} else {
			return (bankSelect[select] & 0xFF00) >>> 8;
		}
	}

	@Override
	public void write(int address, int data) {
		int select = address/2;
		if (address % 2 == 0) {
			bankSelect[select] = (bankSelect[select] & 0xFF00) | (data & 0xFF);
		} else {
			bankSelect[select] = (bankSelect[select] & 0xFF) | ((data & 0xFF) << 8);
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("banksel")) {
			NBTTagCompound bankSelTag = nbt.getCompoundTag("banksel");
			int[] bankSelect = bankSelTag.getIntArray("select");
			if (bankSelect.length == 16)
				this.bankSelect = bankSelect;
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound bankSel = new NBTTagCompound();
		bankSel.setIntArray("select", bankSelect);
		nbt.setTag("banksel", bankSel);
	}
}
