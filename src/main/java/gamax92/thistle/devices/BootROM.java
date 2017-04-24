package gamax92.thistle.devices;

import java.lang.reflect.Field;
import java.util.Map;

import com.loomcom.symon.Bus;
import com.loomcom.symon.devices.Device;
import gamax92.thistle.Thistle;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.EEPROM;
import net.minecraft.nbt.NBTTagCompound;

public class BootROM extends Device {

	private String eepromAddress;
	private Machine machine;

	public BootROM(int address) {
		super(address, 4096+256, "EEPROM");
	}

	private boolean checkEEPROM() {
		if (this.eepromAddress != null && this.machine.components().containsKey(this.eepromAddress))
			return true;
		for (Map.Entry<String, String> entry : this.machine.components().entrySet()) {
			if (entry.getValue().equals("eeprom")) {
				this.eepromAddress = entry.getKey();
				return true;
			}
		}
		return false;
	}

	@Override
	public int read(int address) {
		if (!checkEEPROM())
			return 0;
		Node node = this.machine.node().network().node(this.eepromAddress);
		if (node == null)
			return 0;
		EEPROM eeprom = (EEPROM) node.host();
		if (address < 256) {
			byte[] volatileData = eeprom.volatileData();
			if (address < volatileData.length)
				return volatileData[address];
			else
				return 0;
		} else {
			address -= 256;
			byte[] codeData = eeprom.codeData();
			if (address < codeData.length)
				return codeData[address];
			else
				return 0;
		}
	}

	@Override
	public void write(int address, int data) {
		if (!checkEEPROM())
			return;
		Node node = this.machine.node().network().node(this.eepromAddress);
		if (node == null)
			return;
		EEPROM eeprom = (EEPROM) node.host();
		if (address < 256) {
			byte[] volatileData = eeprom.volatileData();
			if (address < volatileData.length)
				volatileData[address] = (byte) data;
			else {
				try {
					Field field = EEPROM.class.getDeclaredField("volatileData");
					field.setAccessible(true);
					byte[] extendedData = new byte[Settings.get().eepromDataSize()];
					System.arraycopy(volatileData, 0, extendedData, 0, volatileData.length);
					extendedData[address] = (byte) data;
					field.set(eeprom, extendedData);
				} catch (Exception e) {
					Thistle.log.error("Failed expanding EEPROM data", e);
				}
			}
		} else {
			if (eeprom.readonly())
				return;
			address -= 256;
			byte[] codeData = eeprom.codeData();
			if (address < codeData.length)
				codeData[address] = (byte) data;
			else {
				try {
					Field field = EEPROM.class.getDeclaredField("codeData");
					field.setAccessible(true);
					byte[] extendedData = new byte[Settings.get().eepromSize()];
					System.arraycopy(codeData, 0, extendedData, 0, codeData.length);
					extendedData[address] = (byte) data;
					field.set(eeprom, extendedData);
				} catch (Exception e) {
					Thistle.log.error("Failed expanding EEPROM code", e);
				}
			}
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("rom")) {
			NBTTagCompound romTag = nbt.getCompoundTag("rom");
			if (romTag.hasKey("eeprom"))
				this.eepromAddress = romTag.getString("eeprom");
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound romTag = new NBTTagCompound();
		if (this.eepromAddress != null)
			romTag.setString("eeprom", this.eepromAddress);
		nbt.setTag("rom", romTag);
	}

	@Override
	public void setBus(Bus bus) {
		super.setBus(bus);
		this.machine = (Machine) getBus().getMachine().getContext();
	}
}
