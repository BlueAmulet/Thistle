package gamax92.thistle.devices;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryRangeException;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import net.minecraft.nbt.NBTTagCompound;

public class RTC extends Device {

    private Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
	private Machine machine;
	private int timerA = 0;
	private int timerB = 0;
	private int timerAf = 0;
	private int timerBf = 0;
	private int irqmask = 0;
	private int nmimask = 0;

	static final int RTC_TICK_REG = 0;
	static final int RTC_SEC_REG = 1;
	static final int RTC_MIN_REG = 2;
	static final int RTC_HOUR_REG = 3;
	static final int RTC_DAY_REG = 4;
	static final int RTC_MONTH_REG = 5;
	static final int RTC_YEAR_REG_L = 6;
	static final int RTC_YEAR_REG_H = 7;
	static final int RTC_UPTIME_REG_L = 8;
	static final int RTC_UPTIME_REG_H = 9;
	static final int RTC_TA_REG_L = 10;
	static final int RTC_TA_REG_H = 11;
	static final int RTC_TB_REG_L = 12;
	static final int RTC_TB_REG_H = 13;
	static final int RTC_IRQMASK_REG = 14;
	static final int RTC_NMIMASK_REG = 15;

	public RTC(int address) throws MemoryRangeException {
		super(address, 16, "Real Time Clock");
		FMLCommonHandler.instance().bus().register(this);
	}

	private int num2bcd(int value) {
		int bcd = 0;
		for (int i=0; i<4; i++) {
			bcd |= (value % 10) << (i * 4);
			value /= 10;
		}
		return bcd;
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		Context context = this.getBus().getMachine().getContext();
		if (!context.isRunning() && !context.isPaused()) {
			FMLCommonHandler.instance().bus().unregister(this);
			return;
		}
		if (event.phase == Phase.START) {
			Cpu cpu = this.getBus().getMachine().getCpu();
			if (timerA > 0) {
				if (--timerA <= 0) {
					if ((nmimask & 1) != 0)
						cpu.assertNmi();
					else if ((irqmask & 1) != 0)
						cpu.assertIrq();
				}
			}
			if (timerB > 0) {
				if (--timerB <= 0) {
					if ((nmimask & 2) != 0)
						cpu.assertNmi();
					else if ((irqmask & 2) != 0)
						cpu.assertIrq();
				}
			}
		}
	}

	@Override
	public int read(int address) {
		calendar.setTimeInMillis((machine.worldTime() + 6000L) * 60L * 60L);
		switch (address) {
		case RTC_TICK_REG:
			return num2bcd((int) (machine.worldTime() % 20));
		case RTC_SEC_REG:
			return num2bcd(calendar.get(Calendar.SECOND));
		case RTC_MIN_REG:
			return num2bcd(calendar.get(Calendar.MINUTE));
		case RTC_HOUR_REG:
			return num2bcd(calendar.get(Calendar.HOUR_OF_DAY));
		case RTC_DAY_REG:
			return num2bcd(calendar.get(Calendar.DAY_OF_MONTH));
		case RTC_MONTH_REG:
			return num2bcd(calendar.get(Calendar.MONTH) + 1);
		case RTC_YEAR_REG_L:
			return num2bcd(calendar.get(Calendar.YEAR)) & 0xFF;
		case RTC_YEAR_REG_H:
			return (num2bcd(calendar.get(Calendar.YEAR)) & 0xFF00) >>> 8;
		case RTC_UPTIME_REG_L:
			return (int)(machine.upTime() * 20) & 0xFF;
		case RTC_UPTIME_REG_H:
			return ((int)(machine.upTime() * 20) & 0xFF00) >>> 8;
		case RTC_TA_REG_L:
			timerAf = timerA;
			return timerAf & 0xFF;
		case RTC_TA_REG_H:
			return (timerAf & 0xFF00) >>> 8;
		case RTC_TB_REG_L:
			timerBf = timerB;
			return timerBf & 0xFF;
		case RTC_TB_REG_H:
			return (timerBf & 0xFF00) >>> 8;
		case RTC_IRQMASK_REG:
			return irqmask;
		case RTC_NMIMASK_REG:
			return nmimask;
		default:
			return 0;
		}
	}

	@Override
	public void write(int address, int data) {
		switch (address) {
		case RTC_TA_REG_L:
			timerAf = (timerAf & 0xFF00) | data;
			break;
		case RTC_TA_REG_H:
			timerAf = (timerAf & 0xFF) | (data << 8);
			timerA = timerAf;
			break;
		case RTC_TB_REG_L:
			timerBf = (timerBf & 0xFF00) | data;
			break;
		case RTC_TB_REG_H:
			timerBf = (timerBf & 0xFF) | (data << 8);
			timerB = timerBf;
			break;
		case RTC_IRQMASK_REG:
			irqmask = data;
		case RTC_NMIMASK_REG:
			nmimask = data;
		}
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("rtc")) {
			NBTTagCompound rtcTag = nbt.getCompoundTag("rtc");
			this.timerA = rtcTag.getInteger("tA");
			this.timerB = rtcTag.getInteger("tB");
			this.timerAf = rtcTag.getInteger("tAf");
			this.timerBf = rtcTag.getInteger("tBf");
			this.irqmask = rtcTag.getInteger("irq");
			this.nmimask = rtcTag.getInteger("nmi");
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound rtcTag = new NBTTagCompound();
		rtcTag.setInteger("tA", this.timerA);
		rtcTag.setInteger("tB", this.timerB);
		rtcTag.setInteger("tAf", this.timerAf);
		rtcTag.setInteger("tAb", this.timerBf);
		rtcTag.setInteger("irq", this.irqmask);
		rtcTag.setInteger("nmi", this.nmimask);
		nbt.setTag("rtc", rtcTag);
	}

	@Override
	public void setBus(Bus bus) {
		super.setBus(bus);
		this.machine = (Machine) getBus().getMachine().getContext();
	}
}
